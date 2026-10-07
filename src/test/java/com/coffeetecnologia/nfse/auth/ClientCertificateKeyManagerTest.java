package com.coffeetecnologia.nfse.auth;

import com.coffeetecnologia.nfse.exception.NfseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cenário do ADN: o servidor pede certificado listando só as raízes ICP-Brasil, e o PFX da empresa traz só
 * o certificado dela, emitido por uma AC intermediária fora dessa lista.
 */
class ClientCertificateKeyManagerTest {

  private static final String ALIAS = "empresa";
  private static TestCertificates.Issued intermediaria;
  private static TestCertificates.Issued empresa;
  private static TestCertificates.Issued raizIcpFalsa;
  private static KeyStore pfx;

  @BeforeAll
  static void certificados() throws Exception {
    TestCertificates.Issued raizDaEmpresa = TestCertificates.selfSignedCa("CN=Raiz Teste, O=Teste, C=BR");
    intermediaria = TestCertificates.issue("CN=AC SAFEWEB RFB v5 TESTE, O=ICP-Brasil, C=BR", raizDaEmpresa);
    empresa = TestCertificates.issue("CN=CODE E COFFEE TESTE:42742743000187, O=ICP-Brasil, C=BR", intermediaria);
    raizIcpFalsa = TestCertificates.selfSignedCa(
        "CN=Autoridade Certificadora Raiz Brasileira v5, OU=Instituto Nacional de Tecnologia da Informacao - ITI, O=ICP-Brasil, C=BR");
    pfx = TestCertificates.pfxOnlyLeaf(ALIAS, empresa);
  }

  /** Lista do CertificateRequest do ADN: só raízes ICP-Brasil, sem a AC que emitiu o certificado. */
  private static Principal[] raizesIcpBrasil() {
    return new Principal[] {
        new X500Principal("CN=Autoridade Certificadora Raiz Brasileira v5, OU=Instituto Nacional de Tecnologia da Informacao - ITI, O=ICP-Brasil, C=BR"),
        new X500Principal("CN=Autoridade Certificadora Raiz Brasileira v10, OU=Instituto Nacional de Tecnologia da Informacao - ITI, O=ICP-Brasil, C=BR")};
  }

  // ---- unitários ----

  @Test
  void defaultKeyManagerFindsNoAliasForTheIcpRootList() throws Exception {
    // reproduz o bug: o SunX509 filtra pelo emissor e não acha nada
    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    kmf.init(pfx, TestCertificates.SENHA);
    X509KeyManager padrao = (X509KeyManager) kmf.getKeyManagers()[0];

    assertNull(padrao.chooseClientAlias(new String[] {"RSA"}, raizesIcpBrasil(), null));
  }

  @Test
  void alwaysChoosesTheCompanyAliasIgnoringTheServerIssuers() {
    ClientCertificateKeyManager km = ClientCertificateKeyManager.of(pfx, TestCertificates.SENHA);

    assertEquals(ALIAS, km.chooseEngineClientAlias(new String[] {"RSA"}, raizesIcpBrasil(), null));
    assertEquals(ALIAS, km.chooseClientAlias(new String[] {"RSA"}, raizesIcpBrasil(), null));
    assertArrayEquals(new String[] {ALIAS}, km.getClientAliases("RSA", raizesIcpBrasil()));
    // chave e cadeia continuam vindo do KeyManager original
    assertNotNull(km.getPrivateKey(ALIAS));
    assertArrayEquals(new X509Certificate[] {empresa.cert()}, km.getCertificateChain(ALIAS));
  }

  @Test
  void usesTheKeyEntryEvenWhenTheFirstAliasIsOnlyACertificate() throws Exception {
    KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
    ks.load(null, null);
    ks.setCertificateEntry("a-intermediaria", intermediaria.cert());
    ks.setKeyEntry("z-empresa", empresa.key(), TestCertificates.SENHA, new java.security.cert.Certificate[] {empresa.cert()});

    assertEquals("z-empresa", ClientCertificateKeyManager.of(ks, TestCertificates.SENHA).getAlias());
  }

  @Test
  void pfxWithoutPrivateKeyIsAClearError() throws Exception {
    KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
    ks.load(null, null);
    ks.setCertificateEntry("so-certificado", empresa.cert());

    NfseException e = assertThrows(NfseException.class, () -> ClientCertificateKeyManager.of(ks, TestCertificates.SENHA));
    assertTrue(e.getMessage().contains("não contém chave privada"));
  }

  // ---- handshake TLS real em localhost ----

  /** Servidor que exige certificado e anuncia só a "raiz ICP" falsa; valida o cliente pela AC real. */
  private static SSLServerSocket servidor() throws Exception {
    TestCertificates.Issued servidor = TestCertificates.selfSignedCa("CN=localhost");
    KeyStore serverKs = KeyStore.getInstance("PKCS12");
    serverKs.load(null, null);
    serverKs.setKeyEntry("srv", servidor.key(), TestCertificates.SENHA, new java.security.cert.Certificate[] {servidor.cert()});
    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    kmf.init(serverKs, TestCertificates.SENHA);

    X509TrustManager comoAdn = new X509TrustManager() {
      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
          chain[0].verify(intermediaria.cert().getPublicKey()); // aceita o certificado sozinho, como o ADN
        } catch (Exception e) {
          throw new CertificateException(e);
        }
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType) {
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[] {raizIcpFalsa.cert()}; // vira a lista do CertificateRequest
      }
    };

    SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(kmf.getKeyManagers(), new TrustManager[] {comoAdn}, null);
    SSLServerSocket server = (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket(0, 1, InetAddress.getLoopbackAddress());
    server.setNeedClientAuth(true);
    return server;
  }

  /** Faz o handshake com os KeyManagers dados; devolve o certificado que o servidor recebeu. */
  private static X509Certificate handshake(KeyManager[] keyManagers) throws Exception {
    try (SSLServerSocket server = servidor()) {
      CompletableFuture<X509Certificate> recebido = CompletableFuture.supplyAsync(() -> {
        try (SSLSocket s = (SSLSocket) server.accept()) {
          s.startHandshake();
          return (X509Certificate) s.getSession().getPeerCertificates()[0];
        } catch (Exception e) {
          return null;
        }
      });

      TrustManager[] confiaNoServidorDeTeste = {new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0];
        }
      }};
      SSLContext cliente = SSLContext.getInstance("TLS");
      cliente.init(keyManagers, confiaNoServidorDeTeste, null);
      try (SSLSocket s = (SSLSocket) cliente.getSocketFactory().createSocket(InetAddress.getLoopbackAddress(), server.getLocalPort())) {
        s.startHandshake();
        s.getInputStream().available(); // no TLS 1.3 a recusa do servidor chega depois do handshake do cliente
        s.getOutputStream().write(1);
        s.getOutputStream().flush();
        s.getInputStream().read();
      }
      return recebido.get(10, TimeUnit.SECONDS);
    }
  }

  @Test
  void handshakeWithTheDefaultKeyManagerSendsNoCertificate() throws Exception {
    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    kmf.init(pfx, TestCertificates.SENHA);

    assertThrows(javax.net.ssl.SSLException.class, () -> handshake(kmf.getKeyManagers())); // o handshake_failure de produção
  }

  @Test
  void handshakeWithClientCertificateKeyManagerPresentsTheCompanyCertificate() throws Exception {
    X509Certificate recebido = handshake(new KeyManager[] {ClientCertificateKeyManager.of(pfx, TestCertificates.SENHA)});

    assertEquals(empresa.cert(), recebido);
  }
}
