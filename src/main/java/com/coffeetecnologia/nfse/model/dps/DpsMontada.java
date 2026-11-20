package com.coffeetecnologia.nfse.model.dps;

import com.coffeetecnologia.nfse.config.Leiaute;

import java.util.List;

/**
 * DPS montado sem assinar e sem transmitir: o XML, o leiaute usado e os erros contra o XSD oficial
 * (vazio quando o esquema passa). Regras do município (lista de serviços, alíquota) só a Sefin confere.
 */
public record DpsMontada(String idDps, String xml, Leiaute leiaute, List<String> errosEsquema) {

  public boolean esquemaValido() {
    return errosEsquema.isEmpty();
  }
}
