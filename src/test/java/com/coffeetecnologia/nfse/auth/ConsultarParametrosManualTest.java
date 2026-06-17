package com.coffeetecnologia.nfse.auth;

import com.coffeetecnologia.nfse.NfseClient;
import com.coffeetecnologia.nfse.config.Ambiente;

public class ConsultarParametrosManualTest {

  public static void main(String[] args) {

    String caminho = "/home/deivide/Downloads/certificado/coffee.pfx";
    String senha   = "123456";

    System.out.println("=== Consultando parâmetros de Fortaleza (2304400) ===\n");

    try {
      NfseClient client = NfseClient.builder()
          .certificado(CertificadoDigital.fromPfx(caminho, senha))
          .ambiente(Ambiente.PRODUCAO_RESTRITA)
          .build();

      String json = client.consultarParametrosMunicipais("2304400");
      System.out.println(json);

    } catch (Exception e) {
      System.out.println("❌ Erro: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
