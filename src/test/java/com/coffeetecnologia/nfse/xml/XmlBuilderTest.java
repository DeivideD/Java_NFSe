package com.coffeetecnologia.nfse.xml;

import com.coffeetecnologia.nfse.model.dps.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("XmlBuilder - Geração de XML do DPS")
class XmlBuilderTest {

  private XmlBuilder xmlBuilder;
  private Dps dpsValido;

  @BeforeEach
  void setUp() {
    xmlBuilder = new XmlBuilder();
    dpsValido = criarDpsValido();
  }

  @Test
  @DisplayName("Deve gerar XML válido para DPS completo")
  void deveGerarXmlValido() {
    String xml = xmlBuilder.build(dpsValido);

    assertNotNull(xml);
    assertFalse(xml.isBlank());
    assertTrue(xml.contains("<DPS"), "XML deve conter elemento DPS");
    assertTrue(xml.contains("xmlns"), "XML deve conter namespace");
  }

  @Test
  @DisplayName("Deve incluir o CNPJ do prestador no XML")
  void deveIncluirCnpjPrestador() {
    String xml = xmlBuilder.build(dpsValido);

    assertTrue(xml.contains("00000000000191"), "XML deve conter o CNPJ do prestador");
  }

  @Test
  @DisplayName("Deve incluir a descrição do serviço no XML")
  void deveIncluirDescricaoServico() {
    String xml = xmlBuilder.build(dpsValido);

    assertTrue(xml.contains("Desenvolvimento de software"), "XML deve conter a descrição do serviço");
  }

  @Test
  @DisplayName("Deve incluir o valor do serviço formatado corretamente")
  void deveFormatarValorServico() {
    String xml = xmlBuilder.build(dpsValido);

    assertTrue(xml.contains("1500.00"), "XML deve conter o valor do serviço formatado");
  }

  @Test
  @DisplayName("Deve gerar Document DOM corretamente")
  void deveGerarDocument() {
    Document doc = xmlBuilder.buildDocument(dpsValido);

    assertNotNull(doc);
    assertNotNull(doc.getDocumentElement());
    assertEquals("DPS", doc.getDocumentElement().getLocalName());
  }

  @Test
  @DisplayName("Deve gerar Id no elemento raiz para assinatura XMLDSig")
  void deveGerarIdNoElementoRaiz() {
    Document doc = xmlBuilder.buildDocument(dpsValido);

    String id = doc.getDocumentElement().getAttribute("Id");
    assertNotNull(id, "Elemento raiz deve ter atributo Id");
    assertFalse(id.isBlank(), "Atributo Id não deve estar vazio");
  }

  @Test
  @DisplayName("Deve calcular base de cálculo do ISS automaticamente")
  void deveCalcularBaseCalculo() {
    String xml = xmlBuilder.build(dpsValido);

    // Valor = 1500.00, sem deduções → base = 1500.00
    assertTrue(xml.contains("1500.00"), "Base de cálculo deve ser igual ao valor do serviço sem deduções");
  }

  @Test
  @DisplayName("Deve calcular ISS com base na alíquota")
  void deveCalcularIss() {
    String xml = xmlBuilder.build(dpsValido);

    // 5% de 1500.00 = 75.00
    assertTrue(xml.contains("75.00"), "XML deve conter o valor do ISS calculado (75.00)");
  }

  @Test
  @DisplayName("Deve omitir elementos opcionais nulos")
  void deveOmitirElementosNulos() {
    Dps dpsSemEmail = Dps.builder()
        .numero("1")
        .prestador(Prestador.comCnpj("00000000000191"))
        .tomador(Tomador.comCpf("00000000000"))
        .servico(Servico.builder()
            .codigoServico("0107")
            .descricao("Serviço teste")
            .build())
        .valores(Valores.builder()
            .valorServico(new BigDecimal("100.00"))
            .build())
        .build();

    String xml = xmlBuilder.build(dpsSemEmail);

    assertFalse(xml.contains("<email>"), "XML não deve conter tag email quando não informado");
  }

  // ========================
  // Fixtures
  // ========================

  private Dps criarDpsValido() {
    return Dps.builder()
        .numero("1")
        .serie("1")
        .prestador(Prestador.builder()
            .cnpj("00000000000191")
            .inscricaoMunicipal("12345")
            .codigoMunicipio("2304400") // Fortaleza-CE
            .build())
        .tomador(Tomador.builder()
            .cpf("00000000000")
            .nome("João Silva")
            .email("joao@email.com")
            .build())
        .servico(Servico.builder()
            .codigoServico("0107")
            .descricao("Desenvolvimento de software")
            .cnae("6201500")
            .build())
        .valores(Valores.builder()
            .valorServico(new BigDecimal("1500.00"))
            .aliquotaIss(new BigDecimal("0.05"))
            .build())
        .codigoMunicipio("2304400")
        .build();
  }
}