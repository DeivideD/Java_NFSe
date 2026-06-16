package com.coffeetecnologia.nfse.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resposta do POST /nfse/{chaveAcesso}/eventos — Sefin Nacional.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventoResponse {

  @JsonProperty("tipoAmbiente")
  private Integer tipoAmbiente;

  @JsonProperty("versaoAplicativo")
  private String versaoAplicativo;

  @JsonProperty("dataHoraProcessamento")
  private OffsetDateTime dataHoraProcessamento;

  @JsonProperty("protocolo")
  private String protocolo;

  @JsonProperty("chNFSe")
  private String chNFSe;

  @JsonProperty("cStat")
  private String cstat;

  @JsonProperty("xMotivo")
  private String xMotivo;

  @JsonProperty("erros")
  private List<NfsePostResponse.MensagemProcessamento> erros;

  public boolean isSuccess() {
    return erros == null || erros.isEmpty();
  }

  public String getErrosFormatados() {
    if (erros == null || erros.isEmpty()) return "";
    StringBuilder sb = new StringBuilder();
    for (NfsePostResponse.MensagemProcessamento e : erros) {
      sb.append("[").append(e.getCodigo()).append("] ")
          .append(e.getDescricao());
      if (e.getComplemento() != null && !e.getComplemento().isBlank()) {
        sb.append(" — ").append(e.getComplemento());
      }
      sb.append("\n");
    }
    return sb.toString().trim();
  }
}
