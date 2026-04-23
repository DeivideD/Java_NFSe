package com.coffeetecnologia.nfse.model.dps;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Valores monetários do DPS: preço do serviço, tributos e deduções.
 */
@Getter
@Builder
public class Valores {

  /** Valor bruto do serviço antes de deduções. */
  private final BigDecimal valorServico;

  /** Deduções legais (materiais, subempreitadas, etc.). */
  @Builder.Default
  private final BigDecimal deducoes = BigDecimal.ZERO;

  /** Base de cálculo do ISS. Calculada automaticamente se não informada. */
  private final BigDecimal baseCalculo;

  /** Alíquota do ISS (ex: 0.05 para 5%). */
  private final BigDecimal aliquotaIss;

  /** Valor do ISS calculado. Calculado automaticamente se não informado. */
  private final BigDecimal valorIss;

  /** Valor retido de IRPJ. */
  @Builder.Default
  private final BigDecimal valorIr = BigDecimal.ZERO;

  /** Valor retido de PIS. */
  @Builder.Default
  private final BigDecimal valorPis = BigDecimal.ZERO;

  /** Valor retido de COFINS. */
  @Builder.Default
  private final BigDecimal valorCofins = BigDecimal.ZERO;

  /** Valor retido de CSLL. */
  @Builder.Default
  private final BigDecimal valorCsll = BigDecimal.ZERO;

  /** Valor retido de INSS. */
  @Builder.Default
  private final BigDecimal valorInss = BigDecimal.ZERO;

  /** Indica se o ISS é retido pelo tomador. */
  @Builder.Default
  private final boolean issRetido = false;

  /**
   * Calcula a base de cálculo do ISS (valorServico - deducoes).
   */
  public BigDecimal getBaseCalculoEfetiva() {
    if (baseCalculo != null) return baseCalculo;
    return valorServico.subtract(deducoes != null ? deducoes : BigDecimal.ZERO);
  }

  /**
   * Calcula o valor do ISS com base na alíquota, se não informado explicitamente.
   */
  public BigDecimal getValorIssEfetivo() {
    if (valorIss != null) return valorIss;
    if (aliquotaIss == null) return BigDecimal.ZERO;
    return getBaseCalculoEfetiva().multiply(aliquotaIss).setScale(2, java.math.RoundingMode.HALF_UP);
  }

  /**
   * Calcula o valor líquido da nota (valorServico - tributos retidos).
   */
  public BigDecimal getValorLiquido() {
    BigDecimal retencoes = getValorIssRetido()
        .add(valorIr != null ? valorIr : BigDecimal.ZERO)
        .add(valorPis != null ? valorPis : BigDecimal.ZERO)
        .add(valorCofins != null ? valorCofins : BigDecimal.ZERO)
        .add(valorCsll != null ? valorCsll : BigDecimal.ZERO)
        .add(valorInss != null ? valorInss : BigDecimal.ZERO);
    return valorServico.subtract(retencoes);
  }

  private BigDecimal getValorIssRetido() {
    return issRetido ? getValorIssEfetivo() : BigDecimal.ZERO;
  }
}