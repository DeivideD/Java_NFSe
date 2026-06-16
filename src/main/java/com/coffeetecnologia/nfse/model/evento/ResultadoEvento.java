package com.coffeetecnologia.nfse.model.evento;

import lombok.Builder;
import lombok.Getter;

/**
 * Resultado do registro de evento (cancelamento) de uma NFS-e.
 */
@Getter
@Builder
public class ResultadoEvento {

  /** Protocolo de registro do evento. */
  private final String protocolo;

  /** Chave de acesso da NFS-e relacionada ao evento. */
  private final String chaveNfse;

  /** Código de status do processamento. */
  private final String cStat;

  /** Descrição do status do processamento. */
  private final String xMotivo;

  /** Indica se o evento foi registrado com sucesso. */
  private final boolean sucesso;
}
