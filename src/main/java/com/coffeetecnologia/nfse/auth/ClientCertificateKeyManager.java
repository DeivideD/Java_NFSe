package com.coffeetecnologia.nfse.auth;

import com.coffeetecnologia.nfse.exception.NfseException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

/**
 * KeyManager de cliente mTLS que sempre apresenta o certificado da empresa, ignorando a lista de ACs
 * que o servidor manda no CertificateRequest.
 *
 * <p>Por quê: o KeyManager padrão do JDK (SunX509) só oferece um alias se algum certificado da cadeia
 * tiver como EMISSOR uma das ACs da lista do servidor. O ADN lista só as raízes ICP-Brasil
 * (v5/v10/v11/v12); um PFX que traz apenas o certificado da empresa (emitido por uma AC intermediária,
 * ex.: "AC SAFEWEB RFB v5") não casa com nenhuma, o Java faz o handshake sem certificado e o ADN
 * responde handshake_failure — embora aceite esse mesmo certificado sozinho (confirmado com curl).
 *
 * <p>Só a escolha do alias de cliente muda; chave, cadeia e os métodos de servidor vêm do KeyManager
 * original.
 */
public final class ClientCertificateKeyManager extends X509ExtendedKeyManager {

  /**
   * SunX509 explícito: a delegação usa o alias do próprio KeyStore, e é esse o formato de alias do
   * SunX509 (o PKIX/NewSunX509 usa aliases internos próprios, e getPrivateKey(alias) devolveria null).
   */
  static final String KEY_MANAGER_ALGORITHM = "SunX509";

  private final X509KeyManager delegate;
  private final String alias;

  ClientCertificateKeyManager(X509KeyManager delegate, String alias) {
    if (delegate == null || alias == null) {
      throw new IllegalArgumentException("delegate e alias são obrigatórios");
    }
    this.delegate = delegate;
    this.alias = alias;
  }

  /**
   * Monta o KeyManager a partir do KeyStore do certificado: inicializa o KeyManagerFactory com ele e
   * fixa o alias da entrada que tem a chave privada.
   */
  public static ClientCertificateKeyManager of(KeyStore keyStore, char[] senha) {
    try {
      KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_ALGORITHM);
      kmf.init(keyStore, senha);
      X509KeyManager delegate = null;
      for (KeyManager km : kmf.getKeyManagers()) {
        if (km instanceof X509KeyManager x509) {
          delegate = x509;
          break;
        }
      }
      if (delegate == null) {
        throw new NfseException("KeyManagerFactory não devolveu um X509KeyManager.");
      }
      return new ClientCertificateKeyManager(delegate, keyEntryAlias(keyStore));
    } catch (NfseException e) {
      throw e;
    } catch (Exception e) {
      throw new NfseException("Erro ao configurar o certificado de cliente para mTLS.", e);
    }
  }

  /** Alias da entrada com chave privada (o primeiro alias do PFX pode ser só um certificado). */
  static String keyEntryAlias(KeyStore keyStore) throws KeyStoreException {
    for (String a : Collections.list(keyStore.aliases())) {
      if (keyStore.isKeyEntry(a)) return a;
    }
    throw new NfseException("O certificado digital não contém chave privada.");
  }

  public String getAlias() {
    return alias;
  }

  // ---- cliente: sempre o certificado da empresa ----

  @Override
  public String[] getClientAliases(String keyType, Principal[] issuers) {
    return new String[] {alias};
  }

  @Override
  public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
    return alias;
  }

  @Override
  public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
    return alias;
  }

  // ---- chave, cadeia e servidor: KeyManager original ----

  @Override
  public X509Certificate[] getCertificateChain(String alias) {
    return delegate.getCertificateChain(alias);
  }

  @Override
  public PrivateKey getPrivateKey(String alias) {
    return delegate.getPrivateKey(alias);
  }

  @Override
  public String[] getServerAliases(String keyType, Principal[] issuers) {
    return delegate.getServerAliases(keyType, issuers);
  }

  @Override
  public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
    return delegate.chooseServerAlias(keyType, issuers, socket);
  }

  @Override
  public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
    return delegate instanceof X509ExtendedKeyManager ext
        ? ext.chooseEngineServerAlias(keyType, issuers, engine)
        : delegate.chooseServerAlias(keyType, issuers, null);
  }
}
