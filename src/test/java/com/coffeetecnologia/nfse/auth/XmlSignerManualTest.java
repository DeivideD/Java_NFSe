package com.coffeetecnologia.nfse.auth;

import com.coffeetecnologia.nfse.model.dps.*;
import com.coffeetecnologia.nfse.xml.XmlBuilder;
import com.coffeetecnologia.nfse.xml.XmlSigner;
import org.w3c.dom.Document;

import java.math.BigDecimal;

public class XmlSignerManualTest {

  public static void main(String[] args) {

    String caminho = "CAMINHO_SEU_CERTIFICADO";
    String senha   = "SEUA_SENHA";

    System.out.println("=== Gerando e assinando XML do DPS ===\n");

    try {
      // 1. Carrega o certificado
      CertificadoDigital cert = CertificadoDigital.fromPfx(caminho, senha);
      System.out.println("✅ Certificado carregado.");

      // 2. Monta o DPS com seus dados reais
      Dps dps = Dps.builder()
          .numero("1")
          .serie("1")
          .prestador(Prestador.builder()
              .cnpj("22513364000108")
              .inscricaoMunicipal("64550370")
              .codigoMunicipio("2304400") // Fortaleza-CE
              .build())
          .tomador(Tomador.builder()
              .cpf("00000000000") // CPF fictício para teste
              .nome("Tomador Teste")
              .build())
          .servico(Servico.builder()
              .codigoServico("0107")
              .descricao("Desenvolvimento de software")
              .cnae("6201500")
              .build())
          .valores(Valores.builder()
              .valorServico(new BigDecimal("100.00"))
              .aliquotaIss(new BigDecimal("0.05"))
              .build())
          .codigoMunicipio("2304400")
          .build();

      System.out.println("✅ DPS montado.");

      // 3. Gera o XML
      XmlBuilder builder = new XmlBuilder();
      Document doc = builder.buildDocument(dps);
      System.out.println("✅ XML gerado.");

      // 4. Assina o XML
      XmlSigner signer = new XmlSigner();
      String xmlAssinado = signer.assinar(doc, cert);
      System.out.println("✅ XML assinado com sucesso!\n");

      // 5. Imprime o XML assinado
      System.out.println("=== XML ASSINADO ===");
      System.out.println(xmlAssinado);

    } catch (Exception e) {
      System.out.println("❌ Erro: " + e.getMessage());
      e.printStackTrace();
    }
  }
}