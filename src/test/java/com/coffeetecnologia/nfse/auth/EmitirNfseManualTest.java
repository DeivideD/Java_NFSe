package com.coffeetecnologia.nfse.auth;

import com.coffeetecnologia.nfse.NfseClient;
import com.coffeetecnologia.nfse.config.Ambiente;
import com.coffeetecnologia.nfse.model.dps.*;
import com.coffeetecnologia.nfse.model.nfse.Nfse;

import java.math.BigDecimal;

public class EmitirNfseManualTest {

  public static void main(String[] args) {

    String caminho = "CAMINHO_SEU_CERTIFICADO";
    String senha   = "SEUA_SENHA";

    System.out.println("=== Emitindo NFS-e em Homologação ===\n");

    try {
      // 1. Monta o cliente
      NfseClient client = NfseClient.builder()
          .certificado(CertificadoDigital.fromPfx(caminho, senha))
          .ambiente(Ambiente.PRODUCAO_RESTRITA)
          .validarXml(false)
          .build();
      System.out.println("✅ Cliente criado.");

      // 2. Monta o DPS com seus dados reais
      Dps dps = Dps.builder()
          .numero("1")
          .serie("1")
          .prestador(Prestador.builder()
              .cnpj("44372492000111")     // ← VJ PRE MOLDADOS
              .codigoMunicipio("2304400") // ← Fortaleza ✅
              .build())
          .tomador(Tomador.builder()
              .cnpj("22513364000108")     // ← SOWAL como tomadora
              .nome("SOWAL COMERCIO E SERVICOS LTDA")
              .build())
          .servico(Servico.builder()
              .codigoServico("010601")    // ← código real de construção
              .descricao("Consultoria em tecnologia da informação")
              .build())
          .valores(Valores.builder()
              .valorServico(new BigDecimal("100.00"))
              .aliquotaIss(new BigDecimal("0.0271"))
              .build())
          .codigoMunicipio("2304400")
          .build();
      System.out.println("✅ DPS montado.");

      // 3. Emite
      System.out.println("⏳ Enviando para a API de homologação...");

//      System.out.println("⏳ ####################.");
//      consultarServicosFortaleza(client);
//      System.out.println("⏳ ⏳ ####################.");
//
      System.out.println("⏳ +++++++++.");
      consultarParametrosFortaleza(caminho, senha);
      System.out.println("⏳ +++++++++.");



      Nfse nfse = client.emitir(dps);

      // 4. Resultado
      System.out.println("\n✅ NFS-e emitida com sucesso!");
      System.out.println("Número   : " + nfse.getNumero());
      System.out.println("Chave    : " + nfse.getChaveAcesso());
      System.out.println("Situação : " + nfse.getSituacao());
      System.out.println("DANFSE   : " + nfse.getUrlDanfse());

    } catch (Exception e) {
      System.out.println("❌ Erro: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void consultarServicosFortaleza(NfseClient client) throws Exception {
    // Acessa direto via HttpClient para ver os códigos válidos
    java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
    java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
        .uri(java.net.URI.create(
            "https://adn.producaorestrita.nfse.gov.br/parametrizacao/parametros-municipais/2304400/convenio"
        ))
        .version(java.net.http.HttpClient.Version.HTTP_1_1)
        .GET()
        .build();

    java.net.http.HttpResponse<String> resp = http.send(req,
        java.net.http.HttpResponse.BodyHandlers.ofString());

    System.out.println("Status: " + resp.statusCode());
    System.out.println("Serviços Fortaleza: " + resp.body());
  }

  private static void consultarParametrosFortaleza(String caminho, String senha) throws Exception {
    CertificadoDigital cert = CertificadoDigital.fromPfx(caminho, senha);

    // Monta HttpClient com mTLS igual ao NfseClient
    javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory
        .getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(cert.getKeyStore(), cert.getSenha());

    javax.net.ssl.SSLContext sslCtx = javax.net.ssl.SSLContext.getInstance("TLS");
    sslCtx.init(kmf.getKeyManagers(), null, null);

    java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder()
        .sslContext(sslCtx)
        .version(java.net.http.HttpClient.Version.HTTP_1_1)
        .build();

    java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
        .uri(java.net.URI.create(
            "https://sefin.producaorestrita.nfse.gov.br/SefinNacional/parametros-municipais/2304400/convenio"
        ))
        .header("Accept", "application/json")
        .GET()
        .build();

    java.net.http.HttpResponse<String> resp = http.send(req,
        java.net.http.HttpResponse.BodyHandlers.ofString());

    System.out.println("Status: " + resp.statusCode());
    System.out.println("Parâmetros Fortaleza: " + resp.body());
  }
}