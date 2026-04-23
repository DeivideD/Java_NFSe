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
 * Endpoint base (Produção Restrita):
 *   https://adn.producaorestrita.nfse.gov.br/contribuintes
 *
 * Endpoints implementados:
 *   GET /DFe/{NSU}                         → Busca documento por NSU
 *   GET /DFe/{NSU}?lote=true               → Busca lote a partir do NSU
 *   GET /NFSe/{ChaveAcesso}/Eventos        → Busca eventos de uma NFS-e
 */
public class DistribuicaoApiClient {

  private static final Logger log = LoggerFactory.getLogger(DistribuicaoApiClient.class);

  private static final String BASE_PRODUCAO_RESTRITA =
      "https://adn.producaorestrita.nfse.gov.br/contribuintes";
  private static final String BASE_PRODUCAO =
      "https://adn.nfse.gov.br/contribuintes";

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
   *
   * GET /DFe/{NSU}?lote=false
   *
   * @param nsu Número Sequencial Único do documento
   */
  public DfeResponse buscarPorNsu(long nsu) {
    String url = getBaseUrl() + "/DFe/" + nsu + "?lote=false";
    log.info("Buscando DF-e por NSU: {}", nsu);
    return get(url);
  }

  /**
   * Busca um lote de documentos a partir de um NSU.
   * Retorna até 50 documentos com NSU >= ao informado.
   *
   * GET /DFe/{NSU}?lote=true
   *
   * @param nsuInicial NSU a partir do qual buscar
   */
  public DfeResponse buscarLote(long nsuInicial) {
    String url = getBaseUrl() + "/DFe/" + nsuInicial + "?lote=true";
    log.info("Buscando lote de DF-e a partir do NSU: {}", nsuInicial);
    return get(url);
  }

  /**
   * Busca lote filtrando por CNPJ específico.
   *
   * GET /DFe/{NSU}?cnpjConsulta={cnpj}&lote=true
   *
   * @param nsuInicial  NSU inicial
   * @param cnpj        CNPJ a filtrar (apenas dígitos)
   */
  public DfeResponse buscarLotePorCnpj(long nsuInicial, String cnpj) {
    String url = getBaseUrl() + "/DFe/" + nsuInicial
        + "?cnpjConsulta=" + cnpj + "&lote=true";
    log.info("Buscando lote por CNPJ {} a partir do NSU: {}", cnpj, nsuInicial);
    return get(url);
  }

  // ========================
  // Eventos
  // ========================

  /**
   * Busca todos os eventos vinculados a uma NFS-e.
   *
   * GET /NFSe/{ChaveAcesso}/Eventos
   *
   * @param chaveAcesso chave de acesso da NFS-e (50 dígitos)
   */
  public DfeResponse buscarEventos(String chaveAcesso) {
    String url = getBaseUrl() + "/NFSe/" + chaveAcesso + "/Eventos";
    log.info("Buscando eventos da NFS-e: {}", chaveAcesso);
    return get(url);
  }

  // ========================
  // Utilitário de descompressão
  // ========================

  /**
   * Descomprime o campo ArquivoXml (GZip + Base64) retornado pela API.
   *
   * @param arquivoXmlGZipB64 valor do campo ArquivoXml do documento
   * @return XML do documento como String
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
  // HTTP
  // ========================

  private DfeResponse get(String url) {
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

      log.debug("GET {} → HTTP {}", url, response.statusCode());

      if (response.statusCode() == 404) {
        log.info("Nenhum documento encontrado para a consulta.");
        DfeResponse empty = new DfeResponse();
        empty.setStatusProcessamento("NENHUM_DOCUMENTO_LOCALIZADO");
        return empty;
      }

      if (response.statusCode() != 200) {
        throw new NfseException(
            "Erro na API de distribuição. HTTP " + response.statusCode()
                + ": " + response.body()
        );
      }

      return objectMapper.readValue(response.body(), DfeResponse.class);

    } catch (NfseException e) {
      throw e;
    } catch (Exception e) {
      throw new NfseException("Erro ao consultar API de distribuição: " + url, e);
    }
  }

  private String getBaseUrl() {
    return config.getAmbiente().isHomologacao()
        ? BASE_PRODUCAO_RESTRITA
        : BASE_PRODUCAO;
  }
}