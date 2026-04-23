package com.coffeetecnologia.nfse.api;

import com.coffeetecnologia.nfse.api.request.NfsePostRequest;
import com.coffeetecnologia.nfse.api.response.NfsePostResponse;
import com.coffeetecnologia.nfse.config.NfseConfig;
import com.coffeetecnologia.nfse.exception.NfseException;
import com.coffeetecnologia.nfse.model.dps.Dps;
import com.coffeetecnologia.nfse.model.nfse.Nfse;
import com.coffeetecnologia.nfse.model.nfse.SituacaoNfse;
import com.coffeetecnologia.nfse.xml.XmlBuilder;
import com.coffeetecnologia.nfse.xml.XmlSigner;
import com.coffeetecnologia.nfse.xml.XmlValidator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Cliente HTTP para a API Sefin Nacional NFS-e.
 *
 * Endpoints conforme Swagger oficial (sefin.producaorestrita.nfse.gov.br/SefinNacional):
 *
 *   POST /nfse                          → Emissão (DPS → NFS-e)
 *   GET  /nfse/{chaveAcesso}            → Consulta por chave
 *   GET  /dps/{id}                      → Consulta chave pelo id do DPS
 *   POST /nfse/{chaveAcesso}/eventos    → Registro de eventos (cancelamento etc)
 *
 * Request de emissão:  { "dpsXmlGZipB64": "..." }
 * Response de sucesso: { "chaveAcesso": "...", "nfseXmlGZipB64": "...", "idDps": "..." }
 */
public class NfseApiClient {

  private static final Logger log = LoggerFactory.getLogger(NfseApiClient.class);
  private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";

  private final NfseConfig config;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final XmlBuilder xmlBuilder;
  private final XmlSigner xmlSigner;
  private final XmlValidator xmlValidator;

  public NfseApiClient(NfseConfig config, HttpClient httpClient) {
    this.config = config;
    this.httpClient = httpClient;
    this.objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    this.xmlBuilder = new XmlBuilder();
    this.xmlSigner = new XmlSigner();
    this.xmlValidator = new XmlValidator();
  }

  // ========================
  // Emissão
  // ========================

  /**
   * Emite uma NFS-e enviando o DPS assinado para a Sefin Nacional.
   *
   * Fluxo:
   * 1. Gera XML do DPS (XmlBuilder)
   * 2. Valida contra XSD (se habilitado)
   * 3. Assina digitalmente (XmlSigner - XMLDSig RSA-SHA256)
   * 4. Comprime com GZip e codifica em Base64
   * 5. POST /nfse com body: { "dpsXmlGZipB64": "..." }
   * 6. Retorna NFS-e com chaveAcesso e XML
   */
  public Nfse emitir(Dps dps) {
    log.info("Emitindo NFS-e. Prestador: {}", dps.getPrestador().isCnpj()
        ? dps.getPrestador().getCnpj() : dps.getPrestador().getCpf());

    // 1. Gera XML
    Document documento = xmlBuilder.buildDocument(dps);

    // 2. Valida contra XSD (opcional)
    if (config.isValidarXml()) {
      xmlValidator.validarOuLancar(xmlBuilder.toXmlString(documento));
    }

    // 3. Assina
    String xmlAssinado = xmlSigner.assinar(documento, config.getCertificado());
    log.debug("DPS gerado e assinado.");

    // 4. GZip + Base64
    String dpsGZipB64 = gzipBase64(xmlAssinado);

    // 5. Monta request e envia
    try {
      NfsePostRequest request = NfsePostRequest.builder()
          .dpsXmlGZipB64(dpsGZipB64)
          .build();

      String bodyJson = objectMapper.writeValueAsString(request);
      String url = config.getAmbiente().getEndpointEmissao();
      log.info("POST {}", url);

      HttpResponse<String> response = post(url, bodyJson);

      // LOG TEMPORÁRIO — remover depois
      log.info("HTTP Status: {}", response.statusCode());
      log.info("Response body: {}", response.body());

      // 6. Trata resposta
      if (response.statusCode() == 400 || response.statusCode() == 500) {
        NfsePostResponse erro = objectMapper.readValue(response.body(), NfsePostResponse.class);
        throw new NfseException("DPS rejeitada pela Sefin Nacional:\n" + erro.getErrosFormatados());
      }

      tratarErroHttp(response, "Emissão");

      NfsePostResponse sucesso = objectMapper.readValue(response.body(), NfsePostResponse.class);
      log.info("NFS-e emitida! Chave: {}", sucesso.getChaveAcesso());

      // Descomprime o XML da NFS-e para armazenar no modelo
      String xmlNfse = null;
      if (sucesso.getNfseXmlGZipB64() != null) {
        xmlNfse = descomprimirGzipBase64(sucesso.getNfseXmlGZipB64());
      }

      return Nfse.builder()
          .chaveAcesso(sucesso.getChaveAcesso())
          .numeroDps(sucesso.getIdDps())
          .xmlNfse(xmlNfse)
          .situacao(SituacaoNfse.NORMAL)
          .build();

    } catch (NfseException e) {
      throw e;
    } catch (Exception e) {
      throw new NfseException("Erro ao emitir NFS-e.", e);
    }
  }

  // ========================
  // Consulta
  // ========================

  /**
   * GET /nfse/{chaveAcesso}
   * Chave de acesso tem 50 dígitos.
   */
  public Nfse consultar(String chaveAcesso) {
    log.info("Consultando NFS-e. Chave: {}", chaveAcesso);

    HttpResponse<String> response = get(config.getAmbiente().getEndpointConsulta(chaveAcesso));
    tratarErroHttp(response, "Consulta");

    try {
      NfsePostResponse r = objectMapper.readValue(response.body(), NfsePostResponse.class);
      String xmlNfse = r.getNfseXmlGZipB64() != null
          ? descomprimirGzipBase64(r.getNfseXmlGZipB64()) : null;

      return Nfse.builder()
          .chaveAcesso(chaveAcesso)
          .xmlNfse(xmlNfse)
          .situacao(SituacaoNfse.NORMAL)
          .build();
    } catch (Exception e) {
      throw new NfseException("Erro ao parsear resposta da consulta.", e);
    }
  }

  /**
   * GET /dps/{id} — retorna a chave de acesso da NFS-e pelo id do DPS.
   */
  public String consultarChavePorDps(String idDps) {
    log.info("Consultando chave pelo DPS id: {}", idDps);

    HttpResponse<String> response = get(config.getAmbiente().getEndpointDps(idDps));
    tratarErroHttp(response, "Consulta DPS");

    try {
      NfsePostResponse r = objectMapper.readValue(response.body(), NfsePostResponse.class);
      return r.getChaveAcesso();
    } catch (Exception e) {
      throw new NfseException("Erro ao parsear resposta da consulta de DPS.", e);
    }
  }

  // ========================
  // Cancelamento via Evento
  // ========================

  /**
   * POST /nfse/{chaveAcesso}/eventos
   * Body: { "pedidoRegistroEventoXmlGZipB64": "..." }
   *
   * TODO: Implementar geração do XML de Pedido de Registro de Evento
   *       conforme AnexoII-LeiautesRN_Eventos-SNNFSe do manual da RFB.
   */
  public SituacaoNfse cancelar(String chaveAcesso, String motivo) {
    throw new UnsupportedOperationException(
        "Cancelamento via Evento ainda não implementado. " +
            "Requer geração do XML do Pedido de Registro de Evento."
    );
  }

  // ========================
  // HTTP
  // ========================

  private HttpResponse<String> post(String url, String bodyJson) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(config.getTimeout())
          .header("Content-Type", CONTENT_TYPE_JSON)
          .header("Accept", CONTENT_TYPE_JSON)
          .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
          .build();
      return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      throw new NfseException("Erro de comunicação com a Sefin Nacional (POST): " + url, e);
    }
  }

  private HttpResponse<String> get(String url) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(config.getTimeout())
          .header("Accept", CONTENT_TYPE_JSON)
          .GET()
          .build();
      return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      throw new NfseException("Erro de comunicação com a Sefin Nacional (GET): " + url, e);
    }
  }

  private void tratarErroHttp(HttpResponse<String> response, String operacao) {
    int status = response.statusCode();
    if (status == 200 || status == 201) return;
    if (status == 403) throw new NfseException(operacao + " — certificado inválido ou sem permissão (403).");
    if (status == 401) throw new NfseException(operacao + " — não foi possível obter o certificado (401).");
    if (status == 404) throw new NfseException(operacao + " — não encontrado (404): " + response.body());
    throw new NfseException(operacao + " — HTTP " + status + ": " + response.body());
  }

  // ========================
  // GZip + Base64
  // ========================

  private String gzipBase64(String xml) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
        gzip.write(xml.getBytes(StandardCharsets.UTF_8));
      }
      return Base64.getEncoder().encodeToString(baos.toByteArray());
    } catch (Exception e) {
      throw new NfseException("Erro ao comprimir XML com GZip.", e);
    }
  }

  private String descomprimirGzipBase64(String gzipB64) {
    try {
      byte[] compressed = Base64.getDecoder().decode(gzipB64);
      try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
        return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
      }
    } catch (Exception e) {
      throw new NfseException("Erro ao descomprimir XML da NFS-e.", e);
    }
  }
}