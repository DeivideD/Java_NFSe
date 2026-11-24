package com.coffeetecnologia.nfse.model.dps;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * PIS/COFINS de apuração própria do prestador ({@code tribFed/piscofins}): débito da operação, não retenção
 * (NT SE/CGNFS-e 007). Esses valores saem da base do IBS/CBS; a Sefin confere {@code vPis}/{@code vCofins}
 * com arredondamento bancário e tolerância de R$ 0,01.
 */
@Getter
@Builder
public class ApuracaoPisCofins {

  /** Base de cálculo do PIS/COFINS ({@code vBCPisCofins}). */
  private final BigDecimal baseCalculo;

  /** Alíquota do PIS em % ({@code pAliqPis}, ex.: 0.65 ou 1.65). */
  private final BigDecimal aliquotaPis;

  /** Alíquota da COFINS em % ({@code pAliqCofins}, ex.: 3.00 ou 7.60). */
  private final BigDecimal aliquotaCofins;

  /** Débito de PIS ({@code vPis}). */
  private final BigDecimal valorPis;

  /** Débito de COFINS ({@code vCofins}). */
  private final BigDecimal valorCofins;
}
