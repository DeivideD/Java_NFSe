package com.coffeetecnologia.nfse.model.dps;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class Servico {

  private final String codigoServico;
  private final String descricao;
  private final String cnae;
  private final String codigoNbs;

  @Builder.Default
  private final BigDecimal quantidade = BigDecimal.ONE;

  @Builder.Default
  private final String unidade = "UN";

  /** Informações da obra — obrigatório para códigos 07.02, 07.04, 07.05, 07.06, 07.07, 07.08 */
  private final Obra obra;

  @Getter
  @Builder
  public static class Obra {
    private final String logradouro;
    private final String numero;
    private final String complemento; // opcional
    private final String bairro;
    private final String cep;
    private final String codigoMunicipio; // opcional, usa o da DPS se nulo
  }
}