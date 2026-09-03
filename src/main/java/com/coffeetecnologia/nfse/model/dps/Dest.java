package com.coffeetecnologia.nfse.model.dps;

import lombok.Builder;
import lombok.Getter;

/**
 * Dados do destinatário do serviço (dest).
 * Caminho XML: infDPS/IBSCBS/dest/
 *
 * Obrigatório quando {@code Ibscbs.indDest = "1"} (destinatário é pessoa diferente do tomador).
 * Exatamente um dos campos de identificação (cnpj, cpf, nif, cNaoNIF) deve ser informado.
 */
@Getter
@Builder
public class Dest {

  /** CNPJ do destinatário (14 dígitos). Informar apenas um dos campos de identificação. */
  private final String cnpj;

  /** CPF do destinatário (11 dígitos). Informar apenas um dos campos de identificação. */
  private final String cpf;

  /** NIF do destinatário (para não residentes no Brasil, max 40 chars). */
  private final String nif;

  /**
   * Motivo para não informação do NIF: 0=não informado, 1=dispensado, 2=não exigível.
   * Informar apenas um dos campos de identificação.
   */
  private final String cNaoNIF;

  /** Nome / Razão Social do destinatário (obrigatório, max 150 chars). */
  private final String xNome;
}
