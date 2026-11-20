package com.coffeetecnologia.nfse.exception;

/**
 * Sem resposta útil da Sefin (timeout, conexão, erro 5xx sem lista de erros). Na emissão, a lib já
 * consultou o DPS antes de lançar esta exceção e a Sefin não tinha a nota.
 */
public class NfseComunicacaoException extends NfseException {

  public NfseComunicacaoException(String message) {
    super(message);
  }

  public NfseComunicacaoException(String message, Throwable cause) {
    super(message, cause);
  }
}
