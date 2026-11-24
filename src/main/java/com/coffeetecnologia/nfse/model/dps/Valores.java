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

  /** Valor retido de IRRF pelo tomador ({@code tribFed/vRetIRRF}). */
  @Builder.Default
  private final BigDecimal valorIr = BigDecimal.ZERO;

  /** PIS retido pelo tomador: vai somado com COFINS e CSLL retidas em {@code vRetCSLL} (NT 007). */
  @Builder.Default
  private final BigDecimal valorPis = BigDecimal.ZERO;

  /** COFINS retida pelo tomador: vai somada com PIS e CSLL retidos em {@code vRetCSLL} (NT 007). */
  @Builder.Default
  private final BigDecimal valorCofins = BigDecimal.ZERO;

  /** CSLL retida pelo tomador: vai somada com PIS e COFINS retidos em {@code vRetCSLL} (NT 007). */
  @Builder.Default
  private final BigDecimal valorCsll = BigDecimal.ZERO;

  /** Valor retido de INSS (contribuição previdenciária) pelo tomador ({@code tribFed/vRetCP}). */
  @Builder.Default
  private final BigDecimal valorInss = BigDecimal.ZERO;

  /**
   * CST do PIS/COFINS ({@code piscofins/CST}, 2 dígitos). Obrigatório quando há PIS, COFINS ou CSLL retidos ou
   * apuração própria: o tipo de retenção só existe dentro do grupo {@code piscofins}.
   */
  private final String cstPisCofins;

  /** PIS/COFINS de apuração própria do prestador; nulo para não informar. */
  private final ApuracaoPisCofins apuracaoPisCofins;

  /**
   * Percentual aproximado dos tributos do Simples Nacional ({@code totTrib/pTotTribSN}), em % (ex.: 6.00).
   * Obrigatório para optante ME/EPP: a Sefin recusa o {@code indTotTrib} para ME/EPP (E0712).
   */
  private final BigDecimal percentualTributosSimplesNacional;

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