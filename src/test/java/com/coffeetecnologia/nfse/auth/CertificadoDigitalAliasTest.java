package com.coffeetecnologia.nfse.auth;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** O alias usado para assinar é o da chave, mesmo quando o PFX começa por um certificado da cadeia. */
class CertificadoDigitalAliasTest {

  private static byte[] pfx(KeyStore ks) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ks.store(out, TestCertificates.SENHA);
    return out.toByteArray();
  }

  @Test
  void signsWithTheKeyEntryEvenWhenTheFirstAliasIsOnlyACertificate() throws Exception {
    TestCertificates.Issued raiz = TestCertificates.selfSignedCa("CN=Raiz Teste, O=Teste, C=BR");
    TestCertificates.Issued intermediaria = TestCertificates.issue("CN=AC Intermediaria Teste, O=Teste, C=BR", raiz);
    TestCertificates.Issued empresa = TestCertificates.issue("CN=EMPRESA TESTE, O=Teste, C=BR", intermediaria);

    KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
    ks.load(null, null);
    ks.setCertificateEntry("a-intermediaria", intermediaria.cert()); // vem primeiro na ordem dos aliases
    ks.setKeyEntry("z-empresa", empresa.key(), TestCertificates.SENHA, new Certificate[] {empresa.cert(), intermediaria.cert()});

    CertificadoDigital cert = CertificadoDigital.fromStream(new ByteArrayInputStream(pfx(ks)), new String(TestCertificates.SENHA));

    assertEquals("z-empresa", cert.getAlias());
    assertNotNull(cert.getPrivateKey());
    assertEquals(empresa.cert(), cert.getCertificado());
  }

  @Test
  void singleEntryPfxKeepsWorking() throws Exception {
    TestCertificates.Issued ca = TestCertificates.selfSignedCa("CN=AC Teste, O=Teste, C=BR");
    TestCertificates.Issued empresa = TestCertificates.issue("CN=EMPRESA UNICA, O=Teste, C=BR", ca);

    CertificadoDigital cert = CertificadoDigital.fromStream(
        new ByteArrayInputStream(pfx(TestCertificates.pfxOnlyLeaf("empresa", empresa))), new String(TestCertificates.SENHA));

    assertEquals(empresa.cert(), cert.getCertificado());
    assertNotNull(cert.getPrivateKey());
  }
}
