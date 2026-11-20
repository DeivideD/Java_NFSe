package com.coffeetecnologia.nfse.auth;

import com.coffeetecnologia.nfse.api.NfseApiClient;
import com.coffeetecnologia.nfse.config.Ambiente;
import com.coffeetecnologia.nfse.config.NfseConfig;
import com.coffeetecnologia.nfse.exception.NfseException;
import com.coffeetecnologia.nfse.model.dps.Dps;
import com.coffeetecnologia.nfse.model.dps.Prestador;
import com.coffeetecnologia.nfse.model.dps.Servico;
import com.coffeetecnologia.nfse.model.dps.Tomador;
import com.coffeetecnologia.nfse.model.dps.Valores;
import com.coffeetecnologia.nfse.model.nfse.Nfse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Emissão contra uma Sefin simulada: leitura do resultado e recuperação quando a resposta se perde. */
class NfseApiClientEmissaoTest {

  private static final String CHAVE = "23044002200000000191000000000004226110000000000001";
  private static final String NFSE_XML = "<NFSe xmlns=\"http://www.sped.fazenda.gov.br/nfse\"><infNFSe Id=\"NFS"
      + CHAVE + "\"><nNFSe>42</nNFSe><dhProc>2026-11-20T10:15:30-03:00</dhProc></infNFSe></NFSe>";

  private final HttpClient httpClient = mock(HttpClient.class);
  private final List<String> chamadas = new ArrayList<>();
  private NfseApiClient client;

  @BeforeEach
  void setUp() throws Exception {
    TestCertificates.Issued ca = TestCertificates.selfSignedCa("CN=AC Teste, O=Teste, C=BR");
    TestCertificates.Issued empresa = TestCertificates.issue("CN=EMPRESA TESTE, O=Teste, C=BR", ca);
    KeyStore ks = TestCertificates.pfxOnlyLeaf("empresa", empresa);
    ByteArrayOutputStream pfx = new ByteArrayOutputStream();
    ks.store(pfx, TestCertificates.SENHA);
    CertificadoDigital cert = CertificadoDigital.fromStream(new ByteArrayInputStream(pfx.toByteArray()),
        new String(TestCertificates.SENHA));
    NfseConfig config = NfseConfig.builder().certificado(cert).ambiente(Ambiente.PRODUCAO_RESTRITA)
        .validarXml(false).build();
    client = new NfseApiClient(config, httpClient);
  }

  @Test
  void emissaoComSucessoTrazNumeroEDataDoXmlDaSefin() throws Exception {
    responder(postResponde(200, sucessoJson()), null, null);

    Nfse nfse = client.emitir(dps());

    assertEquals(CHAVE, nfse.getChaveAcesso());
    assertEquals("42", nfse.getNumero());
    assertEquals(LocalDateTime.of(2026, 11, 20, 10, 15, 30), nfse.getDataEmissao());
    assertTrue(nfse.getNumeroDps().startsWith("DPS2304400"));
  }

  @Test
  void quedaDeConexaoComNotaGeradaRecuperaPeloDps() throws Exception {
    responder(null, resposta(200, "{\"chaveAcesso\":\"" + CHAVE + "\"}"), resposta(200, sucessoJson()));

    Nfse nfse = client.emitir(dps());

    assertEquals(CHAVE, nfse.getChaveAcesso());
    assertEquals("42", nfse.getNumero());
    assertEquals(List.of("POST /nfse", "GET /dps", "GET /nfse"), chamadas);
  }

  @Test
  void quedaDeConexaoSemNotaNaSefinMantemOErro() throws Exception {
    responder(null, resposta(404, ""), null);

    NfseException e = assertThrows(NfseException.class, () -> client.emitir(dps()));

    assertTrue(e.getMessage().contains("Erro de comunicação"), e.getMessage());
  }

  @Test
  void rejeicaoNaoConsultaODps() throws Exception {
    String rejeicao = "{\"erros\":[{\"Codigo\":\"E0234\",\"Descricao\":\"O endereço do tomador é obrigatório\"}]}";
    responder(postResponde(400, rejeicao), null, null);

    NfseException e = assertThrows(NfseException.class, () -> client.emitir(dps()));

    assertTrue(e.getMessage().contains("E0234"), e.getMessage());
    assertEquals(List.of("POST /nfse"), chamadas);
  }

  @Test
  void erroDoGatewayConsultaODpsAntesDeFalhar() throws Exception {
    responder(postResponde(502, "<html>Bad Gateway</html>"), resposta(404, ""), null);

    NfseException e = assertThrows(NfseException.class, () -> client.emitir(dps()));

    assertTrue(e.getMessage().contains("502"), e.getMessage());
    assertEquals(List.of("POST /nfse", "GET /dps"), chamadas);
  }

  // ========================
  // Sefin simulada
  // ========================

  /** post nulo = a conexão cai no envio. */
  @SuppressWarnings("unchecked")
  private void responder(HttpResponse<String> post, HttpResponse<String> getDps, HttpResponse<String> getNfse)
      throws Exception {
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(inv -> {
      HttpRequest req = inv.getArgument(0);
      String path = req.uri().getPath();
      if ("POST".equals(req.method())) {
        chamadas.add("POST /nfse");
        if (post == null) throw new HttpTimeoutException("request timed out");
        return post;
      }
      if (path.contains("/dps/")) {
        chamadas.add("GET /dps");
        return getDps;
      }
      chamadas.add("GET /nfse");
      return getNfse;
    });
  }

  private static HttpResponse<String> postResponde(int status, String body) {
    return resposta(status, body);
  }

  @SuppressWarnings("unchecked")
  private static HttpResponse<String> resposta(int status, String body) {
    HttpResponse<String> r = mock(HttpResponse.class);
    when(r.statusCode()).thenReturn(status);
    when(r.body()).thenReturn(body);
    return r;
  }

  private static String sucessoJson() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
      gzip.write(NFSE_XML.getBytes(StandardCharsets.UTF_8));
    }
    return "{\"chaveAcesso\":\"" + CHAVE + "\",\"nfseXmlGZipB64\":\""
        + Base64.getEncoder().encodeToString(baos.toByteArray()) + "\"}";
  }

  private static Dps dps() {
    return Dps.builder()
        .numero("1")
        .serie("1")
        .codigoMunicipio("2304400")
        .prestador(Prestador.builder().cnpj("00000000000191").codigoMunicipio("2304400").build())
        .tomador(Tomador.builder().cnpj("11222333000181").nome("Cliente").build())
        .servico(Servico.builder().codigoServico("010700").descricao("Software").build())
        .valores(Valores.builder().valorServico(new BigDecimal("100.00")).build())
        .build();
  }
}
