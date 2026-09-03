package com.coffeetecnologia.nfse.model.dps;

import lombok.Builder;
import lombok.Getter;

/**
 * Grupo de informações declaradas pelo emitente referentes ao IBS e à CBS (IBSCBS).
 * Caminho XML: infDPS/IBSCBS/ — irmão de {@code <serv>} e {@code <valores>} dentro de infDPS.
 *
 * <p>Este grupo é opcional (OCOR 0-1). Quando informado, os campos {@code cIndOp},
 * {@code indDest} e {@code gIbscbs} são obrigatórios. O campo {@code dest} é obrigatório
 * condicionalmente: requerido quando {@code indDest = "1"}.
 *
 * <p>A validação de exigibilidade do grupo pelo Ambiente Nacional está suspensa em 2026
 * conforme NT 004 SE/CGNFS-e v2.0 (10/dez/2025). A estrutura está implementada para
 * conformidade futura (obrigatoriedade prevista a partir de 2027).
 *
 * <p>Uso:
 * <pre>{@code
 * Ibscbs ibscbs = Ibscbs.builder()
 *     .cIndOp("000001")
 *     .indDest("0")
 *     .gIbscbs(GIbscbs.builder()
 *         .cst("101")
 *         .cClassTrib("010100")
 *         .build())
 *     .build();
 * }</pre>
 */
@Getter
@Builder
public class Ibscbs {

  /**
   * Indicador da finalidade da emissão de NFS-e.
   * Atualmente o XSD (v1.01, jul/2026) aceita apenas {@code "0"} (NFS-e regular).
   * Valores {@code "1"} (crédito) e {@code "2"} (débito) estão previstos em NT futura.
   */
  @Builder.Default
  private final String finNFSe = "0";

  /** Código indicador da operação de fornecimento (Anexo VII da NT 004). Formato: 6 dígitos [0-9]{6}. */
  private final String cIndOp;

  /**
   * Indicador do destinatário.
   * {@code "0"} — destinatário é o próprio tomador identificado na NFS-e.
   * {@code "1"} — destinatário é pessoa diferente do tomador (preencher {@link #dest}).
   */
  private final String indDest;

  /**
   * Dados do destinatário. Obrigatório quando {@code indDest = "1"}.
   * Omitir quando {@code indDest = "0"}.
   */
  private final Dest dest;

  /**
   * Informações de CST e cClassTrib do IBS/CBS.
   * Obrigatório sempre que o grupo IBSCBS for informado.
   */
  private final GIbscbs gIbscbs;
}
