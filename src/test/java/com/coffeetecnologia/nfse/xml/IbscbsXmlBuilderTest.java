package com.coffeetecnologia.nfse.xml;

import com.coffeetecnologia.nfse.model.dps.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IBSCBS - Geração de XML do grupo IBS/CBS na DPS")
class IbscbsXmlBuilderTest {

  private static final String NS = "http://www.sped.fazenda.gov.br/nfse";

  private XmlBuilder xmlBuilder;
  private Dps dpsBase;

  @BeforeEach
  void setUp() {
    xmlBuilder = new XmlBuilder();
    dpsBase = criarDpsBase();
  }

  // ========================
  // Testes de regressão
  // ========================

  @Test
  @DisplayName("Regressão: DPS sem IBSCBS não emite a tag <IBSCBS>")
  void semIbscbs_naoEmiteTagIbscbs() {
    String xml = xmlBuilder.build(dpsBase);

    assertFalse(xml.contains("<IBSCBS"), "XML sem grupo IBSCBS não deve conter a tag <IBSCBS>");
    assertFalse(xml.contains("</IBSCBS>"), "XML sem grupo IBSCBS não deve conter a tag </IBSCBS>");
  }

  @Test
  @DisplayName("Regressão: DPS sem IBSCBS continua tendo <serv> e <valores> normalmente")
  void semIbscbs_mantemEstruturaExistente() {
    Document doc = xmlBuilder.buildDocument(dpsBase);

    assertElementExiste(doc, "serv");
    assertElementExiste(doc, "valores");
    assertElementExiste(doc, "cServ");
    assertElementExiste(doc, "tribMun");
    assertElementExiste(doc, "totTrib");
  }

  @Test
  @DisplayName("Regressão: DPS sem IBSCBS ainda contém CNPJ do prestador e valor do serviço")
  void semIbscbs_mantemConteudoExistente() {
    String xml = xmlBuilder.build(dpsBase);

    assertTrue(xml.contains("00000000000191"), "CNPJ do prestador deve estar presente");
    assertTrue(xml.contains("1500.00"), "Valor do serviço deve estar presente");
    assertTrue(xml.contains("Desenvolvimento de software"), "Descrição do serviço deve estar presente");
  }

  // ========================
  // Testes de geração com IBSCBS (indDest=0, sem dest)
  // ========================

  @Test
  @DisplayName("IBSCBS com indDest=0: emite tag <IBSCBS> após <valores>")
  void comIbscbs_emiteTagIbscbsAposValores() {
    Dps dps = dpsBase.toBuilder()
        .ibscbs(ibscbsMinimo())
        .build();

    String xml = xmlBuilder.build(dps);

    assertTrue(xml.contains("<IBSCBS"), "XML deve conter a tag <IBSCBS>");
    // <IBSCBS> deve aparecer depois de </valores>
    int posValores = xml.indexOf("</valores>");
    int posIbscbs = xml.indexOf("<IBSCBS");
    assertTrue(posIbscbs > posValores, "<IBSCBS> deve aparecer após </valores> no XML");
  }

  @Test
  @DisplayName("IBSCBS com indDest=0: emite finNFSe, cIndOp e indDest corretamente")
  void comIbscbs_emiteCamposRaiz() {
    Dps dps = dpsBase.toBuilder()
        .ibscbs(ibscbsMinimo())
        .build();

    String xml = xmlBuilder.build(dps);

    assertTrue(xml.contains("<finNFSe>0</finNFSe>"), "finNFSe deve ser '0'");
    assertTrue(xml.contains("<cIndOp>010000</cIndOp>"), "cIndOp deve estar presente");
    assertTrue(xml.contains("<indDest>0</indDest>"), "indDest deve ser '0'");
  }

  @Test
  @DisplayName("IBSCBS com indDest=0: emite gIBSCBS com CST e cClassTrib corretos")
  void comIbscbs_emiteGIbscbsComCstEClassTrib() {
    Dps dps = dpsBase.toBuilder()
        .ibscbs(ibscbsMinimo())
        .build();

    String xml = xmlBuilder.build(dps);

    assertTrue(xml.contains("<gIBSCBS>"), "Deve conter o elemento gIBSCBS");
    assertTrue(xml.contains("<CST>101</CST>"), "CST deve ser '101'");
    assertTrue(xml.contains("<cClassTrib>010100</cClassTrib>"), "cClassTrib deve ser '010100'");
  }

  @Test
  @DisplayName("IBSCBS com indDest=0: não emite tag <dest>")
  void comIbscbs_indDest0_naoEmiteDest() {
    Dps dps = dpsBase.toBuilder()
        .ibscbs(ibscbsMinimo())
        .build();

    String xml = xmlBuilder.build(dps);

    assertFalse(xml.contains("<dest>"), "XML com indDest=0 não deve conter tag <dest>");
  }

  @Test
  @DisplayName("IBSCBS: estrutura aninhada correta — valores > trib > gIBSCBS")
  void comIbscbs_estruturaAninhada() {
    Dps dps = dpsBase.toBuilder()
        .ibscbs(ibscbsMinimo())
        .build();

    Document doc = xmlBuilder.buildDocument(dps);

    // Confirmar via DOM que IBSCBS/valores/trib/gIBSCBS existe
    NodeList ibscbsList = doc.getElementsByTagNameNS(NS, "IBSCBS");
    assertEquals(1, ibscbsList.getLength(), "Deve haver exatamente um elemento IBSCBS");

    Element ibscbsEl = (Element) ibscbsList.item(0);
    NodeList valoresList = ibscbsEl.getElementsByTagNameNS(NS, "valores");
    assertEquals(1, valoresList.getLength(), "IBSCBS deve conter um elemento valores");

    Element valoresEl = (Element) valoresList.item(0);
    NodeList tribList = valoresEl.getElementsByTagNameNS(NS, "trib");
    assertEquals(1, tribList.getLength(), "valores deve conter um elemento trib");

    Element tribEl = (Element) tribList.item(0);
    NodeList gList = tribEl.getElementsByTagNameNS(NS, "gIBSCBS");
    assertEquals(1, gList.getLength(), "trib deve conter um elemento gIBSCBS");
  }

  // ========================
  // Testes com dest (indDest=1)
  // ========================

  @Test
  @DisplayName("IBSCBS com indDest=1 e dest: emite <dest> com CNPJ e xNome")
  void comIbscbs_indDest1_emiteDestComCnpj() {
    Dest dest = Dest.builder()
        .cnpj("00000000000272")
        .xNome("Empresa Destinatária Ltda")
        .build();

    Ibscbs ibscbs = Ibscbs.builder()
        .cIndOp("010000")
        .indDest("1")
        .dest(dest)
        .gIbscbs(GIbscbs.builder().cst("101").cClassTrib("010100").build())
        .build();

    String xml = xmlBuilder.build(dpsBase.toBuilder().ibscbs(ibscbs).build());

    assertTrue(xml.contains("<dest>"), "XML deve conter tag <dest>");
    assertTrue(xml.contains("<CNPJ>00000000000272</CNPJ>"), "CNPJ do destinatário deve estar em dest");
    assertTrue(xml.contains("<xNome>Empresa Destinatária Ltda</xNome>"), "xNome do destinatário deve estar presente");
  }

  @Test
  @DisplayName("IBSCBS com indDest=1 e dest com CPF: emite <CPF> em dest")
  void comIbscbs_indDest1_emiteDestComCpf() {
    Dest dest = Dest.builder()
        .cpf("00000000011")
        .xNome("João Destinatário")
        .build();

    Ibscbs ibscbs = Ibscbs.builder()
        .cIndOp("010000")
        .indDest("1")
        .dest(dest)
        .gIbscbs(GIbscbs.builder().cst("101").cClassTrib("010100").build())
        .build();

    String xml = xmlBuilder.build(dpsBase.toBuilder().ibscbs(ibscbs).build());

    assertTrue(xml.contains("<CPF>00000000011</CPF>"), "CPF do destinatário deve estar em dest");
    assertTrue(xml.contains("<xNome>João Destinatário</xNome>"), "xNome deve estar presente");
  }

  @Test
  @DisplayName("IBSCBS com indDest=1 e dest com cNaoNIF: emite <cNaoNIF> em dest")
  void comIbscbs_indDest1_emiteDestComCNaoNif() {
    Dest dest = Dest.builder()
        .cNaoNIF("2")
        .xNome("Destinatário Exterior")
        .build();

    Ibscbs ibscbs = Ibscbs.builder()
        .cIndOp("010000")
        .indDest("1")
        .dest(dest)
        .gIbscbs(GIbscbs.builder().cst("101").cClassTrib("010100").build())
        .build();

    String xml = xmlBuilder.build(dpsBase.toBuilder().ibscbs(ibscbs).build());

    assertTrue(xml.contains("<cNaoNIF>2</cNaoNIF>"), "cNaoNIF deve estar presente em dest");
  }

  // ========================
  // Testes de validação (guard clauses)
  // ========================

  @Test
  @DisplayName("Deve lançar exceção quando IBSCBS é informado sem gIbscbs")
  void semGIbscbs_lancaExcecao() {
    Ibscbs ibscbsSemG = Ibscbs.builder()
        .cIndOp("010000")
        .indDest("0")
        .gIbscbs(null)
        .build();

    Dps dps = dpsBase.toBuilder().ibscbs(ibscbsSemG).build();

    assertThrows(IllegalStateException.class, () -> xmlBuilder.build(dps),
        "Deve lançar IllegalStateException quando gIbscbs é null");
  }

  @Test
  @DisplayName("Deve lançar exceção quando IBSCBS é informado sem cIndOp")
  void semCIndOp_lancaExcecao() {
    Ibscbs ibscbsSemIndOp = Ibscbs.builder()
        .indDest("0")
        .gIbscbs(GIbscbs.builder().cst("101").cClassTrib("010100").build())
        .build();

    Dps dps = dpsBase.toBuilder().ibscbs(ibscbsSemIndOp).build();

    assertThrows(IllegalStateException.class, () -> xmlBuilder.build(dps),
        "Deve lançar IllegalStateException quando cIndOp é null");
  }

  @Test
  @DisplayName("Deve lançar exceção quando indDest=1 e dest é null")
  void indDest1_semDest_lancaExcecao() {
    Ibscbs ibscbsSemDest = Ibscbs.builder()
        .cIndOp("010000")
        .indDest("1")
        .gIbscbs(GIbscbs.builder().cst("101").cClassTrib("010100").build())
        .build();

    Dps dps = dpsBase.toBuilder().ibscbs(ibscbsSemDest).build();

    assertThrows(IllegalStateException.class, () -> xmlBuilder.build(dps),
        "Deve lançar IllegalStateException quando indDest=1 e dest é null");
  }

  @Test
  @DisplayName("Deve lançar exceção quando dest não tem nenhum campo de identificação")
  void dest_semIdentificacao_lancaExcecao() {
    Dest destSemId = Dest.builder()
        .xNome("Sem Identificação")
        .build();

    Ibscbs ibscbs = Ibscbs.builder()
        .cIndOp("010000")
        .indDest("1")
        .dest(destSemId)
        .gIbscbs(GIbscbs.builder().cst("101").cClassTrib("010100").build())
        .build();

    Dps dps = dpsBase.toBuilder().ibscbs(ibscbs).build();

    assertThrows(IllegalStateException.class, () -> xmlBuilder.build(dps),
        "Deve lançar IllegalStateException quando dest não tem identificação");
  }

  // ========================
  // Validação contra XSD oficial
  // (usa fixture com cTribNac de 6 dígitos — XSD v1.01 jul/2026: TSCodTribNac = [0-9]{6})
  // ========================

  @Test
  @DisplayName("XSD: DPS sem IBSCBS válido contra schema oficial v1.01")
  void semIbscbs_xmlValidoContraXsd() {
    String xml = xmlBuilder.build(criarDpsParaXsd());
    assertDoesNotThrow(() -> validarContraXsd(xml),
        "DPS sem IBSCBS deve ser válido contra o XSD oficial");
  }

  @Test
  @DisplayName("XSD: DPS com IBSCBS (indDest=0) válido contra schema oficial v1.01")
  void comIbscbs_xmlValidoContraXsd() {
    Dps dps = criarDpsParaXsd().toBuilder()
        .ibscbs(ibscbsMinimo())
        .build();

    String xml = xmlBuilder.build(dps);
    assertDoesNotThrow(() -> validarContraXsd(xml),
        "DPS com IBSCBS deve ser válido contra o XSD oficial");
  }

  @Test
  @DisplayName("XSD: DPS com IBSCBS e dest (indDest=1) válido contra schema oficial v1.01")
  void comIbscbsEDest_xmlValidoContraXsd() {
    Dest dest = Dest.builder()
        .cnpj("00000000000272")
        .xNome("Empresa Destinatária Ltda")
        .build();

    Ibscbs ibscbs = Ibscbs.builder()
        .cIndOp("010000")
        .indDest("1")
        .dest(dest)
        .gIbscbs(GIbscbs.builder().cst("101").cClassTrib("010100").build())
        .build();

    String xml = xmlBuilder.build(criarDpsParaXsd().toBuilder().ibscbs(ibscbs).build());
    assertDoesNotThrow(() -> validarContraXsd(xml),
        "DPS com IBSCBS e dest deve ser válido contra o XSD oficial");
  }

  // ========================
  // Utilitários
  // ========================

  private void validarContraXsd(String xml) throws SAXException, java.io.IOException {
    URL xsdUrl = getClass().getClassLoader().getResource("xsd/DPS_v1.01.xsd");
    assertNotNull(xsdUrl, "XSD DPS_v1.01.xsd deve estar em src/test/resources/xsd/");

    SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = sf.newSchema(xsdUrl);
    Validator validator = schema.newValidator();
    validator.validate(new StreamSource(new StringReader(xml)));
  }

  private void assertElementExiste(Document doc, String tagLocalName) {
    NodeList nodes = doc.getElementsByTagNameNS(NS, tagLocalName);
    assertTrue(nodes.getLength() > 0, "Elemento <" + tagLocalName + "> deve existir no XML");
  }

  private Ibscbs ibscbsMinimo() {
    return Ibscbs.builder()
        .cIndOp("010000")
        .indDest("0")
        .gIbscbs(GIbscbs.builder()
            .cst("101")
            .cClassTrib("010100")
            .build())
        .build();
  }

  /**
   * DPS com cTribNac de 6 dígitos para validação contra o XSD oficial
   * (TSCodTribNac = [0-9]{6}: item 01 + subitem 07 + desdobro 00 = "010700").
   * O fixture padrão usa "0107" que é legado — não alterar para não quebrar testes existentes.
   */
  private Dps criarDpsParaXsd() {
    return criarDpsBase().toBuilder()
        .servico(Servico.builder()
            .codigoServico("010700")
            .descricao("Desenvolvimento de software")
            .build())
        .build();
  }

  private Dps criarDpsBase() {
    return Dps.builder()
        .numero("1")
        .serie("1")
        .prestador(Prestador.builder()
            .cnpj("00000000000191")
            .inscricaoMunicipal("12345")
            .codigoMunicipio("2304400")
            .build())
        .tomador(Tomador.builder()
            .cpf("00000000000")
            .nome("João Silva")
            .build())
        .servico(Servico.builder()
            .codigoServico("0107")
            .descricao("Desenvolvimento de software")
            .build())
        .valores(Valores.builder()
            .valorServico(new BigDecimal("1500.00"))
            .aliquotaIss(new BigDecimal("0.05"))
            .build())
        .codigoMunicipio("2304400")
        .build();
  }
}
