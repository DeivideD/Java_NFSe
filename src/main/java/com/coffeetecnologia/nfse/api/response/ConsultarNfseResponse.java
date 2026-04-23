package com.coffeetecnologia.nfse.api.response;

import com.coffeetecnologia.nfse.model.nfse.Nfse;
import com.coffeetecnologia.nfse.model.nfse.SituacaoNfse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Resposta da API ao consultar uma NFS-e (GET /api/v1/nfse/{numero}).
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsultarNfseResponse {

  @JsonProperty("numeroNFSe")
  private String numeroNfse;

  @JsonProperty("chaveAcesso")
  private String chaveAcesso;

  @JsonProperty("codigoVerificacao")
  private String codigoVerificacao;

  @JsonProperty("dataHoraEmissao")
  private LocalDateTime dataHoraEmissao;

  @JsonProperty("situacao")
  private String situacao;

  @JsonProperty("xmlNFSe")
  private String xmlNfse;

  @JsonProperty("urlDANFSe")
  private String urlDanfse;

  @JsonProperty("numeroDPS")
  private String numeroDps;

  public Nfse toNfse() {
    return Nfse.builder()
        .numero(numeroNfse)
        .chaveAcesso(chaveAcesso)
        .codigoVerificacao(codigoVerificacao)
        .dataEmissao(dataHoraEmissao)
        .situacao(situacao != null ? SituacaoNfse.fromCodigo(situacao) : SituacaoNfse.NORMAL)
        .xmlNfse(xmlNfse)
        .urlDanfse(urlDanfse)
        .numeroDps(numeroDps)
        .build();
  }
}