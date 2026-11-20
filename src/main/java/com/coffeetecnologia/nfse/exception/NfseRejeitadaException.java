package com.coffeetecnologia.nfse.exception;

import java.util.List;

/**
 * A Sefin Nacional processou o pedido e o recusou (HTTP 400 com a lista de erros). Nada foi gerado:
 * corrigir e reenviar é seguro.
 */
public class NfseRejeitadaException extends NfseException {

  /** Erro devolvido pela Sefin: código (ex.: E0234), descrição e complemento, como vieram. */
  public record Rejeicao(String codigo, String descricao, String complemento) {
  }

  private final List<Rejeicao> rejeicoes;

  public NfseRejeitadaException(String message, List<Rejeicao> rejeicoes) {
    super(message);
    this.rejeicoes = rejeicoes == null ? List.of() : List.copyOf(rejeicoes);
  }

  public List<Rejeicao> getRejeicoes() {
    return rejeicoes;
  }
}
