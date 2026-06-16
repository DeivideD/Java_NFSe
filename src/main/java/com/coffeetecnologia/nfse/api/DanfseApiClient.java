package com.coffeetecnologia.nfse.api;

import com.coffeetecnologia.nfse.config.NfseConfig;
import com.coffeetecnologia.nfse.exception.NfseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Cliente HTTP para obtenção do DANFSe (Documento Auxiliar da NFS-e) em PDF.
 *
 * Endpoint: GET /danfse/{chaveAcesso}
 */
public class DanfseApiClient {

  private static final Logger log = LoggerFactory.getLogger(DanfseApiClient.class);
  private static final String BASE_PR = "https://adn.producaorestrita.nfse.gov.br/danfse";
  private static final String BASE_PROD = "https://adn.nfse.gov.br/danfse";

  private final NfseConfig config;
  private final HttpClient httpClient;

  public DanfseApiClient(NfseConfig config, HttpClient httpClient) {
    this.config = config;
    this.httpClient = httpClient;
  }

  /**
   * Obtém o DANFSe em PDF pela chave de acesso.
   *
   * @param chaveAcesso chave de acesso da NFS-e (50 dígitos)
   * @return bytes do PDF do DANFSe
   */
  public byte[] obterDanfse(String chaveAcesso) {
    String url = getBaseUrl() + "/" + chaveAcesso;
    log.info("GET DANFSe {}", url);

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(config.getTimeout())
          .header("Accept", "application/pdf")
          .GET()
          .build();

      HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

      int status = response.statusCode();
      if (status == 200) {
        log.info("DANFSe obtido. Tamanho: {} bytes", response.body().length);
        return response.body();
      }
      if (status == 403) throw new NfseException("DANFSe — certificado inválido ou sem permissão (403).");
      if (status == 404) throw new NfseException("DANFSe — NFS-e não encontrada (404): " + chaveAcesso);
      throw new NfseException("DANFSe — HTTP " + status + " ao obter PDF.");

    } catch (NfseException e) {
      throw e;
    } catch (Exception e) {
      throw new NfseException("Erro de comunicação ao obter DANFSe: " + url, e);
    }
  }

  private String getBaseUrl() {
    return config.getAmbiente().isHomologacao() ? BASE_PR : BASE_PROD;
  }
}
