package com.coffeetecnologia.nfse.model.dps;

/** Regime especial de tributação do prestador ({@code <regTrib><regEspTrib>}). */
public enum RegimeEspecialTributacao {
  NENHUM("0"),
  ATO_COOPERADO("1"),
  ESTIMATIVA("2"),
  MICROEMPRESA_MUNICIPAL("3"),
  NOTARIO_OU_REGISTRADOR("4"),
  PROFISSIONAL_AUTONOMO("5"),
  SOCIEDADE_DE_PROFISSIONAIS("6"),
  OUTROS("9");

  private final String codigo;

  RegimeEspecialTributacao(String codigo) {
    this.codigo = codigo;
  }

  public String getCodigo() {
    return codigo;
  }
}
