package com.coffeetecnologia.nfse.model.dps;

/** Situação do prestador perante o Simples Nacional ({@code <regTrib><opSimpNac>}). */
public enum OpcaoSimplesNacional {
  NAO_OPTANTE("1"),
  MEI("2"),
  ME_EPP("3");

  private final String codigo;

  OpcaoSimplesNacional(String codigo) {
    this.codigo = codigo;
  }

  public String getCodigo() {
    return codigo;
  }
}
