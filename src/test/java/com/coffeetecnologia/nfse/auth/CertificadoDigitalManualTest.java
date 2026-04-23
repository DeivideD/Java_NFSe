package com.coffeetecnologia.nfse.auth;

import java.security.cert.X509Certificate;

/**
 * Teste manual de leitura do certificado digital.
 * Execute o main diretamente no IntelliJ (botão play na linha do main).
 * NÃO commitar este arquivo — apenas para validação local.
 */
public class CertificadoDigitalManualTest {

  public static void main(String[] args) {

    // ✅ Ajuste o caminho e a senha do seu .pfx
    String caminho = "/home/deivide/projects/coffee/dependence/certificados/sowal_comercio.pfx";
//    String senha   = System.getenv("CERT_SENHA"); // ou coloque a senha direto só para teste local
    String senha = "1234";

    System.out.println("=== Lendo certificado ===");

    try {
      CertificadoDigital cert = CertificadoDigital.fromPfx(caminho, senha);
      X509Certificate x509    = cert.getCertificado();

      System.out.println("✅ Certificado lido com sucesso!");
      System.out.println();
      System.out.println("Titular : " + x509.getSubjectX500Principal().getName());
      System.out.println("Emissor : " + x509.getIssuerX500Principal().getName());
      System.out.println("Válido até : " + cert.getValidade());
      System.out.println("Vencido?   : " + cert.isVencido());
      System.out.println("Algoritmo  : " + x509.getSigAlgName());
      System.out.println("Alias      : " + cert.getAlias());

    } catch (Exception e) {
      System.out.println("❌ Erro ao ler certificado: " + e.getMessage());
      e.printStackTrace();
    }
  }
}