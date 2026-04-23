package com.coffeetecnologia.nfse.model.dps;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DPS - Documento Particular de Serviço.
 *
 * <p>É o documento gerado pelo prestador que origina a NFS-e.
 * Substitui o antigo RPS (Recibo Provisório de Serviço) no padrão nacional.
 *
 * <p>Exemplo de uso:
 * <pre>{@code
 * Dps dps = Dps.builder()
 *     .prestador(Prestador.comCnpj("00.000.000/0001-00"))
 *     .tomador(Tomador.comCpf("000.000.000-00").nome("João Silva").build())
 *     .servico(Servico.builder()
 *         .descricao("Desenvolvimento de software")
 *         .codigoServico("01.07")
 *         .build())
 *     .valores(Valores.builder()
 *         .valorServico(new BigDecimal("1500.00"))
 *         .aliquotaIss(new BigDecimal("0.05"))
 *         .build())
 *     .build();
 * }</pre>
 */
@Getter
@Builder
public class Dps {

  /** Identificador único do DPS gerado pelo prestador. */
  private final String id;

  /** Versão do schema DPS (padrão: "1.00"). */
  @Builder.Default
  private final String versao = "1.00";

  /** Data de emissão do DPS. */
  @Builder.Default
  private final LocalDate dataEmissao = LocalDate.now();

  /** Série do DPS (opcional, para controle interno do prestador). */
  private final String serie;

  /** Número sequencial do DPS no prestador. */
  private final String numero;

  /** Dados do prestador de serviço (quem emite a nota). */
  private final Prestador prestador;

  /** Dados do tomador de serviço (quem contrata/recebe o serviço). */
  private final Tomador tomador;

  /** Descrição e código do serviço prestado. */
  private final Servico servico;

  /** Valores monetários (preço, tributos, deduções). */
  private final Valores valores;

  /** Local de prestação do serviço (município IBGE). */
  private final String codigoMunicipio;

  /** Natureza da operação conforme tabela da RFB. */
  @Builder.Default
  private final NaturezaOperacao naturezaOperacao = NaturezaOperacao.TRIBUTACAO_MUNICIPIO;

  /**
   * Natureza da operação da NFS-e conforme padrão nacional.
   */
  public enum NaturezaOperacao {
    TRIBUTACAO_MUNICIPIO("1"),
    TRIBUTACAO_FORA_MUNICIPIO("2"),
    ISENCAO("3"),
    IMUNE("4"),
    EXIGIBILIDADE_SUSPENSA("5");

    private final String codigo;

    NaturezaOperacao(String codigo) {
      this.codigo = codigo;
    }

    public String getCodigo() {
      return codigo;
    }
  }
}