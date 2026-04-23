package com.coffeetecnologia.nfse.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resposta do endpoint GET /contribuintes/DFe/{NSU}
 * Conforme Swagger: LoteDistribuicaoNSUResponse
 *
 * Retorna documentos fiscais (NFS-e, DPS, Eventos) vinculados ao CNPJ do certificado.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DfeResponse {

  /** DOCUMENTOS_LOCALIZADOS, NENHUM_DOCUMENTO_LOCALIZADO, REJEICAO */
  @JsonProperty("StatusProcessamento")
  private String statusProcessamento;

  @JsonProperty("LoteDFe")
  private List<Documento> loteDFe;

  @JsonProperty("Alertas")
  private List<Mensagem> alertas;

  @JsonProperty("Erros")
  private List<Mensagem> erros;

  @JsonProperty("TipoAmbiente")
  private String tipoAmbiente;

  @JsonProperty("VersaoAplicativo")
  private String versaoAplicativo;

  @JsonProperty("DataHoraProcessamento")
  private OffsetDateTime dataHoraProcessamento;

  public boolean isSuccess() {
    return "DOCUMENTOS_LOCALIZADOS".equals(statusProcessamento)
        || "NENHUM_DOCUMENTO_LOCALIZADO".equals(statusProcessamento);
  }

  public boolean temDocumentos() {
    return "DOCUMENTOS_LOCALIZADOS".equals(statusProcessamento)
        && loteDFe != null && !loteDFe.isEmpty();
  }

  /**
   * Documento fiscal retornado — schema: DistribuicaoNSU
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Documento {

    /** Número Sequencial Único do documento */
    @JsonProperty("NSU")
    private Long nsu;

    /** Chave de acesso da NFS-e (50 dígitos) */
    @JsonProperty("ChaveAcesso")
    private String chaveAcesso;

    /** Tipo: DPS, NFSE, EVENTO, PEDIDO_REGISTRO_EVENTO, CNC, NENHUM */
    @JsonProperty("TipoDocumento")
    private String tipoDocumento;

    /** Tipo do evento, se TipoDocumento=EVENTO */
    @JsonProperty("TipoEvento")
    private String tipoEvento;

    /** XML do documento comprimido em GZip+Base64 */
    @JsonProperty("ArquivoXml")
    private String arquivoXml;

    @JsonProperty("DataHoraGeracao")
    private LocalDateTime dataHoraGeracao;
  }

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