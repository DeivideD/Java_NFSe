package com.coffeetecnologia.nfse.api;

import com.coffeetecnologia.nfse.api.request.EventoRequest;
import com.coffeetecnologia.nfse.api.request.NfsePostRequest;
import com.coffeetecnologia.nfse.api.response.EventoResponse;
import com.coffeetecnologia.nfse.api.response.NfsePostResponse;
import com.coffeetecnologia.nfse.config.NfseConfig;
import com.coffeetecnologia.nfse.config.Leiaute;
import com.coffeetecnologia.nfse.exception.NfseComunicacaoException;
import com.coffeetecnologia.nfse.exception.NfseException;
import com.coffeetecnologia.nfse.exception.NfseRejeitadaException;
import com.coffeetecnologia.nfse.model.dps.DpsMontada;
import com.coffeetecnologia.nfse.model.dps.Dps;
import com.coffeetecnologia.nfse.model.dps.Substituicao;
import com.coffeetecnologia.nfse.model.evento.PedidoEvento;
import com.coffeetecnologia.nfse.model.evento.ResultadoEvento;
import com.coffeetecnologia.nfse.model.nfse.Nfse;
import com.coffeetecnologia.nfse.model.nfse.SituacaoNfse;
import com.coffeetecnologia.nfse.xml.XmlBuilder;
import com.coffeetecnologia.nfse.xml.XmlEventoBuilder;
import com.coffeetecnologia.nfse.xml.XmlSigner;
import com.coffeetecnologia.nfse.xml.XmlValidator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
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
  private static final String NS = "http://www.sped.fazenda.gov.br/nfse";
  private static final ZoneId ZONE_BR = ZoneId.of("America/Sao_Paulo");

  private final NfseConfig config;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final XmlBuilder xmlBuilder;
  private final XmlEventoBuilder xmlEventoBuilder;
  private final XmlSigner xmlSigner;
  private final XmlValidator xmlValidator;

  public NfseApiClient(NfseConfig config, HttpClient httpClient) {
    this.config = config;
    this.httpClient = httpClient;
    this.objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    int tpAmb = config.getAmbiente().getTpAmb();
    this.xmlBuilder = new XmlBuilder(config.getAmbiente(), config.getLeiaute());
    this.xmlEventoBuilder = new XmlEventoBuilder(tpAmb);
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
    String idDps = idDps(documento);

    // 2. Valida contra XSD (opcional)
    if (config.isValidarXml()) {
      xmlValidator.validarOuLancar(xmlBuilder.toXmlString(documento), xmlBuilder.leiaute());
    }

    // 3. Assina
    String xmlAssinado = xmlSigner.assinar(documento, config.getCertificado());
    log.debug("DPS gerado e assinado.");

    // 4. GZip + Base64
    String dpsGZipB64 = gzipBase64(xmlAssinado);

    // 5. Monta request e envia
    String bodyJson;
    try {
      bodyJson = objectMapper.writeValueAsString(NfsePostRequest.builder().dpsXmlGZipB64(dpsGZipB64).build());
    } catch (Exception e) {
      throw new NfseException("Erro ao emitir NFS-e.", e);
    }
    String url = config.getAmbiente().getEndpointEmissao();
    log.info("POST {} (DPS {})", url, idDps);

    HttpResponse<String> response;
    try {
      response = post(url, bodyJson);
    } catch (NfseException falhaDeComunicacao) {
      // A conexão pode cair depois de a Sefin gerar a nota: sem consultar, o chamador acharia que
      // falhou e uma nova emissão duplicaria o serviço.
      return recuperarPorDps(idDps).orElseThrow(() -> falhaDeComunicacao);
    }
    log.info("Emissão: HTTP {}", response.statusCode());
    log.debug("Resposta da emissão: {}", response.body());

    // 6. Trata resposta
    if (response.statusCode() == 400 || response.statusCode() >= 500) {
      NfsePostResponse erro = lerOuNull(response.body());
      if (erro != null && erro.getErros() != null && !erro.getErros().isEmpty()) {
        throw new NfseRejeitadaException("DPS rejeitada pela Sefin Nacional:\n" + erro.getErrosFormatados(),
            rejeicoes(erro.getErros()));
      }
      if (response.statusCode() >= 500) {
        Optional<Nfse> recuperada = recuperarPorDps(idDps);
        if (recuperada.isPresent()) return recuperada.get();
        throw new NfseComunicacaoException("Emissão — HTTP " + response.statusCode() + " sem resposta da Sefin.");
      }
    }
    tratarErroHttp(response, "Emissão");

    try {
      NfsePostResponse sucesso = objectMapper.readValue(response.body(), NfsePostResponse.class);
      log.info("NFS-e emitida! Chave: {}", sucesso.getChaveAcesso());
      String xmlNfse = sucesso.getNfseXmlGZipB64() != null ? descomprimirGzipBase64(sucesso.getNfseXmlGZipB64()) : null;
      return montarNfse(sucesso.getChaveAcesso(), xmlNfse, sucesso.getDataHoraProcessamento(),
          sucesso.getIdDps() != null ? sucesso.getIdDps() : idDps);
    } catch (NfseException e) {
      throw e;
    } catch (Exception e) {
      throw new NfseException("Erro ao emitir NFS-e.", e);
    }
  }

  /**
   * Monta o DPS como a emissão montaria e valida contra o XSD do leiaute vigente, sem assinar e sem
   * enviar nada à Sefin.
   */
  public DpsMontada montarDps(Dps dps) {
    Document documento = xmlBuilder.buildDocument(dps);
    Leiaute leiaute = xmlBuilder.leiaute();
    String xml = xmlBuilder.toXmlString(documento);
    return new DpsMontada(idDps(documento), xml, leiaute, xmlValidator.validarDps(xml, leiaute));
  }

  /**
   * Procura a NFS-e gerada a partir do DPS (GET /dps/{id} e depois GET /nfse/{chave}). Vazio quando a
   * Sefin não tem a nota ou a consulta também falha: nesse caso o erro original da emissão prevalece.
   */
  public Optional<Nfse> recuperarPorDps(String idDps) {
    try {
      HttpResponse<String> response = get(config.getAmbiente().getEndpointDps(idDps));
      if (response.statusCode() != 200) {
        log.warn("DPS {} não encontrado na Sefin após falha na emissão (HTTP {}).", idDps, response.statusCode());
        return Optional.empty();
      }
      String chave = objectMapper.readValue(response.body(), NfsePostResponse.class).getChaveAcesso();
      if (chave == null || chave.isBlank()) return Optional.empty();
      log.warn("Emissão do DPS {} não teve resposta, mas a Sefin gerou a NFS-e {}. Recuperada.", idDps, chave);
      Nfse nfse = consultar(chave);
      return Optional.of(nfse.toBuilder().numeroDps(idDps).build());
    } catch (Exception e) {
      log.warn("Não foi possível consultar o DPS {} após falha na emissão: {}", idDps, e.getMessage());
      return Optional.empty();
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

      return montarNfse(chaveAcesso, xmlNfse, r.getDataHoraProcessamento(), r.getIdDps());
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
   */
  public ResultadoEvento cancelar(String chaveAcesso, PedidoEvento pedido) {
    log.info("Cancelando NFS-e. Chave: {}", chaveAcesso);

    try {
      Document doc = xmlEventoBuilder.buildDocument(pedido);
      String xmlAssinado = xmlSigner.assinarEvento(doc, config.getCertificado());
      String gzipB64 = gzipBase64(xmlAssinado);

      String url = config.getAmbiente().getEndpointEventos(chaveAcesso);
      EventoRequest request = EventoRequest.builder()
          .pedidoRegistroEventoXmlGZipB64(gzipB64)
          .build();

      String bodyJson = objectMapper.writeValueAsString(request);
      log.info("POST {}", url);

      HttpResponse<String> response = post(url, bodyJson);

      log.info("Evento: HTTP {}", response.statusCode());
      log.debug("Resposta do evento: {}", response.body());

      if (response.statusCode() == 400 || response.statusCode() == 500) {
        EventoResponse erro = objectMapper.readValue(response.body(), EventoResponse.class);
        throw new NfseRejeitadaException("Evento rejeitado pela Sefin Nacional:\n" + erro.getErrosFormatados(),
            rejeicoes(erro.getErros()));
      }

      tratarErroHttp(response, "Cancelamento");

      EventoResponse sucesso = objectMapper.readValue(response.body(), EventoResponse.class);
      log.info("Evento registrado! Protocolo: {}", sucesso.getProtocolo());

      return ResultadoEvento.builder()
          .protocolo(sucesso.getProtocolo())
          .chaveNfse(sucesso.getChNFSe() != null ? sucesso.getChNFSe() : chaveAcesso)
          .cStat(sucesso.getCstat())
          .xMotivo(sucesso.getXMotivo())
          .sucesso(sucesso.isSuccess())
          .build();

    } catch (NfseException e) {
      throw e;
    } catch (Exception e) {
      throw new NfseException("Erro ao cancelar NFS-e.", e);
    }
  }

  // ========================
  // Substituição
  // ========================

  /**
   * Substitui uma NFS-e enviando um DPS com referência à nota original.
   */
  public Nfse substituir(String chaveNfseOriginal, Dps novaDps) {
    log.info("Substituindo NFS-e. Chave original: {}", chaveNfseOriginal);

    Substituicao subst = (novaDps.getSubst() != null) ? novaDps.getSubst() :
        Substituicao.builder()
            .chSubstda(chaveNfseOriginal)
            .cMotivo("99")
            .build();

    Dps dpsComSubst = novaDps.toBuilder().subst(subst).build();
    return emitir(dpsComSubst);
  }

  // ========================
  // Parâmetros Municipais
  // ========================

  /**
   * GET /parametros-municipais/{codigoMunicipio}/convenio
   * Retorna JSON com serviços habilitados e parâmetros do município.
   */
  public String consultarParametrosMunicipais(String codigoMunicipio) {
    log.info("Consultando parâmetros municipais. Município: {}", codigoMunicipio);
    String url = config.getAmbiente().getEndpointParametrosMunicipais(codigoMunicipio);
    HttpResponse<String> response = get(url);
    tratarErroHttp(response, "Consulta parâmetros municipais");
    return response.body();
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
      throw new NfseComunicacaoException("Erro de comunicação com a Sefin Nacional (POST): " + url, e);
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
      throw new NfseComunicacaoException("Erro de comunicação com a Sefin Nacional (GET): " + url, e);
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
  // Montagem do resultado
  // ========================

  /**
   * Número da NFS-e e data de processamento vêm do XML gerado pela Sefin ({@code <nNFSe>}, {@code <dhProc>});
   * na falta do XML, a data vem do JSON da resposta.
   */
  Nfse montarNfse(String chaveAcesso, String xmlNfse, OffsetDateTime dataHoraProcessamento, String idDps) {
    String numero = null;
    LocalDateTime dataEmissao = dataHoraProcessamento != null
        ? dataHoraProcessamento.atZoneSameInstant(ZONE_BR).toLocalDateTime() : null;
    if (xmlNfse != null) {
      try {
        Document nfse = lerXml(xmlNfse);
        numero = textoDe(nfse, "nNFSe");
        String dhProc = textoDe(nfse, "dhProc");
        if (dhProc != null) {
          dataEmissao = OffsetDateTime.parse(dhProc).atZoneSameInstant(ZONE_BR).toLocalDateTime();
        }
      } catch (Exception e) {
        log.warn("NFS-e {} emitida, mas o XML devolvido não pôde ser lido: {}", chaveAcesso, e.getMessage());
      }
    }
    return Nfse.builder()
        .chaveAcesso(chaveAcesso)
        .numero(numero)
        .dataEmissao(dataEmissao)
        .xmlNfse(xmlNfse)
        .numeroDps(idDps)
        .situacao(SituacaoNfse.NORMAL)
        .build();
  }

  private static String idDps(Document dps) {
    NodeList nodes = dps.getElementsByTagNameNS(NS, "infDPS");
    return nodes.getLength() == 0 ? null : ((Element) nodes.item(0)).getAttribute("Id");
  }

  private static Document lerXml(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
  }

  private static String textoDe(Document doc, String tag) {
    NodeList nodes = doc.getElementsByTagNameNS(NS, tag);
    if (nodes.getLength() == 0) return null;
    String texto = nodes.item(0).getTextContent();
    return texto == null || texto.isBlank() ? null : texto.trim();
  }

  private static List<NfseRejeitadaException.Rejeicao> rejeicoes(List<NfsePostResponse.MensagemProcessamento> erros) {
    if (erros == null) return List.of();
    return erros.stream()
        .map(e -> new NfseRejeitadaException.Rejeicao(e.getCodigo(), e.getDescricao(), e.getComplemento()))
        .toList();
  }

  private NfsePostResponse lerOuNull(String body) {
    try {
      return body == null || body.isBlank() ? null : objectMapper.readValue(body, NfsePostResponse.class);
    } catch (Exception e) {
      return null;
    }
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