package com.coffeetecnologia.nfse.auth;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/** Certificados gerados em memória para os testes de mTLS (nada sai da máquina). */
final class TestCertificates {

  static final char[] SENHA = "senha-teste".toCharArray();
  private static final AtomicLong SERIAL = new AtomicLong(System.currentTimeMillis());

  static {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  record Issued(X509Certificate cert, PrivateKey key) {
  }

  private TestCertificates() {
  }

  static KeyPair keyPair() throws Exception {
    KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
    g.initialize(2048);
    return g.generateKeyPair();
  }

  static Issued selfSignedCa(String dn) throws Exception {
    KeyPair kp = keyPair();
    return new Issued(sign(dn, kp, dn, kp.getPrivate(), true), kp.getPrivate());
  }

  static Issued issue(String dn, Issued issuer) throws Exception {
    KeyPair kp = keyPair();
    return new Issued(sign(dn, kp, issuer.cert().getSubjectX500Principal().getName(), issuer.key(), false), kp.getPrivate());
  }

  private static X509Certificate sign(String subject, KeyPair subjectKeys, String issuerDn, PrivateKey issuerKey,
                                      boolean ca) throws Exception {
    Date from = new Date(System.currentTimeMillis() - 60_000);
    Date to = new Date(System.currentTimeMillis() + 86_400_000L);
    JcaX509v3CertificateBuilder b = new JcaX509v3CertificateBuilder(new X500Name(issuerDn),
        BigInteger.valueOf(SERIAL.incrementAndGet()), from, to, new X500Name(subject), subjectKeys.getPublic());
    b.addExtension(Extension.basicConstraints, true, new BasicConstraints(ca));
    return new JcaX509CertificateConverter().getCertificate(
        b.build(new JcaContentSignerBuilder("SHA256withRSA").build(issuerKey)));
  }

  /** PKCS12 (BouncyCastle, como a java-nfse) só com o certificado da empresa — sem a cadeia. */
  static KeyStore pfxOnlyLeaf(String alias, Issued leaf) throws Exception {
    KeyStore ks = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
    ks.load(null, null);
    ks.setKeyEntry(alias, leaf.key(), SENHA, new Certificate[] {leaf.cert()});
    return ks;
  }
}
