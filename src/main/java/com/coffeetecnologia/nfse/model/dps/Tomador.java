package com.coffeetecnologia.nfse.model.dps;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Dados do tomador de serviço (quem contrata/recebe o serviço).
 */
@Getter
@Builder
public class Tomador {

  private final String cnpj;
  private final String cpf;
  private final String nome;
  private final String email;
  private final Endereco endereco;

  public static Tomador comCnpj(String cnpj) {
    return Tomador.builder().cnpj(cnpj.replaceAll("[^0-9]", "")).build();
  }

  public static Tomador comCpf(String cpf) {
    return Tomador.builder().cpf(cpf.replaceAll("[^0-9]", "")).build();
  }

  public boolean isEstrangeiro() {
    return cnpj == null && cpf == null;
  }

  @Getter
  @Builder
  public static class Endereco {
    private final String logradouro;
    private final String numero;
    private final String complemento;
    private final String bairro;
    private final String codigoMunicipio;
    private final String uf;
    private final String cep;
  }
}