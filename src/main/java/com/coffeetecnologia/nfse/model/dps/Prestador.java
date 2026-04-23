package com.coffeetecnologia.nfse.model.dps;

import lombok.Builder;
import lombok.Getter;

/**
 * Dados do prestador de serviço (emitente da NFS-e).
 */
@Getter
@Builder
public class Prestador {

  private final String cnpj;
  private final String cpf;
  private final String inscricaoMunicipal;
  private final String codigoMunicipio;

  /** Cria prestador identificado por CNPJ (apenas dígitos). */
  public static Prestador comCnpj(String cnpj) {
    return Prestador.builder().cnpj(cnpj.replaceAll("[^0-9]", "")).build();
  }

  /** Cria prestador identificado por CPF (apenas dígitos). */
  public static Prestador comCpf(String cpf) {
    return Prestador.builder().cpf(cpf.replaceAll("[^0-9]", "")).build();
  }

  public boolean isCnpj() {
    return cnpj != null && !cnpj.isBlank();
  }
}