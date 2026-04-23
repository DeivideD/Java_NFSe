package com.coffeetecnologia.nfse.api.request;

import lombok.Builder;
import lombok.Getter;

/**
 * Payload para cancelamento de NFS-e via DELETE na API Nacional.
 */
@Getter
@Builder
public class CancelarNfseRequest {

  private final String numero;
  private final String motivo;

  /** Código do motivo conforme tabela da RFB (padrão: "2" = Erro na emissão). */
  @Builder.Default
  private final String codigoMotivo = "2";
}