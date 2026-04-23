package com.coffeetecnologia.nfse.config;

/**
 * Ambientes da API Nacional NFS-e — Sefin Nacional.
 *
 * <p>URLs confirmadas pelo Swagger oficial da Sefin Nacional:
 * host: sefin.producaorestrita.nfse.gov.br
 * basePath: /SefinNacional
 */
public enum Ambiente {

  /**
   * Produção Restrita — testes e homologação.
   * Swagger: https://sefin.producaorestrita.nfse.gov.br/SefinNacional/swagger/ui/index
   */
  PRODUCAO_RESTRITA(
      "https://sefin.producaorestrita.nfse.gov.br/SefinNacional",
      2
  ),

  /**
   * Produção — emissão real de NFS-e.
   */
  PRODUCAO(
      "https://sefin.nfse.gov.br/SefinNacional",
      1
  );

  private final String baseUrl;
  private final int tpAmb;

  Ambiente(String baseUrl, int tpAmb) {
    this.baseUrl = baseUrl;
    this.tpAmb = tpAmb;
  }

  public String getBaseUrl() { return baseUrl; }

  /** Código do campo tpAmb no XML do DPS: 1=Produção, 2=Homologação */
  public int getTpAmb() { return tpAmb; }

  /** POST /nfse — emissão de NFS-e a partir do DPS */
  public String getEndpointEmissao() {
    return baseUrl + "/nfse";
  }

  /** GET /nfse/{chaveAcesso} — consulta por chave de acesso */
  public String getEndpointConsulta(String chaveAcesso) {
    return baseUrl + "/nfse/" + chaveAcesso;
  }

  /** GET /dps/{id} — consulta chave de acesso pelo id do DPS */
  public String getEndpointDps(String idDps) {
    return baseUrl + "/dps/" + idDps;
  }

  /** POST /nfse/{chaveAcesso}/eventos — registro de evento (cancelamento etc) */
  public String getEndpointEventos(String chaveAcesso) {
    return baseUrl + "/nfse/" + chaveAcesso + "/eventos";
  }

  public boolean isHomologacao() {
    return this == PRODUCAO_RESTRITA;
  }
}