package com.coffeetecnologia.nfse.xml;

import com.coffeetecnologia.nfse.config.Ambiente;
import com.coffeetecnologia.nfse.config.Leiaute;
import com.coffeetecnologia.nfse.model.dps.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("XmlBuilder - leiaute da NT 009 (Produção Restrita desde 20/10/2026, Produção a partir de 01/12/2026)")
class DpsLeiauteNt009Test {

  private static final String NS = "http://www.sped.fazenda.gov.br/nfse";

  private final XmlBuilder nt009 = new XmlBuilder(Ambiente.PRODUCAO, Leiaute.NT_009);
  private final XmlBuilder nt004 = new XmlBuilder(Ambiente.PRODUCAO, Leiaute.NT_004);

  @Test
  @DisplayName("Calendário: Restrita já no novo; Produção muda em 01/12/2026")
  void leiauteVigente() {
    assertEquals(Leiaute.NT_009, Leiaute.vigente(Ambiente.PRODUCAO_RESTRITA, LocalDate.of(2026, 11, 20)));
    assertEquals(Leiaute.NT_004, Leiaute.vigente(Ambiente.PRODUCAO, LocalDate.of(2026, 11, 30)));
    assertEquals(Leiaute.NT_009, Leiaute.vigente(Ambiente.PRODUCAO, LocalDate.of(2026, 12, 1)));
  }

  @Test
  @DisplayName("finNFSe vai em infDPS, depois de tpEmit e antes de cLocEmi")
  void finNFSeEmInfDps() throws Exception {
    Document doc = nt009.buildDocument(dps().build());

    List<String> filhos = filhos(doc.getElementsByTagNameNS(NS, "infDPS").item(0));
    assertEquals(filhos.indexOf("tpEmit") + 1, filhos.indexOf("finNFSe"));
    assertEquals(filhos.indexOf("finNFSe") + 1, filhos.indexOf("cLocEmi"));
    assertEquals("0", doc.getElementsByTagNameNS(NS, "finNFSe").item(0).getTextContent());
    DpsCamposFiscaisTest.validarContraXsd(nt009.toXmlString(doc), "xsd/nt009/DPS_v1.01.xsd");
  }

  @Test
  @DisplayName("xDescServ vem depois de cNBS")
  void ordemDoCServ() throws Exception {
    Servico servico = Servico.builder().codigoServico("010700").codigoNbs("115022000").descricao("Software").build();
    Document doc = nt009.buildDocument(dps().servico(servico).build());

    assertEquals(List.of("cTribNac", "cNBS", "xDescServ"), filhos(doc.getElementsByTagNameNS(NS, "cServ").item(0)));
    DpsCamposFiscaisTest.validarContraXsd(nt009.toXmlString(doc), "xsd/nt009/DPS_v1.01.xsd");
  }

  @Test
  @DisplayName("IBSCBS: sem finNFSe no grupo e CST/cClassTrib direto em trib")
  void ibscbsNoLeiauteNovo() throws Exception {
    Document doc = nt009.buildDocument(dps().ibscbs(ibscbs()).build());

    Element ibscbs = (Element) doc.getElementsByTagNameNS(NS, "IBSCBS").item(0);
    assertEquals(0, ibscbs.getElementsByTagNameNS(NS, "finNFSe").getLength());
    assertEquals(0, ibscbs.getElementsByTagNameNS(NS, "gIBSCBS").getLength());
    Node trib = ibscbs.getElementsByTagNameNS(NS, "trib").item(0);
    assertEquals(List.of("CST", "cClassTrib"), filhos(trib));
    DpsCamposFiscaisTest.validarContraXsd(nt009.toXmlString(doc), "xsd/nt009/DPS_v1.01.xsd");
  }

  @Test
  @DisplayName("cIndOp passa a ser opcional")
  void cIndOpOpcional() throws Exception {
    Ibscbs semIndOp = Ibscbs.builder().indDest("0")
        .gIbscbs(GIbscbs.builder().cst("000").cClassTrib("000001").build()).build();

    String xml = nt009.build(dps().ibscbs(semIndOp).build());
    DpsCamposFiscaisTest.validarContraXsd(xml, "xsd/nt009/DPS_v1.01.xsd");
  }

  @Test
  @DisplayName("O mesmo DPS no leiaute da NT 004 continua válido no XSD antigo")
  void nt004Inalterado() throws Exception {
    Servico servico = Servico.builder().codigoServico("010700").codigoNbs("115022000").descricao("Software").build();
    String xml = nt004.build(dps().servico(servico).ibscbs(ibscbs()).build());

    assertFalse(xml.contains("<finNFSe>0</finNFSe><cLocEmi>"));
    DpsCamposFiscaisTest.validarContraXsd(xml, "xsd/DPS_v1.01.xsd");
  }

  private static Dps.DpsBuilder dps() {
    return Dps.builder()
        .numero("1")
        .serie("1")
        .codigoMunicipio("2304400")
        .prestador(Prestador.builder().cnpj("00000000000191").codigoMunicipio("2304400")
            .opcaoSimplesNacional(OpcaoSimplesNacional.ME_EPP).build())
        .tomador(Tomador.builder().cnpj("11222333000181").nome("Cliente Ltda").build())
        .servico(Servico.builder().codigoServico("010700").descricao("Software").build())
        .valores(Valores.builder().valorServico(new BigDecimal("100.00")).build());
  }

  private static Ibscbs ibscbs() {
    return Ibscbs.builder().cIndOp("010000").indDest("0")
        .gIbscbs(GIbscbs.builder().cst("000").cClassTrib("000001").build()).build();
  }

  private static List<String> filhos(Node pai) {
    List<String> nomes = new ArrayList<>();
    for (Node n = pai.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (n.getNodeType() == Node.ELEMENT_NODE) nomes.add(n.getLocalName());
    }
    return nomes;
  }
}
