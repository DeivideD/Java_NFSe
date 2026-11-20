package com.coffeetecnologia.nfse.model.dps;

/**
 * Regime de apuração do optante ME/EPP que ultrapassou sublimite ou limite do Simples Nacional
 * ({@code <regTrib><regApTribSN>}). Só vale com {@link OpcaoSimplesNacional#ME_EPP}.
 */
public enum RegimeApuracaoSimplesNacional {
  /** Tributos federais e municipal apurados pelo SN. */
  FEDERAIS_E_MUNICIPAL_PELO_SN("1"),
  /** Tributos federais pelo SN e ISSQN por fora, conforme a legislação municipal. */
  FEDERAIS_PELO_SN_ISSQN_POR_FORA("2"),
  /** Tributos federais e municipal por fora do SN. */
  FEDERAIS_E_MUNICIPAL_POR_FORA("3");

  private final String codigo;

  RegimeApuracaoSimplesNacional(String codigo) {
    this.codigo = codigo;
  }

  public String getCodigo() {
    return codigo;
  }
}
