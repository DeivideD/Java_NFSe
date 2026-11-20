package com.coffeetecnologia.nfse.xml;

import com.coffeetecnologia.nfse.config.Ambiente;
import com.coffeetecnologia.nfse.config.Leiaute;
import com.coffeetecnologia.nfse.exception.NfseException;
import com.coffeetecnologia.nfse.model.dps.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("XmlBuilder - regime tributário, competência, municípios e endereço do tomador")
class DpsCamposFiscaisTest {

  private static final String NS = "http://www.sped.fazenda.gov.br/nfse";
  private static final ZoneId ZONE_BR = ZoneId.of("America/Sao_Paulo");

  private final XmlBuilder xmlBuilder = new XmlBuilder(Ambiente.PRODUCAO_RESTRITA, Leiaute.NT_009);

  // --- cTribMun -----------------------------------------------------------------------------

  @Test
  @DisplayName("cTribMun vai logo depois do cTribNac e valida no XSD")
  void codigoTributacaoMunicipal() throws Exception {
    Servico servico = Servico.builder().codigoServico("010601").codigoTributacaoMunicipal("001")
        .descricao("Desenvolvimento de software").codigoNbs("115011000").build();
    Document doc = xmlBuilder.buildDocument(dps().servico(servico).build());

    Element cServ = (Element) doc.getElementsByTagNameNS(NS, "cServ").item(0);
    assertEquals("cTribNac", cServ.getFirstChild().getLocalName());
    assertEquals("cTribMun", cServ.getFirstChild().getNextSibling().getLocalName());
    assertEquals("001", texto(doc, "cTribMun"));
    validarNosDoisLeiautes(doc);
  }

  @Test
  @DisplayName("Sem cTribMun o elemento não aparece")
  void semCodigoTributacaoMunicipal() throws Exception {
    Document doc = xmlBuilder.buildDocument(dps().build());
    assertEquals(0, doc.getElementsByTagNameNS(NS, "cTribMun").getLength());
  }

  @Test
  @DisplayName("cTribMun fora do padrão de 3 dígitos é recusado antes de enviar")
  void codigoTributacaoMunicipalInvalido() {
    Servico servico = Servico.builder().codigoServico("010601").codigoTributacaoMunicipal("01").descricao("x").build();
    assertThrows(NfseException.class, () -> xmlBuilder.buildDocument(dps().servico(servico).build()));
  }

  // --- regTrib ------------------------------------------------------------------------------

  @Test
  @DisplayName("Sem regime informado, mantém 'não optante' e regime especial 'nenhum'")
  void regimePadrao() throws Exception {
    Document doc = xmlBuilder.buildDocument(dps().build());

    assertEquals("1", texto(doc, "opSimpNac"));
    assertEquals(0, doc.getElementsByTagNameNS(NS, "regApTribSN").getLength());
    assertEquals("0", texto(doc, "regEspTrib"));
    validarNosDoisLeiautes(doc);
  }

  @Test
  @DisplayName("Optante ME/EPP com regime de apuração e regime especial")
  void optanteMeEpp() throws Exception {
    Prestador prestador = prestador()
        .opcaoSimplesNacional(OpcaoSimplesNacional.ME_EPP)
        .regimeApuracaoSimplesNacional(RegimeApuracaoSimplesNacional.FEDERAIS_E_MUNICIPAL_PELO_SN)
        .regimeEspecialTributacao(RegimeEspecialTributacao.SOCIEDADE_DE_PROFISSIONAIS)
        .build();
    Document doc = xmlBuilder.buildDocument(dps().prestador(prestador).build());

    assertEquals("3", texto(doc, "opSimpNac"));
    assertEquals("1", texto(doc, "regApTribSN"));
    assertEquals("6", texto(doc, "regEspTrib"));
    validarNosDoisLeiautes(doc);
  }

  @Test
  @DisplayName("MEI vai como opSimpNac 2")
  void mei() {
    Prestador prestador = prestador().opcaoSimplesNacional(OpcaoSimplesNacional.MEI).build();

    assertEquals("2", texto(xmlBuilder.buildDocument(dps().prestador(prestador).build()), "opSimpNac"));
  }

  @Test
  @DisplayName("Regime de apuração do SN fora de ME/EPP é recusado")
  void regimeApuracaoSoParaMeEpp() {
    Prestador prestador = prestador()
        .opcaoSimplesNacional(OpcaoSimplesNacional.MEI)
        .regimeApuracaoSimplesNacional(RegimeApuracaoSimplesNacional.FEDERAIS_E_MUNICIPAL_POR_FORA)
        .build();

    assertThrows(NfseException.class, () -> xmlBuilder.build(dps().prestador(prestador).build()));
  }

  @Test
  @DisplayName("ME/EPP informa pTotTribSN no lugar do indTotTrib")
  void totalTributosSimples() throws Exception {
    Valores valores = Valores.builder().valorServico(new BigDecimal("1500.00"))
        .percentualTributosSimplesNacional(new BigDecimal("6")).build();
    Document doc = xmlBuilder.buildDocument(dps().valores(valores).build());

    assertEquals("6.00", texto(doc, "pTotTribSN"));
    assertNull(texto(doc, "indTotTrib"));
    validarNosDoisLeiautes(doc);
  }

  // --- datas ---------------------------------------------------------------------------------

  @Test
  @DisplayName("Competência informada vai em dCompet")
  void competenciaInformada() {
    LocalDate competencia = LocalDate.now(ZONE_BR).minusMonths(1).withDayOfMonth(10);

    assertEquals(competencia.toString(),
        texto(xmlBuilder.buildDocument(dps().dataCompetencia(competencia).build()), "dCompet"));
  }

  @Test
  @DisplayName("Sem competência, dCompet é a data do dhEmi")
  void competenciaPadrao() {
    Document doc = xmlBuilder.buildDocument(dps().build());

    assertEquals(texto(doc, "dhEmi").substring(0, 10), texto(doc, "dCompet"));
  }

  @Test
  @DisplayName("Competência futura é recusada")
  void competenciaFutura() {
    Dps dps = dps().dataCompetencia(LocalDate.now(ZONE_BR).plusDays(2)).build();

    NfseException e = assertThrows(NfseException.class, () -> xmlBuilder.build(dps));
    assertTrue(e.getMessage().contains("competência"));
  }

  @Test
  @DisplayName("O recuo do dhEmi não volta para o dia anterior")
  void dhEmiNaoCruzaMeiaNoite() {
    ZonedDateTime meiaNoiteEDois = ZonedDateTime.of(2026, 12, 1, 0, 2, 0, 0, ZONE_BR);
    ZonedDateTime meioDia = ZonedDateTime.of(2026, 12, 1, 12, 0, 0, 0, ZONE_BR);

    assertEquals(ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZONE_BR), XmlBuilder.dhEmi(meiaNoiteEDois));
    assertEquals(meioDia.minusMinutes(5), XmlBuilder.dhEmi(meioDia));
  }

  // --- municípios ----------------------------------------------------------------------------

  @Test
  @DisplayName("Município emissor vem do prestador e o local de prestação pode ser outro")
  void municipioEmissorEPrestacao() throws Exception {
    Dps dps = dps().codigoMunicipio("3550308").codigoMunicipioPrestacao("2927408").build();
    Document doc = xmlBuilder.buildDocument(dps);

    assertEquals("2304400", texto(doc, "cLocEmi"));
    assertEquals("2927408", texto(doc, "cLocPrestacao"));
    String id = ((Element) doc.getElementsByTagNameNS(NS, "infDPS").item(0)).getAttribute("Id");
    assertTrue(id.startsWith("DPS2304400"), "o Id do DPS usa o município emissor: " + id);
    validarNosDoisLeiautes(doc);
  }

  @Test
  @DisplayName("Sem município no prestador, o da DPS é emissor e local de prestação")
  void municipioSoNaDps() {
    Prestador prestador = Prestador.builder().cnpj("00000000000191").build();
    Document doc = xmlBuilder.buildDocument(dps().prestador(prestador).codigoMunicipio("2304400").build());

    assertEquals("2304400", texto(doc, "cLocEmi"));
    assertEquals("2304400", texto(doc, "cLocPrestacao"));
  }

  // --- endereço do tomador -------------------------------------------------------------------

  @Test
  @DisplayName("Endereço completo do tomador gera <end> válido no XSD")
  void enderecoCompleto() throws Exception {
    Document doc = xmlBuilder.buildDocument(dps().tomador(tomador(endereco().build())).build());

    assertEquals("2304400", texto(doc, "cMun"));
    assertEquals("60115170", texto(doc, "CEP"));
    assertEquals("Rua A", texto(doc, "xLgr"));
    validarNosDoisLeiautes(doc);
  }

  @Test
  @DisplayName("Endereço incompleto falha dizendo o que falta, em vez de sumir do XML")
  void enderecoIncompleto() {
    Tomador.Endereco endereco = endereco().numero(" ").cep("60115-17").codigoMunicipio("230440").build();
    Dps dps = dps().tomador(tomador(endereco)).build();

    NfseException e = assertThrows(NfseException.class, () -> xmlBuilder.build(dps));
    assertTrue(e.getMessage().contains("número"), e.getMessage());
    assertTrue(e.getMessage().contains("CEP"), e.getMessage());
    assertTrue(e.getMessage().contains("IBGE"), e.getMessage());
    assertFalse(e.getMessage().contains("bairro"), e.getMessage());
  }

  @Test
  @DisplayName("Sem endereço, o tomador vai sem <end>")
  void semEndereco() {
    Document doc = xmlBuilder.buildDocument(dps().build());

    assertEquals(0, doc.getElementsByTagNameNS(NS, "end").getLength());
  }

  // ========================
  // Utilitários
  // ========================

  private static Dps.DpsBuilder dps() {
    return Dps.builder()
        .numero("1")
        .serie("1")
        .codigoMunicipio("2304400")
        .prestador(prestador().build())
        .tomador(tomador(null))
        .servico(Servico.builder().codigoServico("010700").descricao("Desenvolvimento de software").build())
        .valores(Valores.builder().valorServico(new BigDecimal("1500.00")).aliquotaIss(new BigDecimal("0.05")).build());
  }

  private static Prestador.PrestadorBuilder prestador() {
    return Prestador.builder().cnpj("00000000000191").inscricaoMunicipal("12345").codigoMunicipio("2304400");
  }

  private static Tomador tomador(Tomador.Endereco endereco) {
    return Tomador.builder().cnpj("11222333000181").nome("Cliente Ltda").endereco(endereco).build();
  }

  private static Tomador.Endereco.EnderecoBuilder endereco() {
    return Tomador.Endereco.builder()
        .logradouro("Rua A")
        .numero("100")
        .bairro("Centro")
        .codigoMunicipio("2304400")
        .uf("CE")
        .cep("60115-170");
  }

  private static String texto(Document doc, String tag) {
    NodeList nodes = doc.getElementsByTagNameNS(NS, tag);
    return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
  }

  /** O mesmo DPS gerado em cada leiaute passa no XSD correspondente. */
  private void validarNosDoisLeiautes(Document nt009) throws Exception {
    validarContraXsd(xmlBuilder.toXmlString(nt009), "schemas/nt009/DPS_v1.01.xsd");
  }

  static void validarContraXsd(String xml, String xsd) throws Exception {
    URL xsdUrl = DpsCamposFiscaisTest.class.getClassLoader().getResource(xsd);
    assertNotNull(xsdUrl, xsd);
    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(xsdUrl).newValidator()
        .validate(new StreamSource(new StringReader(xml)));
  }
}
