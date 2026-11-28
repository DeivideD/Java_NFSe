package com.coffeetecnologia.nfse.xml;

import com.coffeetecnologia.nfse.config.Ambiente;
import com.coffeetecnologia.nfse.config.Leiaute;
import com.coffeetecnologia.nfse.exception.NfseException;
import com.coffeetecnologia.nfse.model.dps.Dest;
import com.coffeetecnologia.nfse.model.dps.Dps;
import com.coffeetecnologia.nfse.model.dps.GIbscbs;
import com.coffeetecnologia.nfse.model.dps.Ibscbs;
import com.coffeetecnologia.nfse.model.dps.OpcaoSimplesNacional;
import com.coffeetecnologia.nfse.model.dps.RegimeApuracaoSimplesNacional;
import com.coffeetecnologia.nfse.model.dps.RegimeEspecialTributacao;
import com.coffeetecnologia.nfse.model.dps.Prestador;
import com.coffeetecnologia.nfse.model.dps.Servico;
import com.coffeetecnologia.nfse.model.dps.Substituicao;
import com.coffeetecnologia.nfse.model.dps.Tomador;
import com.coffeetecnologia.nfse.model.dps.ApuracaoPisCofins;
import com.coffeetecnologia.nfse.model.dps.Valores;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Constrói o XML do DPS conforme o schema oficial v1.01 da RFB.
 *
 * Estrutura gerada:
 * <DPS versao="1.01" xmlns="http://www.sped.fazenda.gov.br/nfse">
 *   <infDPS Id="DPS...">
 *     <tpAmb>2</tpAmb>
 *     <dhEmi>2026-04-22T23:00:00-03:00</dhEmi>
 *     <verAplic>java-nfse-1.0.0</verAplic>
 *     <serie>1</serie>
 *     <nDPS>1</nDPS>
 *     <dCompet>2026-04-22</dCompet>
 *     <tpEmit>1</tpEmit>
 *     <cLocEmi>2304400</cLocEmi>
 *     <prest>...</prest>
 *     <toma>...</toma>
 *     <serv>...</serv>
 *     <valores>...</valores>
 *   </infDPS>
 * </DPS>
 */
public class XmlBuilder {

  private static final String NS = "http://www.sped.fazenda.gov.br/nfse";
  private static final String VERSAO = "1.01";
  private static final String VER_APLIC = "java-nfse-1.3.0";
  private static final ZoneId ZONE_BR = ZoneId.of("America/Sao_Paulo");
  private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
  private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  /** Recuo aplicado a {@code <dhEmi>} para não estourar E0008 por diferença de relógio com a Sefin. */
  private static final java.time.Duration MARGEM_DH_EMI = java.time.Duration.ofMinutes(5);

  private final DocumentBuilder documentBuilder;
  private final Ambiente ambiente;
  /** Leiaute fixo; nulo = o vigente no ambiente no momento de cada DPS. */
  private final Leiaute leiauteFixo;

  /** Compatibilidade: assume Produção Restrita (tpAmb=2). */
  public XmlBuilder() {
    this(Ambiente.PRODUCAO_RESTRITA, null);
  }

  /**
   * @param tpAmb código do ambiente gravado em {@code <tpAmb>}: 1=Produção, 2=Produção Restrita.
   *              Deve bater com o endpoint para o qual a DPS é enviada (senão a Sefin devolve E0006).
   */
  public XmlBuilder(int tpAmb) {
    this(tpAmb == 1 ? Ambiente.PRODUCAO : Ambiente.PRODUCAO_RESTRITA, null);
  }

  /**
   * @param ambiente define {@code <tpAmb>} e, se {@code leiaute} for nulo, o leiaute vigente.
   * @param leiaute  leiaute fixo da DPS, ou nulo para seguir o calendário da NT 009.
   */
  public XmlBuilder(Ambiente ambiente, Leiaute leiaute) {
    this.ambiente = ambiente;
    this.leiauteFixo = leiaute;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      // Proteção XXE
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      this.documentBuilder = factory.newDocumentBuilder();
    } catch (Exception e) {
      throw new NfseException("Erro ao inicializar XmlBuilder.", e);
    }
  }

  /** Leiaute usado agora: o fixo, se houver, ou o vigente no ambiente. */
  public Leiaute leiaute() {
    return leiauteFixo != null ? leiauteFixo : Leiaute.vigente(ambiente);
  }

  /**
   * Gera o XML do DPS como String (sem assinatura).
   */
  public String build(Dps dps) {
    return toXmlString(buildDocument(dps));
  }

  /**
   * Gera o Document DOM do DPS (para uso pelo XmlSigner).
   */
  public Document buildDocument(Dps dps) {
    try {
      Document doc = documentBuilder.newDocument();
      Element root = buildDps(doc, dps, leiaute());
      doc.appendChild(root);
      return doc;
    } catch (NfseException | IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new NfseException("Erro ao gerar Document do DPS.", e);
    }
  }

  // ========================
  // <DPS versao="1.01">
  // ========================

  private Element buildDps(Document doc, Dps dps, Leiaute leiaute) {
    Element el = doc.createElementNS(NS, "DPS");
    el.setAttribute("versao", VERSAO);
    el.appendChild(buildInfDps(doc, dps, leiaute));
    return el;
  }

  // ========================
  // <infDPS Id="DPS...">
  // ========================

  private Element buildInfDps(Document doc, Dps dps, Leiaute leiaute) {
    Element el = doc.createElementNS(NS, "infDPS");

    String id = gerarId(dps);
    el.setAttribute("Id", id);

    // Ambiente: 1=Produção, 2=Produção Restrita — derivado do Ambiente configurado
    addEl(doc, el, "tpAmb", String.valueOf(ambiente.getTpAmb()));

    // Data/hora com timezone: yyyy-MM-dd'T'HH:mm:ssxxx
    // Recuo de MARGEM_DH_EMI: a Sefin rejeita com E0008 qualquer dhEmi à frente do
    // relógio dela no momento do processamento. O ambiente de Produção Restrita
    // costuma estar alguns minutos atrás do horário oficial; o recuo absorve essa
    // diferença e a latência de assinatura/transmissão sem violar nenhuma regra
    // (dhEmi só não pode ser futuro; não há limite inferior no mesmo dia).
    // O recuo nunca volta para o dia anterior: à 00:02 do dia 1º a nota seria do mês passado.
    ZonedDateTime agora = dhEmi(ZonedDateTime.now(ZONE_BR));
    addEl(doc, el, "dhEmi", agora.format(FMT_DATETIME));

    addEl(doc, el, "verAplic", VER_APLIC);

    // Série: numérica, max 5 dígitos
    addEl(doc, el, "serie", dps.getSerie() != null ? dps.getSerie() : "1");

    // Número DPS: numérico, max 15 dígitos
    addEl(doc, el, "nDPS", dps.getNumero() != null ? dps.getNumero() : "1");

    // Data de competência (AAAA-MM-DD): início da prestação; padrão = data da emissão
    addEl(doc, el, "dCompet", dataCompetencia(dps, agora.toLocalDate()).format(FMT_DATE));

    // Tipo de emitente: 1=Prestador, 2=Tomador, 3=Intermediário
    addEl(doc, el, "tpEmit", "1");

    // Finalidade da NFS-e: na NT 009 fica em infDPS e é obrigatória ("0" = regular, único valor aceito)
    if (leiaute == Leiaute.NT_009) {
      addEl(doc, el, "finNFSe", finNFSe(dps));
    }

    // Código IBGE do município emissor (7 dígitos): onde o prestador é cadastrado
    addEl(doc, el, "cLocEmi", municipioEmissor(dps));

    // Substituição (opcional) — deve vir antes de <prest> conforme XSD
    if (dps.getSubst() != null) {
      el.appendChild(buildSubst(doc, dps.getSubst()));
    }

    // Prestador
    el.appendChild(buildPrest(doc, dps.getPrestador()));

    // Tomador (opcional no XSD mas recomendado)
    if (dps.getTomador() != null) {
      el.appendChild(buildToma(doc, dps.getTomador()));
    }

    // NT 009 (XSD da Sefin de 24/11/2026): indDest e dest saíram de IBSCBS e vêm logo após <toma>.
    // indDest é obrigatório mesmo sem o grupo IBSCBS; "0" = o tomador é o destinatário.
    if (leiaute == Leiaute.NT_009) {
      Ibscbs ibscbs = dps.getIbscbs();
      String indDest = ibscbs != null && ibscbs.getIndDest() != null && !ibscbs.getIndDest().isBlank()
          ? ibscbs.getIndDest() : "0";
      if ("1".equals(indDest) && ibscbs.getDest() == null) {
        throw new IllegalStateException("IBSCBS.dest é obrigatório quando indDest=\"1\".");
      }
      addEl(doc, el, "indDest", indDest);
      if (ibscbs != null && ibscbs.getDest() != null) {
        el.appendChild(buildDest(doc, ibscbs.getDest()));
      }
    }

    // Serviço
    el.appendChild(buildServ(doc, dps.getServico(), municipioPrestacao(dps), leiaute));

    // Valores
    el.appendChild(buildValores(doc, dps.getValores()));

    // IBSCBS — último elemento de infDPS conforme TCInfDPS (XSD v1.01, jul/2026)
    if (dps.getIbscbs() != null) {
      el.appendChild(buildIbscbs(doc, dps.getIbscbs(), leiaute));
    }

    return el;
  }

  // ========================
  // <subst>
  // ========================

  private Element buildSubst(Document doc, Substituicao subst) {
    Element el = doc.createElementNS(NS, "subst");
    addEl(doc, el, "chSubstda", subst.getChSubstda());
    addEl(doc, el, "cMotivo", subst.getCMotivo());
    if (subst.getXMotivo() != null) {
      addEl(doc, el, "xMotivo", subst.getXMotivo());
    }
    return el;
  }

  // ========================
  // <prest>
  // ========================

  private Element buildPrest(Document doc, Prestador prestador) {
    Element el = doc.createElementNS(NS, "prest");

    // CNPJ ou CPF — direto, sem wrapper
    if (prestador.isCnpj()) {
      addEl(doc, el, "CNPJ", prestador.getCnpj());
    } else {
      addEl(doc, el, "CPF", prestador.getCpf());
    }

    // Inscrição municipal (opcional)
    if (prestador.getInscricaoMunicipal() != null) {
      addEl(doc, el, "IM", prestador.getInscricaoMunicipal());
    }

    // Regime tributário — obrigatório no XSD
    Element regTrib = doc.createElementNS(NS, "regTrib");
    OpcaoSimplesNacional opSimpNac = prestador.getOpcaoSimplesNacional() != null
        ? prestador.getOpcaoSimplesNacional() : OpcaoSimplesNacional.NAO_OPTANTE;
    addEl(doc, regTrib, "opSimpNac", opSimpNac.getCodigo());
    // regApTribSN só existe para ME/EPP (opSimpNac = 3)
    RegimeApuracaoSimplesNacional regAp = prestador.getRegimeApuracaoSimplesNacional();
    if (regAp != null) {
      if (opSimpNac != OpcaoSimplesNacional.ME_EPP) {
        throw new NfseException("Regime de apuração do Simples Nacional só se aplica a optante ME/EPP.");
      }
      addEl(doc, regTrib, "regApTribSN", regAp.getCodigo());
    }
    RegimeEspecialTributacao regEsp = prestador.getRegimeEspecialTributacao() != null
        ? prestador.getRegimeEspecialTributacao() : RegimeEspecialTributacao.NENHUM;
    addEl(doc, regTrib, "regEspTrib", regEsp.getCodigo());
    el.appendChild(regTrib);

    return el;
  }

  // ========================
  // <toma>
  // ========================

  private Element buildToma(Document doc, Tomador tomador) {
    Element el = doc.createElementNS(NS, "toma");

    if (tomador.getCnpj() != null) {
      addEl(doc, el, "CNPJ", tomador.getCnpj());
    } else if (tomador.getCpf() != null) {
      addEl(doc, el, "CPF", tomador.getCpf());
    } else {
      // Sem identificação fiscal
      addEl(doc, el, "cNaoNIF", "2");
    }

    // xNome é obrigatório para TCInfoPessoa (tomador)
    if (tomador.getNome() != null) {
      addEl(doc, el, "xNome", tomador.getNome());
    } else {
      addEl(doc, el, "xNome", "Não Informado");
    }

    // <end> — exigido pela Sefin Nacional quando o ISSQN é devido no município do
    // tomador (itens LC 01.xx etc.) ou pelo indicador de operação (rejeição E0234).
    // Endereço informado pela metade é erro do emitente: descartá-lo calado só trocava
    // a mensagem clara pela E0234 da Sefin.
    Element end = buildEndTomador(doc, tomador.getEndereco());
    if (end != null) {
      el.appendChild(end);
    }

    addEl(doc, el, "email", tomador.getEmail());

    return el;
  }

  // ========================
  // <toma><end> — endereço do tomador (TCEndereco / endNac)
  // ========================

  private Element buildEndTomador(Document doc, Tomador.Endereco endereco) {
    if (endereco == null) return null;

    String cMun = soDigitos(endereco.getCodigoMunicipio());
    String cep = soDigitos(endereco.getCep());
    String xLgr = trimToNull(endereco.getLogradouro());
    String nro = trimToNull(endereco.getNumero());
    String xBairro = trimToNull(endereco.getBairro());

    // TCEndereco + endNac: cMun(7), CEP(8), xLgr, nro e xBairro são obrigatórios.
    List<String> faltando = new ArrayList<>();
    if (xLgr == null) faltando.add("logradouro");
    if (nro == null) faltando.add("número");
    if (xBairro == null) faltando.add("bairro");
    if (cMun.length() != 7) faltando.add("código IBGE do município (7 dígitos)");
    if (cep.length() != 8) faltando.add("CEP (8 dígitos)");
    if (!faltando.isEmpty()) {
      throw new NfseException("Endereço do tomador incompleto: " + String.join(", ", faltando) + ".");
    }

    Element el = doc.createElementNS(NS, "end");

    Element endNac = doc.createElementNS(NS, "endNac");
    addEl(doc, endNac, "cMun", cMun);
    addEl(doc, endNac, "CEP", cep);
    el.appendChild(endNac);

    addEl(doc, el, "xLgr", xLgr);
    addEl(doc, el, "nro", nro);
    addEl(doc, el, "xCpl", trimToNull(endereco.getComplemento()));
    addEl(doc, el, "xBairro", xBairro);

    return el;
  }

  // ========================
  // <serv>
  // ========================

  private Element buildServ(Document doc, Servico servico, String codigoMunicipio, Leiaute leiaute) {
    Element el = doc.createElementNS(NS, "serv");

    // <locPrest>
    Element locPrest = doc.createElementNS(NS, "locPrest");
    addEl(doc, locPrest, "cLocPrestacao", codigoMunicipio);
    el.appendChild(locPrest);

    // <cServ>
    Element cServ = doc.createElementNS(NS, "cServ");
    addEl(doc, cServ, "cTribNac", servico.getCodigoServico());
    // cTribMun vem logo depois do cTribNac nos dois leiautes (TCServ)
    String cTribMun = codigoTributacaoMunicipal(servico);
    if (cTribMun != null) {
      addEl(doc, cServ, "cTribMun", cTribMun);
    }
    // NT 009: xDescServ passou para depois de cNBS
    if (leiaute == Leiaute.NT_004) {
      addEl(doc, cServ, "xDescServ", servico.getDescricao());
    }
    addEl(doc, cServ, "cNBS", servico.getCodigoNbs());
    if (leiaute == Leiaute.NT_009) {
      addEl(doc, cServ, "xDescServ", servico.getDescricao());
    }
    el.appendChild(cServ);

    // <obra> — obrigatório para códigos 07.02, 07.04, 07.05, 07.06, 07.07, 07.08 etc
    if (servico.getObra() != null) {
      el.appendChild(buildObra(doc, servico.getObra(), codigoMunicipio));
    }

    return el;
  }

  // ========================
  // <valores>
  // ========================

  private Element buildValores(Document doc, Valores valores) {
    Element el = doc.createElementNS(NS, "valores");

    // <vServPrest> — valor do serviço
    Element vServPrest = doc.createElementNS(NS, "vServPrest");
    addEl(doc, vServPrest, "vServ", fmt(valores.getValorServico()));
    el.appendChild(vServPrest);

    // <trib> — informações de tributação
    Element trib = doc.createElementNS(NS, "trib");

    // <tribMun> — ISSQN
    Element tribMun = doc.createElementNS(NS, "tribMun");
    // tribISSQN: 1=Tributável, 2=Imunidade, 3=Exportação, 4=Não Incidência
    addEl(doc, tribMun, "tribISSQN", "1");
    // tpRetISSQN: 1=Não Retido, 2=Retido pelo Tomador, 3=Retido pelo Intermediário
    addEl(doc, tribMun, "tpRetISSQN", valores.isIssRetido() ? "2" : "1");
    if (valores.getAliquotaIss() != null) {
      addEl(doc, tribMun, "pAliq", fmt(valores.getAliquotaIss().multiply(BigDecimal.valueOf(100))));
    }
    trib.appendChild(tribMun);

    // <tribFed> — retenções federais pelo tomador (opcional; só aparece com algum valor retido)
    Element tribFed = buildTribFed(doc, valores);
    if (tribFed != null) {
      trib.appendChild(tribFed);
    }

    // <totTrib> — total aproximado de tributos (obrigatório). ME/EPP informa pTotTribSN (o indTotTrib
    // é recusado com E0712); os demais, indTotTrib = 0 ("não informar valor estimado").
    Element totTrib = doc.createElementNS(NS, "totTrib");
    if (valores.getPercentualTributosSimplesNacional() != null) {
      addEl(doc, totTrib, "pTotTribSN", fmt(valores.getPercentualTributosSimplesNacional()));
    } else {
      addEl(doc, totTrib, "indTotTrib", "0");
    }
    trib.appendChild(totTrib);

    el.appendChild(trib);

    return el;
  }

  /**
   * Tributação federal, na ordem do XSD: {@code piscofins}, {@code vRetCP} (INSS), {@code vRetIRRF} e
   * {@code vRetCSLL}. Pela NT 007, PIS, COFINS e CSLL retidos vão SOMADOS em {@code vRetCSLL} e o
   * {@code tpRetPisCofins} diz quais foram retidos; {@code vPis}/{@code vCofins} são só da apuração própria.
   * Sem retenção nem apuração própria o grupo não aparece.
   */
  private Element buildTribFed(Document doc, Valores valores) {
    boolean pis = positivo(valores.getValorPis());
    boolean cofins = positivo(valores.getValorCofins());
    boolean csll = positivo(valores.getValorCsll());
    boolean inss = positivo(valores.getValorInss());
    boolean irrf = positivo(valores.getValorIr());
    ApuracaoPisCofins apuracao = valores.getApuracaoPisCofins();
    boolean grupoPisCofins = pis || cofins || csll || apuracao != null;
    if (!grupoPisCofins && !inss && !irrf) {
      return null;
    }

    Element el = doc.createElementNS(NS, "tribFed");
    if (grupoPisCofins) {
      String cst = valores.getCstPisCofins();
      if (cst == null || !cst.matches("\\d{2}")) {
        throw new NfseException("Informe o CST do PIS/COFINS (2 dígitos): é obrigatório com PIS, COFINS ou CSLL "
            + "retidos ou com apuração própria.");
      }
      Element piscofins = doc.createElementNS(NS, "piscofins");
      addEl(doc, piscofins, "CST", cst);
      if (apuracao != null) {
        addElSe(doc, piscofins, "vBCPisCofins", apuracao.getBaseCalculo());
        addElSe(doc, piscofins, "pAliqPis", apuracao.getAliquotaPis());
        addElSe(doc, piscofins, "pAliqCofins", apuracao.getAliquotaCofins());
        addElSe(doc, piscofins, "vPis", apuracao.getValorPis());
        addElSe(doc, piscofins, "vCofins", apuracao.getValorCofins());
      }
      addEl(doc, piscofins, "tpRetPisCofins", tipoRetencaoPisCofins(pis, cofins, csll));
      el.appendChild(piscofins);
    }
    if (inss) addEl(doc, el, "vRetCP", fmt(valores.getValorInss()));
    if (irrf) addEl(doc, el, "vRetIRRF", fmt(valores.getValorIr()));
    if (pis || cofins || csll) {
      addEl(doc, el, "vRetCSLL", fmt(zeroSeNulo(valores.getValorPis()).add(zeroSeNulo(valores.getValorCofins()))
          .add(zeroSeNulo(valores.getValorCsll()))));
    }
    return el;
  }

  /** {@code tpRetPisCofins} da NT 007: qual das três contribuições foi retida (0 = nenhuma). */
  static String tipoRetencaoPisCofins(boolean pis, boolean cofins, boolean csll) {
    if (pis && cofins) return csll ? "3" : "4";
    if (pis) return csll ? "9" : "5";
    if (cofins) return csll ? "7" : "6";
    return csll ? "8" : "0";
  }

  private void addElSe(Document doc, Element pai, String nome, BigDecimal valor) {
    if (valor != null) addEl(doc, pai, nome, fmt(valor));
  }

  private static BigDecimal zeroSeNulo(BigDecimal valor) {
    return valor != null ? valor : BigDecimal.ZERO;
  }

  private static boolean positivo(BigDecimal valor) {
    return valor != null && valor.signum() > 0;
  }

  // ========================
  // Geração do Id do DPS
  // ========================

  /**
   * Formato do Id conforme XSD TSIdDPS:
   * "DPS" + cLocEmi(7) + tpInsc(1) + CNPJ/CPF(14) + serie(5) + nDPS(15)
   */
  private String gerarId(Dps dps) {
    String cLocEmi = municipioEmissor(dps);
    String tpInsc = dps.getPrestador().isCnpj() ? "2" : "1";
    String inscricao = dps.getPrestador().isCnpj()
        ? dps.getPrestador().getCnpj()
        : String.format("%014d", Long.parseLong(dps.getPrestador().getCpf()));
    String serie = String.format("%05d", Integer.parseInt(dps.getSerie() != null ? dps.getSerie() : "1"));
    String numero = String.format("%015d", Long.parseLong(dps.getNumero() != null ? dps.getNumero() : "1"));

    return "DPS" + cLocEmi + tpInsc + inscricao + serie + numero;
  }

  // ========================
  // Datas e municípios
  // ========================

  /** Agora menos {@link #MARGEM_DH_EMI}, sem recuar para o dia anterior. */
  static ZonedDateTime dhEmi(ZonedDateTime agora) {
    ZonedDateTime recuado = agora.minus(MARGEM_DH_EMI);
    ZonedDateTime inicioDoDia = agora.toLocalDate().atStartOfDay(agora.getZone());
    return recuado.isBefore(inicioDoDia) ? inicioDoDia : recuado;
  }

  private static LocalDate dataCompetencia(Dps dps, LocalDate dataEmissao) {
    LocalDate competencia = dps.getDataCompetencia();
    if (competencia == null) return dataEmissao;
    if (competencia.isAfter(dataEmissao)) {
      throw new NfseException("A data de competência (" + competencia + ") não pode ser posterior à data da emissão ("
          + dataEmissao + ").");
    }
    return competencia;
  }

  private static String codigoTributacaoMunicipal(Servico servico) {
    String codigo = trimToNull(servico.getCodigoTributacaoMunicipal());
    if (codigo == null) return null;
    if (!codigo.matches("\\d{3}")) {
      throw new NfseException("O código de tributação municipal (cTribMun) deve ter 3 dígitos: " + codigo);
    }
    return codigo;
  }

  private static String finNFSe(Dps dps) {
    String fin = dps.getIbscbs() != null ? trimToNull(dps.getIbscbs().getFinNFSe()) : null;
    return fin != null ? fin : "0";
  }

  private static String municipioEmissor(Dps dps) {
    String doPrestador = dps.getPrestador() != null ? trimToNull(dps.getPrestador().getCodigoMunicipio()) : null;
    return doPrestador != null ? doPrestador : dps.getCodigoMunicipio();
  }

  private static String municipioPrestacao(Dps dps) {
    String prestacao = trimToNull(dps.getCodigoMunicipioPrestacao());
    return prestacao != null ? prestacao : municipioEmissor(dps);
  }

  // ========================
  // Utilitários DOM
  // ========================

  private void addEl(Document doc, Element parent, String tag, String value) {
    if (value == null || value.isBlank()) return;
    Element el = doc.createElementNS(NS, tag);
    el.setTextContent(value);
    parent.appendChild(el);
  }

  private String fmt(BigDecimal value) {
    if (value == null) return "0.00";
    return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private static String soDigitos(String v) {
    return v == null ? "" : v.replaceAll("[^0-9]", "");
  }

  private static String trimToNull(String v) {
    if (v == null) return null;
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }

  public String toXmlString(Document doc) {
    try {
      TransformerFactory tf = TransformerFactory.newInstance();
      hardenTransformerFactory(tf);

      Transformer t = tf.newTransformer();
      t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      t.setOutputProperty(OutputKeys.INDENT, "no");

      StringWriter sw = new StringWriter();
      t.transform(new DOMSource(doc), new StreamResult(sw));
      return sw.toString();
    } catch (Exception e) {
      throw new NfseException("Erro ao serializar XML.", e);
    }
  }

  /**
   * Aplica hardening XXE (ACCESS_EXTERNAL_DTD / ACCESS_EXTERNAL_STYLESHEET) na
   * TransformerFactory, tolerando implementações pré-JAXP 1.5 (ex.: Apache Xalan
   * 2.7.x no classpath da aplicação) que lançam IllegalArgumentException nesses
   * atributos. Nesse caso o hardening é best-effort e a serialização segue.
   */
  static void hardenTransformerFactory(TransformerFactory tf) {
    try {
      tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    } catch (IllegalArgumentException ignored) {
      // TransformerFactory sem suporte a JAXP 1.5 — segue sem os atributos.
    }
  }

  // ========================
  // <IBSCBS>
  // ========================

  private Element buildIbscbs(Document doc, Ibscbs ibscbs, Leiaute leiaute) {
    if (ibscbs.getGIbscbs() == null) {
      throw new IllegalStateException("IBSCBS.gIbscbs é obrigatório quando o grupo IBSCBS é informado.");
    }
    if (leiaute == Leiaute.NT_004 && (ibscbs.getCIndOp() == null || ibscbs.getCIndOp().isBlank())) {
      throw new IllegalStateException("IBSCBS.cIndOp é obrigatório quando o grupo IBSCBS é informado.");
    }
    Element el = doc.createElementNS(NS, "IBSCBS");

    if (leiaute == Leiaute.NT_004) {
      if (ibscbs.getIndDest() == null || ibscbs.getIndDest().isBlank()) {
        throw new IllegalStateException("IBSCBS.indDest é obrigatório quando o grupo IBSCBS é informado.");
      }
      if ("1".equals(ibscbs.getIndDest()) && ibscbs.getDest() == null) {
        throw new IllegalStateException("IBSCBS.dest é obrigatório quando indDest=\"1\".");
      }
      addEl(doc, el, "finNFSe", ibscbs.getFinNFSe());
    }
    addEl(doc, el, "cIndOp", ibscbs.getCIndOp());

    // Na NT 009, indDest e dest ficam em infDPS (ver buildInfDps).
    if (leiaute == Leiaute.NT_004) {
      addEl(doc, el, "indDest", ibscbs.getIndDest());
      if (ibscbs.getDest() != null) {
        el.appendChild(buildDest(doc, ibscbs.getDest()));
      }
    }

    // NT 004: <valores><trib><gIBSCBS><CST/><cClassTrib/>; NT 009: <valores><trib><CST/><cClassTrib/>
    GIbscbs g = ibscbs.getGIbscbs();
    Element valores = doc.createElementNS(NS, "valores");
    Element trib = doc.createElementNS(NS, "trib");
    Element destino = trib;
    if (leiaute == Leiaute.NT_004) {
      destino = doc.createElementNS(NS, "gIBSCBS");
      trib.appendChild(destino);
    }
    addEl(doc, destino, "CST", g.getCst());
    addEl(doc, destino, "cClassTrib", g.getCClassTrib());
    valores.appendChild(trib);
    el.appendChild(valores);

    return el;
  }

  private Element buildDest(Document doc, Dest dest) {
    Element el = doc.createElementNS(NS, "dest");

    if (dest.getCnpj() != null) {
      addEl(doc, el, "CNPJ", dest.getCnpj());
    } else if (dest.getCpf() != null) {
      addEl(doc, el, "CPF", dest.getCpf());
    } else if (dest.getNif() != null) {
      addEl(doc, el, "NIF", dest.getNif());
    } else if (dest.getCNaoNIF() != null) {
      addEl(doc, el, "cNaoNIF", dest.getCNaoNIF());
    } else {
      throw new IllegalStateException("dest requer exatamente um de: CNPJ, CPF, NIF ou cNaoNIF.");
    }

    addEl(doc, el, "xNome", dest.getXNome());
    return el;
  }

  private Element buildObra(Document doc, Servico.Obra obra, String codigoMunicipio) {
    Element el = doc.createElementNS(NS, "obra");

    // end — endereço da obra (quando não tem CNO/CEI ou CIB)
    Element end = doc.createElementNS(NS, "end");

    Element endNac = doc.createElementNS(NS, "endNac");
    addEl(doc, endNac, "cMun", obra.getCodigoMunicipio() != null
        ? obra.getCodigoMunicipio() : codigoMunicipio);
    addEl(doc, endNac, "CEP", soDigitos(obra.getCep()));
    end.appendChild(endNac);

    addEl(doc, end, "xLgr", obra.getLogradouro());
    addEl(doc, end, "nro", obra.getNumero());
    if (obra.getComplemento() != null) {
      addEl(doc, end, "xCpl", obra.getComplemento());
    }
    addEl(doc, end, "xBairro", obra.getBairro());
    el.appendChild(end);

    return el;
  }
}