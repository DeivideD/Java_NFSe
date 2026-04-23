package com.coffeetecnologia.nfse.model.nfse;

/**
 * Situação da NFS-e conforme retornado pela API Nacional.
 */
public enum SituacaoNfse {

  /** NFS-e emitida normalmente. */
  NORMAL("1", "Normal"),

  /** NFS-e cancelada. */
  CANCELADA("2", "Cancelada"),

  /** NFS-e substituída por outra. */
  SUBSTITUIDA("3", "Substituída");

  private final String codigo;
  private final String descricao;

  SituacaoNfse(String codigo, String descricao) {
    this.codigo = codigo;
    this.descricao = descricao;
  }

  public String getCodigo() { return codigo; }
  public String getDescricao() { return descricao; }

  public static SituacaoNfse fromCodigo(String codigo) {
    for (SituacaoNfse s : values()) {
      if (s.codigo.equals(codigo)) return s;
    }
    throw new IllegalArgumentException("Situação NFS-e desconhecida: " + codigo);
  }
}