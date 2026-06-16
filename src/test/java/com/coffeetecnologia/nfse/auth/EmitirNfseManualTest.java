package com.coffeetecnologia.nfse.auth;

import com.coffeetecnologia.nfse.NfseClient;
import com.coffeetecnologia.nfse.config.Ambiente;
import com.coffeetecnologia.nfse.model.dps.*;
import com.coffeetecnologia.nfse.model.nfse.Nfse;

import java.math.BigDecimal;

public class EmitirNfseManualTest {

  public static void main(String[] args) {

    // ← Preencha antes de rodar
    String caminho = "CAMINHO_SEU_CERTIFICADO";
    String senha   = "SEUA_SENHA";

    System.out.println("=== Emitindo NFS-e em Produção Restrita ===\n");

    try {
      NfseClient client = NfseClient.builder()
          .certificado(CertificadoDigital.fromPfx(caminho, senha))
          .ambiente(Ambiente.PRODUCAO_RESTRITA)
          .validarXml(false)
          .build();
      System.out.println("✅ Cliente criado.");

      Dps dps = Dps.builder()
          .numero("1")
          .serie("1")
          .prestador(Prestador.builder()
              .cnpj("44372492000111")
              .codigoMunicipio("2304400")
              .build())
          .tomador(Tomador.builder()
              .cnpj("22513364000108")
              .nome("SOWAL COMERCIO E SERVICOS LTDA")
              .build())
          .servico(Servico.builder()
              .codigoServico("010601")
              .descricao("Consultoria em tecnologia da informação")
              .build())
          .valores(Valores.builder()
              .valorServico(new BigDecimal("100.00"))
              .aliquotaIss(new BigDecimal("0.0271"))
              .build())
          .codigoMunicipio("2304400")
          .build();
      System.out.println("✅ DPS montado.");

      System.out.println("⏳ Enviando para a API...");
      Nfse nfse = client.emitir(dps);

      System.out.println("\n✅ NFS-e emitida com sucesso!");
      System.out.println("Chave    : " + nfse.getChaveAcesso());
      System.out.println("Situação : " + nfse.getSituacao());

    } catch (Exception e) {
      System.out.println("❌ Erro: " + e.getMessage());
      e.printStackTrace();
    }
  }
}