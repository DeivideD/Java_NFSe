package com.coffeetecnologia.nfse.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resposta de SUCESSO do POST /nfse — Sefin Nacional.
 *
 * Conforme Swagger NFSePostResponseSucesso:
 * {
 *   "tipoAmbiente": 2,
 *   "versaoAplicativo": "...",
 *   "dataHoraProcessamento": "...",
 *   "idDps": "DPS...",
 *   "chaveAcesso": "...",
 *   "nfseXmlGZipB64": "...",
 *   "alertas": []
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NfsePostResponse {

  /** 1=Produção, 2=Homologação */
  @JsonProperty("tipoAmbiente")
  private Integer tipoAmbiente;

  @JsonProperty("versaoAplicativo")
  private String versaoAplicativo;

  @JsonProperty("dataHoraProcessamento")
  private OffsetDateTime dataHoraProcessamento;

  /** Identificador do DPS processado */
  @JsonProperty("idDps")
  private String idDps;

  /** Chave de acesso da NFS-e gerada (50 dígitos) */
  @JsonProperty("chaveAcesso")
  private String chaveAcesso;

  /** NFS-e em formato XML compactado GZip + Base64 */
  @JsonProperty("nfseXmlGZipB64")
  private String nfseXmlGZipB64;

  /** Alertas (não impedem a emissão) */
  @JsonProperty("alertas")
  private List<MensagemProcessamento> alertas;

  // ========================
  // Resposta de ERRO
  // ========================

  /** Id do DPS — presente apenas nas respostas de erro */
  @JsonProperty("idDPS")
  private String idDpsErro;

  /** Erros de processamento — presente apenas nas respostas de erro */
  @JsonProperty("erros")
  private List<MensagemProcessamento> erros;

  public boolean isSuccess() {
    return erros == null || erros.isEmpty();
  }

  public String getErrosFormatados() {
    if (erros == null || erros.isEmpty()) return "";
    StringBuilder sb = new StringBuilder();
    for (MensagemProcessamento e : erros) {
      sb.append("[").append(e.getCodigo()).append("] ")
          .append(e.getDescricao());
      if (e.getComplemento() != null && !e.getComplemento().isBlank()) {
        sb.append(" — ").append(e.getComplemento());
      }
      sb.append("\n");
    }
    return sb.toString().trim();
  }

  /**
   * Mensagem de erro/alerta — conforme Swagger MensagemProcessamento.
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class MensagemProcessamento {

    @JsonProperty("Codigo")
    private String codigo;

    @JsonProperty("Descricao")
    private String descricao;

    @JsonProperty("Complemento")
    private String complemento;
  }
}