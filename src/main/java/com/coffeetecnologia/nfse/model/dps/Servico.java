package com.coffeetecnologia.nfse.model.dps;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class Servico {

  private final String codigoServico;

  /**
   * Código de tributação municipal ({@code cTribMun}, 3 dígitos): o desdobro do {@code cTribNac} que o
   * município administra. Obrigatório onde o município parametrizou desdobros (ex.: Fortaleza usa 01.06.01.001;
   * sem ele a Sefin rejeita com E0312). Nulo = não informado.
   */
  private final String codigoTributacaoMunicipal;
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