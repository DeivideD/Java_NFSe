package com.coffeetecnologia.nfse.xml;

import com.coffeetecnologia.nfse.exception.NfseException;
import com.coffeetecnologia.nfse.model.dps.*;
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
 *     <dCompet>20260422</dCompet>
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
  private static final String VER_APLIC = "java-nfse-1.0.0";
  private static final ZoneId ZONE_BR = ZoneId.of("America/Sao_Paulo");
  private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
  private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final DocumentBuilder documentBuilder;

  public XmlBuilder() {
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
      Element root = buildDps(doc, dps);
      doc.appendChild(root);
      return doc;
    } catch (NfseException e) {
      throw e;
    } catch (Exception e) {
      throw new NfseException("Erro ao gerar Document do DPS.", e);
    }
  }

  // ========================
  // <DPS versao="1.01">
  // ========================

  private Element buildDps(Document doc, Dps dps) {
    Element el = doc.createElementNS(NS, "DPS");
    el.setAttribute("versao", VERSAO);
    el.appendChild(buildInfDps(doc, dps));
    return el;
  }

  // ========================
  // <infDPS Id="DPS...">
  // ========================

  private Element buildInfDps(Document doc, Dps dps) {
    Element el = doc.createElementNS(NS, "infDPS");

    String id = gerarId(dps);
    el.setAttribute("Id", id);

    // Ambiente: 1=Produção, 2=Homologação
    addEl(doc, el, "tpAmb", "2");

    // Data/hora com timezone: yyyy-MM-dd'T'HH:mm:ssxxx
    ZonedDateTime agora = ZonedDateTime.now(ZONE_BR);
    addEl(doc, el, "dhEmi", agora.format(FMT_DATETIME));

    addEl(doc, el, "verAplic", VER_APLIC);

    // Série: numérica, max 5 dígitos
    addEl(doc, el, "serie", dps.getSerie() != null ? dps.getSerie() : "1");

    // Número DPS: numérico, max 15 dígitos
    addEl(doc, el, "nDPS", dps.getNumero() != null ? dps.getNumero() : "1");

    // Data de competência: AAAAMMDD
    addEl(doc, el, "dCompet", agora.format(FMT_DATE));

    // Tipo de emitente: 1=Prestador, 2=Tomador, 3=Intermediário
    addEl(doc, el, "tpEmit", "1");

    // Código IBGE do município emissor (7 dígitos)
    addEl(doc, el, "cLocEmi", dps.getCodigoMunicipio());

    // Prestador
    el.appendChild(buildPrest(doc, dps.getPrestador()));

    // Tomador (opcional no XSD mas recomendado)
    if (dps.getTomador() != null) {
      el.appendChild(buildToma(doc, dps.getTomador()));
    }

    // Serviço
    el.appendChild(buildServ(doc, dps.getServico(), dps.getCodigoMunicipio()));

    // Valores
    el.appendChild(buildValores(doc, dps.getValores()));

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
    // opSimpNac: 1=Não Optante, 2=MEI, 3=ME/EPP
    addEl(doc, regTrib, "opSimpNac", "1");
    // regEspTrib: 0=Nenhum
    addEl(doc, regTrib, "regEspTrib", "0");
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

    return el;
  }

  // ========================
  // <serv>
  // ========================

  private Element buildServ(Document doc, Servico servico, String codigoMunicipio) {
    Element el = doc.createElementNS(NS, "serv");

    // <locPrest>
    Element locPrest = doc.createElementNS(NS, "locPrest");
    addEl(doc, locPrest, "cLocPrestacao", codigoMunicipio);
    el.appendChild(locPrest);

    // <cServ>
    Element cServ = doc.createElementNS(NS, "cServ");
    addEl(doc, cServ, "cTribNac", servico.getCodigoServico());
    addEl(doc, cServ, "xDescServ", servico.getDescricao());
    if (servico.getCodigoNbs() != null) {
      addEl(doc, cServ, "cNBS", servico.getCodigoNbs());
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

    // <totTrib> — total aproximado de tributos (obrigatório)
    Element totTrib = doc.createElementNS(NS, "totTrib");
    // indTotTrib = 0 significa "não informar valor estimado"
    addEl(doc, totTrib, "indTotTrib", "0");
    trib.appendChild(totTrib);

    el.appendChild(trib);

    return el;
  }

  // ========================
  // Geração do Id do DPS
  // ========================

  /**
   * Formato do Id conforme XSD TSIdDPS:
   * "DPS" + cLocEmi(7) + tpInsc(1) + CNPJ/CPF(14) + serie(5) + nDPS(15)
   */
  private String gerarId(Dps dps) {
    String cLocEmi = dps.getCodigoMunicipio();
    String tpInsc = dps.getPrestador().isCnpj() ? "2" : "1";
    String inscricao = dps.getPrestador().isCnpj()
        ? dps.getPrestador().getCnpj()
        : String.format("%014d", Long.parseLong(dps.getPrestador().getCpf()));
    String serie = String.format("%05d", Integer.parseInt(dps.getSerie() != null ? dps.getSerie() : "1"));
    String numero = String.format("%015d", Long.parseLong(dps.getNumero() != null ? dps.getNumero() : "1"));

    return "DPS" + cLocEmi + tpInsc + inscricao + serie + numero;
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

  public String toXmlString(Document doc) {
    try {
      TransformerFactory tf = TransformerFactory.newInstance();
      tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

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

  private Element buildObra(Document doc, Servico.Obra obra, String codigoMunicipio) {
    Element el = doc.createElementNS(NS, "obra");

    // end — endereço da obra (quando não tem CNO/CEI ou CIB)
    Element end = doc.createElementNS(NS, "end");

    Element endNac = doc.createElementNS(NS, "endNac");
    addEl(doc, endNac, "cMun", obra.getCodigoMunicipio() != null
        ? obra.getCodigoMunicipio() : codigoMunicipio);
    addEl(doc, endNac, "CEP", obra.getCep().replaceAll("[^0-9]", ""));
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