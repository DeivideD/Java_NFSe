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
 * Resposta da API ao emitir uma NFS-e (POST /api/v1/dps).
 *
 * <p>Os nomes dos campos seguem a especificação da API Nacional NFS-e.
 * Campos desconhecidos são ignorados para compatibilidade futura.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmitirNfseResponse {

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

  /**
   * Converte o DTO de resposta para o modelo de domínio {@link Nfse}.
   */
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