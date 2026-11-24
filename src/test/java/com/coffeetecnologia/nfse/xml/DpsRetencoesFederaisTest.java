package com.coffeetecnologia.nfse.xml;

import com.coffeetecnologia.nfse.config.Ambiente;
import com.coffeetecnologia.nfse.config.Leiaute;
import com.coffeetecnologia.nfse.exception.NfseException;
import com.coffeetecnologia.nfse.model.dps.ApuracaoPisCofins;
import com.coffeetecnologia.nfse.model.dps.Dps;
import com.coffeetecnologia.nfse.model.dps.Prestador;
import com.coffeetecnologia.nfse.model.dps.Servico;
import com.coffeetecnologia.nfse.model.dps.Tomador;
import com.coffeetecnologia.nfse.model.dps.Valores;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("XmlBuilder - retenções federais (tribFed)")
class DpsRetencoesFederaisTest {

  private static final String NS = "http://www.sped.fazenda.gov.br/nfse";

  private final XmlBuilder xmlBuilder = new XmlBuilder(Ambiente.PRODUCAO_RESTRITA, Leiaute.NT_009);

  @Test
  @DisplayName("Obra com ISS e INSS retidos: vRetCP entre tribMun e totTrib, local da prestação no tomador")
  void inssRetidoComIssRetido() throws Exception {
    Valores valores = Valores.builder()
        .valorServico(new BigDecimal("28000.00"))
        .aliquotaIss(new BigDecimal("0.05"))
        .issRetido(true)
        .valorInss(new BigDecimal("980.00"))
        .build();
    Document doc = xmlBuilder.buildDocument(dps(valores).codigoMunicipioPrestacao("2611606").build());

    Element trib = (Element) doc.getElementsByTagNameNS(NS, "trib").item(0);
    assertEquals(List.of("tribMun", "tribFed", "totTrib"), filhos(trib));
    Element tribFed = (Element) doc.getElementsByTagNameNS(NS, "tribFed").item(0);
    assertEquals(List.of("vRetCP"), filhos(tribFed));
    assertEquals("980.00", tribFed.getTextContent());
    assertEquals("2", texto(doc, "tpRetISSQN"));
    assertEquals("2611606", texto(doc, "cLocPrestacao"));
    assertEquals(new BigDecimal("25620.00"), valores.getValorLiquido());
    DpsCamposFiscaisTest.validarContraXsd(xmlBuilder.toXmlString(doc), "schemas/nt009/DPS_v1.01.xsd");
  }

  @Test
  @DisplayName("PIS, COFINS e CSLL retidos: somados em vRetCSLL e tpRetPisCofins 3 no piscofins, na ordem do XSD")
  void contribuicoesRetidasSomadasEmVRetCsll() throws Exception {
    Valores valores = Valores.builder()
        .valorServico(new BigDecimal("10000.00"))
        .valorInss(new BigDecimal("1100.00"))
        .valorIr(new BigDecimal("150.00"))
        .valorPis(new BigDecimal("65.00"))
        .valorCofins(new BigDecimal("300.00"))
        .valorCsll(new BigDecimal("100.00"))
        .cstPisCofins("01")
        .build();
    Document doc = xmlBuilder.buildDocument(dps(valores).build());

    Element tribFed = (Element) doc.getElementsByTagNameNS(NS, "tribFed").item(0);
    assertEquals(List.of("piscofins", "vRetCP", "vRetIRRF", "vRetCSLL"), filhos(tribFed));
    Element piscofins = (Element) doc.getElementsByTagNameNS(NS, "piscofins").item(0);
    assertEquals(List.of("CST", "tpRetPisCofins"), filhos(piscofins));
    assertEquals("3", texto(doc, "tpRetPisCofins"));
    assertEquals("465.00", texto(doc, "vRetCSLL"));
    assertEquals(0, doc.getElementsByTagNameNS(NS, "vPis").getLength());
    assertEquals(new BigDecimal("8285.00"), valores.getValorLiquido());
    validarNosDoisLeiautes(dps(valores).build());
  }

  @Test
  @DisplayName("Tipo de retenção para cada combinação de PIS, COFINS e CSLL (NT 007)")
  void tipoDeRetencaoPorCombinacao() {
    assertEquals("0", XmlBuilder.tipoRetencaoPisCofins(false, false, false));
    assertEquals("3", XmlBuilder.tipoRetencaoPisCofins(true, true, true));
    assertEquals("4", XmlBuilder.tipoRetencaoPisCofins(true, true, false));
    assertEquals("5", XmlBuilder.tipoRetencaoPisCofins(true, false, false));
    assertEquals("6", XmlBuilder.tipoRetencaoPisCofins(false, true, false));
    assertEquals("7", XmlBuilder.tipoRetencaoPisCofins(false, true, true));
    assertEquals("8", XmlBuilder.tipoRetencaoPisCofins(false, false, true));
    assertEquals("9", XmlBuilder.tipoRetencaoPisCofins(true, false, true));
  }

  @Test
  @DisplayName("Apuração própria sem retenção: vBC, alíquotas, vPis e vCofins com tpRetPisCofins 0")
  void apuracaoPropria() throws Exception {
    Valores valores = Valores.builder()
        .valorServico(new BigDecimal("1000.00"))
        .cstPisCofins("01")
        .apuracaoPisCofins(ApuracaoPisCofins.builder()
            .baseCalculo(new BigDecimal("1000.00"))
            .aliquotaPis(new BigDecimal("0.65"))
            .aliquotaCofins(new BigDecimal("3.00"))
            .valorPis(new BigDecimal("6.50"))
            .valorCofins(new BigDecimal("30.00"))
            .build())
        .build();
    Document doc = xmlBuilder.buildDocument(dps(valores).build());

    Element piscofins = (Element) doc.getElementsByTagNameNS(NS, "piscofins").item(0);
    assertEquals(List.of("CST", "vBCPisCofins", "pAliqPis", "pAliqCofins", "vPis", "vCofins", "tpRetPisCofins"),
        filhos(piscofins));
    assertEquals("0", texto(doc, "tpRetPisCofins"));
    assertEquals(0, doc.getElementsByTagNameNS(NS, "vRetCSLL").getLength());
    assertEquals(new BigDecimal("1000.00"), valores.getValorLiquido());
    validarNosDoisLeiautes(dps(valores).build());
  }

  @Test
  @DisplayName("Só CSLL retida: tpRetPisCofins 8 e vRetCSLL com a CSLL")
  void soCsll() throws Exception {
    Valores valores = Valores.builder().valorServico(new BigDecimal("1000.00"))
        .valorCsll(new BigDecimal("10.00")).cstPisCofins("01").build();
    Document doc = xmlBuilder.buildDocument(dps(valores).build());

    assertEquals("8", texto(doc, "tpRetPisCofins"));
    assertEquals("10.00", texto(doc, "vRetCSLL"));
  }

  @Test
  @DisplayName("Sem retenção federal o tribFed não aparece")
  void semRetencao() throws Exception {
    Document doc = xmlBuilder.buildDocument(dps(Valores.builder().valorServico(new BigDecimal("100.00")).build()).build());
    assertEquals(0, doc.getElementsByTagNameNS(NS, "tribFed").getLength());
  }

  @Test
  @DisplayName("Contribuição retida sem CST é recusada antes de enviar")
  void semCstRecusado() {
    Valores valores = Valores.builder().valorServico(new BigDecimal("100.00")).valorPis(new BigDecimal("0.65")).build();
    assertThrows(NfseException.class, () -> xmlBuilder.buildDocument(dps(valores).build()));
  }

  private static void validarNosDoisLeiautes(Dps dps) throws Exception {
    XmlBuilder nt009 = new XmlBuilder(Ambiente.PRODUCAO_RESTRITA, Leiaute.NT_009);
    XmlBuilder nt004 = new XmlBuilder(Ambiente.PRODUCAO_RESTRITA, Leiaute.NT_004);
    DpsCamposFiscaisTest.validarContraXsd(nt009.build(dps), "schemas/nt009/DPS_v1.01.xsd");
    DpsCamposFiscaisTest.validarContraXsd(nt004.build(dps), "schemas/nt004/DPS_v1.01.xsd");
  }

  private static Dps.DpsBuilder dps(Valores valores) {
    return Dps.builder()
        .numero("1")
        .serie("1")
        .codigoMunicipio("2304285")
        .prestador(Prestador.builder().cnpj("22513364000108").inscricaoMunicipal("102016627").codigoMunicipio("2304285").build())
        .tomador(Tomador.builder().cnpj("00819201029288").nome("Lojas Avenida S.A").build())
        .servico(Servico.builder().codigoServico("070202").descricao("Obra de construção civil").codigoNbs("101012200").build())
        .valores(valores);
  }

  private static List<String> filhos(Element el) {
    List<String> nomes = new ArrayList<>();
    for (Node n = el.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (n.getNodeType() == Node.ELEMENT_NODE) nomes.add(n.getLocalName());
    }
    return nomes;
  }

  private static String texto(Document doc, String tag) {
    var nodes = doc.getElementsByTagNameNS(NS, tag);
    return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
  }
}
