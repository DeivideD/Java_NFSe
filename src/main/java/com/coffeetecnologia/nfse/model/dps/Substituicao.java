package com.coffeetecnologia.nfse.model.dps;

import lombok.Builder;
import lombok.Getter;

/**
 * Dados de substituição de NFS-e conforme XSD TCSubstituicao.
 *
 * <p>Utilizado quando a emissão de um DPS visa substituir uma NFS-e já existente.
 * O campo {@code chSubstda} deve conter a chave de acesso (50 dígitos) da nota a ser substituída.
 */
@Getter
@Builder
public class Substituicao {

  /** Chave de acesso da NFS-e a ser substituída (50 dígitos). */
  private final String chSubstda;

  /**
   * Código do motivo da substituição.
   * Valores: "01"=Erro na emissão, "02"=Serviço não prestado,
   * "03"=Emissão em duplicidade, "04"=Fraude, "05"=Outros, "99"=Não especificado.
   */
  private final String cMotivo;

  /** Descrição do motivo da substituição (mínimo 15, máximo 255 caracteres). Opcional. */
  private final String xMotivo;
}
