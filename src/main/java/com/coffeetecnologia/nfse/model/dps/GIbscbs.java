package com.coffeetecnologia.nfse.model.dps;

import lombok.Builder;
import lombok.Getter;

/**
 * Grupo de informações relacionadas ao IBS e à CBS (gIBSCBS).
 * Caminho XML: infDPS/IBSCBS/valores/trib/gIBSCBS/
 *
 * Campos obrigatórios quando o grupo IBSCBS é informado.
 * Validação de exigibilidade pelo Ambiente Nacional suspensa em 2026 (NT 004 v2.0).
 */
@Getter
@Builder
public class GIbscbs {

  /** Código de Situação Tributária do IBS e da CBS. Formato: 3 dígitos numéricos [0-9]{3}. */
  private final String cst;

  /** Código de Classificação Tributária do IBS e da CBS. Formato: 6 dígitos numéricos [0-9]{6}. */
  private final String cClassTrib;
}
