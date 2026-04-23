package com.coffeetecnologia.nfse.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Body do endpoint POST /adn/DFe conforme Swagger oficial da API Nacional NFS-e.
 *
 * <p>O campo {@code LoteXmlGZipB64} é um array de strings, onde cada string
 * é um XML de DPS comprimido com GZip e codificado em Base64.
 *
 * <p>Suporta envio em lote (múltiplos DPS por requisição).
 */
@Getter
@Builder
public class RecepcaoRequest {

  /**
   * Array de XMLs de DPS, cada um comprimido com GZip e codificado em Base64.
   * Conforme schema: {@code LoteXmlGZipB64: string[]}
   */
  @JsonProperty("LoteXmlGZipB64")
  private final List<String> loteXmlGZipB64;

  /**
   * Cria uma requisição com um único DPS (caso mais comum).
   */
  public static RecepcaoRequest deUmDps(String xmlGZipB64) {
    return RecepcaoRequest.builder()
        .loteXmlGZipB64(List.of(xmlGZipB64))
        .build();
  }
}