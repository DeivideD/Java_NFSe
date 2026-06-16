package com.coffeetecnologia.nfse.model.evento;

/**
 * Motivo de cancelamento de NFS-e conforme tabela da RFB.
 */
public enum MotivoCancelamento {

  ERRO_EMISSAO("1"),
  SERVICO_NAO_PRESTADO("2"),
  OUTROS("9");

  private final String codigo;

  MotivoCancelamento(String codigo) {
    this.codigo = codigo;
  }

  public String getCodigo() {
    return codigo;
  }
}
