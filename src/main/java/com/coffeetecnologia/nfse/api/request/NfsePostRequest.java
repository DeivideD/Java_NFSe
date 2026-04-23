package com.coffeetecnologia.nfse.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Body do endpoint POST /nfse da Sefin Nacional.
 *
 * Conforme Swagger NFSePostRequest:
 * {
 *   "dpsXmlGZipB64": "xml_comprimido_gzip_base64"
 * }
 */
@Getter
@Builder
public class NfsePostRequest {

  /**
   * DPS compactado no padrão GZip (base64Binary).
   * Campo exato conforme Swagger: dpsXmlGZipB64
   */
  @JsonProperty("dpsXmlGZipB64")
  private final String dpsXmlGZipB64;
}