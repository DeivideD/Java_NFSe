package com.coffeetecnologia.nfse.exception;

/**
 * Exceção base da biblioteca Java-NFS-e.
 */
public class NfseException extends RuntimeException {

  public NfseException(String message) {
    super(message);
  }

  public NfseException(String message, Throwable cause) {
    super(message, cause);
  }
}