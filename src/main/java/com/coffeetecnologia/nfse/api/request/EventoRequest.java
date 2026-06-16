package com.coffeetecnologia.nfse.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Requisição de registro de evento (cancelamento) — POST /nfse/{chaveAcesso}/eventos.
 */
@Getter
@Builder
public class EventoRequest {

  @JsonProperty("pedidoRegistroEventoXmlGZipB64")
  private final String pedidoRegistroEventoXmlGZipB64;
}
