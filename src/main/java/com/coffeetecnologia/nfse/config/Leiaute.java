package com.coffeetecnologia.nfse.config;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Leiaute da DPS (schema v1.01). A NT SE/CGNFS-e 009 v1.01 moveu {@code finNFSe} do grupo IBSCBS para
 * {@code infDPS}, pôs {@code xDescServ} depois de {@code cNBS} e tirou o {@code gIBSCBS} de
 * {@code IBSCBS/valores/trib}. A Produção Restrita valida o leiaute novo desde 20/10/2026 e a Produção
 * passa a exigi-lo em 01/12/2026; antes disso só aceita o da NT 004 v2.0.
 */
public enum Leiaute {
  NT_004,
  NT_009;

  /** Primeiro dia em que a Produção exige o leiaute da NT 009. */
  public static final LocalDate INICIO_NT_009_PRODUCAO = LocalDate.of(2026, 12, 1);

  private static final ZoneId ZONE_BR = ZoneId.of("America/Sao_Paulo");

  /** Leiaute vigente no ambiente na data informada. */
  public static Leiaute vigente(Ambiente ambiente, LocalDate hoje) {
    if (ambiente == Ambiente.PRODUCAO_RESTRITA) return NT_009;
    return hoje.isBefore(INICIO_NT_009_PRODUCAO) ? NT_004 : NT_009;
  }

  public static Leiaute vigente(Ambiente ambiente) {
    return vigente(ambiente, LocalDate.now(ZONE_BR));
  }
}
