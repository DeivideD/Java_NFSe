package com.coffeetecnologia.nfse.model.nfse;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * NFS-e emitida e armazenada pelo sistema nacional.
 */
@Getter
@Builder
public class Nfse {

  /** Número da NFS-e (gerado pelo fisco). */
  private final String numero;

  /** Código de verificação da NFS-e. */
  private final String codigoVerificacao;

  /** Chave de acesso (44 dígitos). */
  private final String chaveAcesso;

  /** Data e hora de emissão pela prefeitura. */
  private final LocalDateTime dataEmissao;

  /** Situação atual da NFS-e. */
  private final SituacaoNfse situacao;

  /** XML completo da NFS-e assinado pelo fisco. */
  private final String xmlNfse;

  /** URL do DANFSE (Documento Auxiliar da NFS-e) para visualização. */
  private final String urlDanfse;

  /** Número do DPS que originou esta NFS-e. */
  private final String numeroDps;

  public boolean isCancelada() {
    return SituacaoNfse.CANCELADA.equals(situacao);
  }
}