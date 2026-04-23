package com.coffeetecnologia.nfse.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resposta do endpoint POST /adn/DFe conforme Swagger oficial da API Nacional NFS-e.
 *
 * <p>Schema: {@code RecepcaoResponseLote}
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecepcaoResponse {

  /** Lista de documentos processados no lote. */
  @JsonProperty("Lote")
  private List<Documento> lote;

  /** Tipo do ambiente: PRODUCAO ou HOMOLOGACAO. */
  @JsonProperty("TipoAmbiente")
  private String tipoAmbiente;

  @JsonProperty("VersaoAplicativo")
  private String versaoAplicativo;

  @JsonProperty("DataHoraProcessamento")
  private OffsetDateTime dataHoraProcessamento;

  /**
   * Retorna o primeiro documento do lote (caso mais comum — envio de 1 DPS).
   */
  public Documento primeiroDps() {
    if (lote == null || lote.isEmpty()) {
      throw new com.coffeetecnologia.nfse.exception.NfseException(
          "Resposta da API não contém documentos no lote."
      );
    }
    return lote.get(0);
  }

  /**
   * Documento processado — schema: {@code RecepcaoResponseDocumento}
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Documento {

    /** Chave de acesso da NFS-e gerada (44 dígitos). */
    @JsonProperty("ChaveAcesso")
    private String chaveAcesso;

    /** NSU - Número Sequencial de Busca. */
    @JsonProperty("NsuRecepcao")
    private String nsuRecepcao;

    /** Status do processamento. */
    @JsonProperty("StatusProcessamento")
    private String statusProcessamento;

    /** Alertas (não impedem a emissão, mas devem ser registrados). */
    @JsonProperty("Alertas")
    private List<Mensagem> alertas;

    /** Erros de validação (impedem a emissão). */
    @JsonProperty("Erros")
    private List<Mensagem> erros;

    public boolean isSuccess() {
      return erros == null || erros.isEmpty();
    }

    public String getErrosFormatados() {
      if (erros == null) return "";
      return erros.stream()
          .map(e -> "[" + e.getCodigo() + "] " + e.getDescricao()
              + (e.getComplemento() != null ? " — " + e.getComplemento() : ""))
          .reduce("", (a, b) -> a + "\n" + b);
    }
  }

  /**
   * Mensagem de erro ou alerta — schema: {@code MensagemProcessamento}
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Mensagem {

    @JsonProperty("Codigo")
    private String codigo;

    @JsonProperty("Descricao")
    private String descricao;

    @JsonProperty("Complemento")
    private String complemento;
  }
}