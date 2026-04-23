package com.coffeetecnologia.nfse.auth;

import com.coffeetecnologia.nfse.config.NfseConfig;
import com.coffeetecnologia.nfse.exception.NfseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/**
 * Gerencia a obtenção e renovação do token JWT da API Nacional de NFS-e.
 *
 * <p>A API utiliza autenticação mTLS (mutual TLS) com o certificado digital,
 * retornando um Bearer Token usado nas demais requisições.
 */
public class TokenManager {

  private static final String AUTH_PATH = "/api/v1/autenticar";

  private final NfseConfig config;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  private String tokenAtual;
  private Instant tokenExpiracao;

  public TokenManager(NfseConfig config, HttpClient httpClient) {
    this.config = config;
    this.httpClient = httpClient;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Retorna um token JWT válido, obtendo um novo se necessário.
   *
   * @return Bearer token para uso nas requisições
   */
  public synchronized String getToken() {
    if (tokenAtual == null || isExpirado()) {
      renovarToken();
    }
    return tokenAtual;
  }

  /**
   * Força a renovação do token, útil quando a API retorna 401.
   */
  public synchronized void renovarToken() {
    try {
      String url = config.getAmbiente().getBaseUrl() + AUTH_PATH;

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(config.getTimeout())
          .POST(HttpRequest.BodyPublishers.noBody())
          .build();

      HttpResponse<String> response = httpClient.send(
          request, HttpResponse.BodyHandlers.ofString()
      );

      if (response.statusCode() != 200) {
        throw new NfseException("Falha na autenticação: HTTP " + response.statusCode()
            + " - " + response.body());
      }

      JsonNode json = objectMapper.readTree(response.body());
      this.tokenAtual = json.get("access_token").asText();

      // Subtrai 60s da expiração para renovar com margem de segurança
      long expiresIn = json.has("expires_in") ? json.get("expires_in").asLong() : 3600L;
      this.tokenExpiracao = Instant.now().plus(Duration.ofSeconds(expiresIn - 60));

    } catch (NfseException e) {
      throw e;
    } catch (Exception e) {
      throw new NfseException("Erro ao obter token JWT da API NFS-e.", e);
    }
  }

  private boolean isExpirado() {
    return tokenExpiracao == null || Instant.now().isAfter(tokenExpiracao);
  }
}