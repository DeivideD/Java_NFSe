package com.coffeetecnologia.nfse;

import com.coffeetecnologia.nfse.api.DistribuicaoApiClient;
import com.coffeetecnologia.nfse.api.NfseApiClient;
import com.coffeetecnologia.nfse.api.response.DfeResponse;
import com.coffeetecnologia.nfse.auth.CertificadoDigital;
import com.coffeetecnologia.nfse.config.Ambiente;
import com.coffeetecnologia.nfse.config.NfseConfig;
import com.coffeetecnologia.nfse.exception.NfseException;
import com.coffeetecnologia.nfse.model.dps.Dps;
import com.coffeetecnologia.nfse.model.nfse.Nfse;
import com.coffeetecnologia.nfse.model.nfse.SituacaoNfse;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.time.Duration;

/**
 * Ponto de entrada principal da biblioteca Java-NFS-e.
 *
 * Autenticação via mTLS — o certificado digital É a credencial.
 *
 * Exemplo:
 * <pre>{@code
 * NfseClient client = NfseClient.builder()
 *     .certificado(CertificadoDigital.fromPfx("/cert.pfx", "senha"))
 *     .ambiente(Ambiente.PRODUCAO_RESTRITA)
 *     .build();
 *
 * // Emitir
 * Nfse nfse = client.emitir(dps);
 *
 * // Sincronizar notas recebidas
 * DfeResponse docs = client.buscarLote(1L);
 * docs.getLoteDFe().forEach(doc -> System.out.println(doc.getChaveAcesso()));
 * }</pre>
 */
public class NfseClient {

  private final NfseConfig config;
  private final NfseApiClient apiClient;
  private final DistribuicaoApiClient distribuicaoClient;

  private NfseClient(NfseConfig config) {
    this.config = config;
    this.config.validar();

    HttpClient httpClient = criarHttpClient(config);
    this.apiClient = new NfseApiClient(config, httpClient);
    this.distribuicaoClient = new DistribuicaoApiClient(config, httpClient);
  }

  // ========================
  // Emissão
  // ========================

  /** Emite uma NFS-e a partir de um DPS. POST /SefinNacional/nfse */
  public Nfse emitir(Dps dps) {
    return apiClient.emitir(dps);
  }

  // ========================
  // Consulta NFS-e
  // ========================

  /** Consulta uma NFS-e pela chave de acesso. GET /SefinNacional/nfse/{chaveAcesso} */
  public Nfse consultar(String chaveAcesso) {
    return apiClient.consultar(chaveAcesso);
  }

  /** Consulta chave de acesso da NFS-e pelo id do DPS. GET /SefinNacional/dps/{id} */
  public String consultarChavePorDps(String idDps) {
    return apiClient.consultarChavePorDps(idDps);
  }

  /** Cancela uma NFS-e. POST /SefinNacional/nfse/{chaveAcesso}/eventos */
  public SituacaoNfse cancelar(String chaveAcesso, String motivo) {
    return apiClient.cancelar(chaveAcesso, motivo);
  }

  // ========================
  // Distribuição / Sincronização
  // ========================

  /**
   * Busca um único DF-e pelo NSU.
   * GET /contribuintes/DFe/{NSU}?lote=false
   */
  public DfeResponse buscarDfe(long nsu) {
    return distribuicaoClient.buscarPorNsu(nsu);
  }

  /**
   * Busca um lote de DF-e a partir de um NSU (até 50 documentos).
   * Ideal para sincronização incremental.
   * GET /contribuintes/DFe/{NSU}?lote=true
   */
  public DfeResponse buscarLote(long nsuInicial) {
    return distribuicaoClient.buscarLote(nsuInicial);
  }

  /**
   * Busca lote de DF-e filtrando por CNPJ.
   * GET /contribuintes/DFe/{NSU}?cnpjConsulta={cnpj}&lote=true
   */
  public DfeResponse buscarLotePorCnpj(long nsuInicial, String cnpj) {
    return distribuicaoClient.buscarLotePorCnpj(nsuInicial, cnpj);
  }

  /**
   * Busca eventos de uma NFS-e (cancelamentos, manifestações etc).
   * GET /contribuintes/NFSe/{chaveAcesso}/Eventos
   */
  public DfeResponse buscarEventos(String chaveAcesso) {
    return distribuicaoClient.buscarEventos(chaveAcesso);
  }

  /**
   * Descomprime o XML de um documento retornado pela API de distribuição.
   */
  public String descomprimirXml(String arquivoXmlGZipB64) {
    return distribuicaoClient.descomprimirXml(arquivoXmlGZipB64);
  }

  // ========================
  // Builder
  // ========================

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final NfseConfig.NfseConfigBuilder configBuilder = NfseConfig.builder();

    public Builder certificado(CertificadoDigital certificado) {
      configBuilder.certificado(certificado);
      return this;
    }

    public Builder ambiente(Ambiente ambiente) {
      configBuilder.ambiente(ambiente);
      return this;
    }

    public Builder timeout(Duration timeout) {
      configBuilder.timeout(timeout);
      return this;
    }

    public Builder validarXml(boolean validar) {
      configBuilder.validarXml(validar);
      return this;
    }

    public NfseClient build() {
      return new NfseClient(configBuilder.build());
    }
  }

  // ========================
  // mTLS
  // ========================

  private HttpClient criarHttpClient(NfseConfig config) {
    try {
      KeyStore keyStore = config.getCertificado().getKeyStore();
      char[] senha = config.getCertificado().getSenha();

      KeyManagerFactory kmf = KeyManagerFactory.getInstance(
          KeyManagerFactory.getDefaultAlgorithm()
      );
      kmf.init(keyStore, senha);

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(kmf.getKeyManagers(), null, null);

      return HttpClient.newBuilder()
          .sslContext(sslContext)
          .connectTimeout(config.getTimeout())
          .version(HttpClient.Version.HTTP_1_1)
          .build();

    } catch (Exception e) {
      throw new NfseException("Erro ao configurar cliente HTTP com mTLS.", e);
    }
  }
}