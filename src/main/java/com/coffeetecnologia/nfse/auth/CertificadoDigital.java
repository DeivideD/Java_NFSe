package com.coffeetecnologia.nfse.auth;

import com.coffeetecnologia.nfse.exception.CertificadoException;
import lombok.Getter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Enumeration;

/**
 * Abstração para certificados digitais A1 (arquivo .pfx) e A3 (token/smartcard).
 *
 * <p>Exemplo de uso com A1:
 * <pre>{@code
 * CertificadoDigital cert = CertificadoDigital.fromPfx("/path/cert.pfx", "senha");
 * }</pre>
 *
 * <p>Exemplo de uso com A3 (PKCS#11):
 * <pre>{@code
 * CertificadoDigital cert = CertificadoDigital.fromA3("/path/driver.so", "pin");
 * }</pre>
 */
@Getter
public class CertificadoDigital {

  static {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  private final KeyStore keyStore;
  private final String alias;
  private final char[] senha;

  private CertificadoDigital(KeyStore keyStore, String alias, char[] senha) {
    this.keyStore = keyStore;
    this.alias = alias;
    this.senha = senha;
  }

  /**
   * Carrega um certificado A1 a partir de um arquivo .pfx/.p12.
   *
   * @param caminho caminho absoluto para o arquivo .pfx
   * @param senha   senha do certificado
   * @return instância configurada de {@link CertificadoDigital}
   */
  public static CertificadoDigital fromPfx(String caminho, String senha) {
    try (InputStream in = new FileInputStream(caminho)) {
      return fromStream(in, senha);
    } catch (IOException e) {
      throw new CertificadoException("Erro ao abrir arquivo de certificado: " + caminho, e);
    }
  }

  /**
   * Carrega um certificado A1 a partir de um InputStream.
   * Útil para carregar de classpath, banco de dados, etc.
   *
   * @param inputStream stream com o conteúdo .pfx
   * @param senha       senha do certificado
   * @return instância configurada de {@link CertificadoDigital}
   */
  public static CertificadoDigital fromStream(InputStream inputStream, String senha) {
    try {
      KeyStore ks = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
      ks.load(inputStream, senha.toCharArray());
      String alias = resolverAlias(ks);
      return new CertificadoDigital(ks, alias, senha.toCharArray());
    } catch (Exception e) {
      throw new CertificadoException("Erro ao carregar certificado A1.", e);
    }
  }

  /**
   * Carrega um certificado A3 via PKCS#11 (token ou smartcard).
   *
   * @param driverPath caminho para a biblioteca .so/.dll do driver do token
   * @param pin        PIN do token
   * @return instância configurada de {@link CertificadoDigital}
   */
  public static CertificadoDigital fromA3(String driverPath, String pin) {
    try {
      String config = String.format("--\nname=NfseA3\nlibrary=%s\n", driverPath);
      Provider pkcs11Provider = Security.getProvider("SunPKCS11");
      Provider configured = pkcs11Provider.configure(config);
      Security.addProvider(configured);

      KeyStore ks = KeyStore.getInstance("PKCS11", configured);
      ks.load(null, pin.toCharArray());
      String alias = resolverAlias(ks);
      return new CertificadoDigital(ks, alias, pin.toCharArray());
    } catch (Exception e) {
      throw new CertificadoException("Erro ao carregar certificado A3 via PKCS#11.", e);
    }
  }

  /**
   * Retorna a chave privada do certificado para uso em assinaturas.
   */
  public PrivateKey getPrivateKey() {
    try {
      return (PrivateKey) keyStore.getKey(alias, senha);
    } catch (Exception e) {
      throw new CertificadoException("Erro ao obter chave privada do certificado.", e);
    }
  }

  /**
   * Retorna o certificado X.509 público.
   */
  public X509Certificate getCertificado() {
    try {
      return (X509Certificate) keyStore.getCertificate(alias);
    } catch (Exception e) {
      throw new CertificadoException("Erro ao obter certificado X.509.", e);
    }
  }

  /**
   * Verifica se o certificado está vencido.
   */
  public boolean isVencido() {
    try {
      getCertificado().checkValidity();
      return false;
    } catch (Exception e) {
      return true;
    }
  }

  /**
   * Retorna a data de validade do certificado.
   */
  public LocalDateTime getValidade() {
    return getCertificado()
        .getNotAfter()
        .toInstant()
        .atZone(ZoneId.of("America/Sao_Paulo"))
        .toLocalDateTime();
  }

  /**
   * Resolve o primeiro alias disponível no KeyStore.
   */
  private static String resolverAlias(KeyStore keyStore) throws KeyStoreException {
    Enumeration<String> aliases = keyStore.aliases();
    if (aliases.hasMoreElements()) {
      return aliases.nextElement();
    }
    throw new CertificadoException("Nenhum alias encontrado no certificado.");
  }
}