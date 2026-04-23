package com.coffeetecnologia.nfse.xml;

import com.coffeetecnologia.nfse.model.dps.Valores;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Valores - Cálculos financeiros do DPS")
class ValoresTest {

  @Test
  @DisplayName("Deve calcular base de cálculo sem deduções")
  void deveCalcularBaseCalculoSemDeducoes() {
    Valores valores = Valores.builder()
        .valorServico(new BigDecimal("1000.00"))
        .build();

    assertEquals(new BigDecimal("1000.00"), valores.getBaseCalculoEfetiva());
  }

  @Test
  @DisplayName("Deve calcular base de cálculo com deduções")
  void deveCalcularBaseCalculoComDeducoes() {
    Valores valores = Valores.builder()
        .valorServico(new BigDecimal("1000.00"))
        .deducoes(new BigDecimal("200.00"))
        .build();

    assertEquals(new BigDecimal("800.00"), valores.getBaseCalculoEfetiva());
  }

  @Test
  @DisplayName("Deve calcular ISS com alíquota de 5%")
  void deveCalcularIssComAliquota5Porcento() {
    Valores valores = Valores.builder()
        .valorServico(new BigDecimal("1000.00"))
        .aliquotaIss(new BigDecimal("0.05"))
        .build();

    assertEquals(new BigDecimal("50.00"), valores.getValorIssEfetivo());
  }

  @Test
  @DisplayName("Deve calcular ISS com deduções e alíquota")
  void deveCalcularIssComDeducoesEAliquota() {
    Valores valores = Valores.builder()
        .valorServico(new BigDecimal("1000.00"))
        .deducoes(new BigDecimal("200.00"))
        .aliquotaIss(new BigDecimal("0.05"))
        .build();

    // (1000 - 200) * 0.05 = 40.00
    assertEquals(new BigDecimal("40.00"), valores.getValorIssEfetivo());
  }

  @Test
  @DisplayName("Deve retornar zero de ISS quando alíquota não informada")
  void deveRetornarZeroIssQuandoAliquotaNaoInformada() {
    Valores valores = Valores.builder()
        .valorServico(new BigDecimal("1000.00"))
        .build();

    assertEquals(BigDecimal.ZERO, valores.getValorIssEfetivo());
  }

  @Test
  @DisplayName("Deve calcular valor líquido com ISS retido")
  void deveCalcularValorLiquidoComIssRetido() {
    Valores valores = Valores.builder()
        .valorServico(new BigDecimal("1000.00"))
        .aliquotaIss(new BigDecimal("0.05"))
        .issRetido(true)
        .build();

    // 1000 - 50 (ISS retido) = 950
    assertEquals(new BigDecimal("950.00"), valores.getValorLiquido());
  }

  @Test
  @DisplayName("Deve calcular valor líquido com múltiplas retenções")
  void deveCalcularValorLiquidoComMultiplasRetencoes() {
    Valores valores = Valores.builder()
        .valorServico(new BigDecimal("1000.00"))
        .aliquotaIss(new BigDecimal("0.05"))
        .issRetido(true)
        .valorIr(new BigDecimal("15.00"))
        .valorInss(new BigDecimal("11.00"))
        .build();

    // 1000 - 50 (ISS) - 15 (IR) - 11 (INSS) = 924.00
    assertEquals(new BigDecimal("924.00"), valores.getValorLiquido());
  }

  @Test
  @DisplayName("Deve priorizar ISS informado explicitamente sobre o calculado")
  void devePriorizarIssInformadoExplicitamente() {
    Valores valores = Valores.builder()
        .valorServico(new BigDecimal("1000.00"))
        .aliquotaIss(new BigDecimal("0.05"))
        .valorIss(new BigDecimal("60.00")) // Sobrescreve o calculado (50.00)
        .build();

    assertEquals(new BigDecimal("60.00"), valores.getValorIssEfetivo());
  }
}