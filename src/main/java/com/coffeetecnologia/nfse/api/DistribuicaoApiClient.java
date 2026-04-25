package com.coffeetecnologia.nfse.api;

import com.coffeetecnologia.nfse.api.response.DfeResponse;
import com.coffeetecnologia.nfse.config.NfseConfig;
import com.coffeetecnologia.nfse.exception.NfseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

/**
 * Cliente para a API de Distribuição de DF-e para Contribuintes.
 *
 */
public class DistribuicaoApiClient {

  private static final Logger log = LoggerFactory.getLogger(DistribuicaoApiClient.class);

  private static final String BASE_PRODUCAO_RESTRITA =
      "https://adn.producaorestrita.nfse.gov.br/contribuintes";
  private static final String BASE_PRODUCAO =
      "https://adn.nfse.gov.br/contribuintes";

  private static final int MAX_RETRIES = 3;
  private static final long RATE_LIMIT_WAIT_MS = 10_000L; // 10s
  private static final long RETRY_WAIT_MS = 5_000L;       // 5s entre retries normais

  private final NfseConfig config;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public DistribuicaoApiClient(NfseConfig config, HttpClient httpClient) {
    this.config = config;
    this.httpClient = httpClient;
    this.objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  // ========================
  // Busca por NSU
  // ========================

  /**
   * Busca um único documento pelo NSU.
   * GET /DFe/{NSU}?lote=false
   */
  public DfeResponse buscarPorNsu(long nsu) {
    String url = getBaseUrl() + "/DFe/" + nsu + "?lote=false";
    log.info("Buscando DF-e por NSU: {}", nsu);
    return getComRetry(url);
  }

  /**
   * Busca um lote de documentos a partir de um NSU.
   * GET /DFe/{NSU}?lote=true
   */
  public DfeResponse buscarLote(long nsuInicial) {
    String url = getBaseUrl() + "/DFe/" + nsuInicial + "?lote=true";
    log.info("Buscando lote de DF-e a partir do NSU: {}", nsuInicial);
    return getComRetry(url);
  }

  /**
   * Busca lote filtrando por CNPJ.
   * GET /DFe/{NSU}?cnpjConsulta={cnpj}&lote=true
   */
  public DfeResponse buscarLotePorCnpj(long nsuInicial, String cnpj) {
    String url = getBaseUrl() + "/DFe/" + nsuInicial
        + "?cnpjConsulta=" + cnpj + "&lote=true";
    log.info("Buscando lote por CNPJ {} a partir do NSU: {}", cnpj, nsuInicial);
    return getComRetry(url);
  }

  // ========================
  // Eventos
  // ========================

  /**
   * Busca eventos de uma NFS-e.
   * GET /NFSe/{ChaveAcesso}/Eventos
   */
  public DfeResponse buscarEventos(String chaveAcesso) {
    String url = getBaseUrl() + "/NFSe/" + chaveAcesso + "/Eventos";
    log.info("Buscando eventos da NFS-e: {}", chaveAcesso);
    return getComRetry(url);
  }

  // ========================
  // Descompressão
  // ========================

  /**
   * Descomprime o campo ArquivoXml (GZip + Base64).
   */
  public String descomprimirXml(String arquivoXmlGZipB64) {
    try {
      byte[] compressed = Base64.getDecoder().decode(arquivoXmlGZipB64);
      try (GZIPInputStream gzip = new GZIPInputStream(
          new ByteArrayInputStream(compressed))) {
        return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
      }
    } catch (Exception e) {
      throw new NfseException("Erro ao descomprimir XML do DF-e.", e);
    }
  }

  // ========================
  // HTTP com Retry + Rate Limit
  // ========================

  private DfeResponse getComRetry(String url) {
    int tentativa = 0;

    while (tentativa < MAX_RETRIES) {
      tentativa++;
      try {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(config.getTimeout())
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(
            request, HttpResponse.BodyHandlers.ofString()
        );

        int status = response.statusCode();
        log.debug("GET {} → HTTP {}", url, status);

        // 404 — sem documentos
        if (status == 404) {
          log.info("Nenhum documento encontrado.");
          DfeResponse empty = new DfeResponse();
          empty.setStatusProcessamento("NENHUM_DOCUMENTO_LOCALIZADO");
          return empty;
        }

        // 429 — rate limit → aguarda 60s e retenta
        if (status == 429) {
          log.warn("Rate limit atingido (429). Aguardando {}s antes de retentar... " +
              "(tentativa {}/{})", RATE_LIMIT_WAIT_MS / 1000, tentativa, MAX_RETRIES);
          sleep(RATE_LIMIT_WAIT_MS);
          continue;
        }

        // 200 — sucesso
        if (status == 200) {
          return objectMapper.readValue(response.body(), DfeResponse.class);
        }

        // Outros erros — retenta com delay menor
        log.warn("Erro HTTP {} na tentativa {}/{}. Aguardando {}s...",
            status, tentativa, MAX_RETRIES, RETRY_WAIT_MS / 1000);

        if (tentativa < MAX_RETRIES) {
          sleep(RETRY_WAIT_MS);
        } else {
          throw new NfseException(
              "Erro na API de distribuição após " + MAX_RETRIES +
                  " tentativas. HTTP " + status + ": " + response.body()
          );
        }

      } catch (NfseException e) {
        throw e;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new NfseException("Thread interrompida durante retry.", e);
      } catch (Exception e) {
        if (tentativa < MAX_RETRIES) {
          log.warn("Erro na tentativa {}/{}: {}. Retentando...",
              tentativa, MAX_RETRIES, e.getMessage());
          sleep(RETRY_WAIT_MS);
        } else {
          throw new NfseException(
              "Erro ao consultar API de distribuição após " +
                  MAX_RETRIES + " tentativas: " + url, e
          );
        }
      }
    }

    throw new NfseException("Máximo de tentativas atingido para: " + url);
  }

  private void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private String getBaseUrl() {
    return config.getAmbiente().isHomologacao()
        ? BASE_PRODUCAO_RESTRITA
        : BASE_PRODUCAO;
  }
}