package com.coffeetecnologia.nfse.model.evento;

import lombok.Builder;
import lombok.Getter;

/**
 * Pedido de Registro de Evento para cancelamento de NFS-e.
 *
 * <p>Representa os dados necessários para gerar o XML {@code pedRegEvento}
 * conforme especificação AnexoII-LeiautesRN_Eventos-SNNFSe da RFB.
 */
@Getter
@Builder
public class PedidoEvento {

  /** Chave de acesso da NFS-e (50 dígitos). */
  private final String chaveNfse;

  /** CNPJ do autor do evento (prestador, normalmente). */
  private final String cnpjAutor;

  /** CPF do autor do evento (alternativo ao CNPJ). */
  private final String cpfAutor;

  /**
   * Código do motivo do cancelamento.
   * 1=Erro na Emissão, 2=Serviço não Prestado, 9=Outros.
   */
  private final String cMotivo;

  /** Descrição do motivo (mínimo 15, máximo 255 caracteres). */
  private final String xMotivo;

  /** Retorna {@code true} se o autor for identificado por CNPJ. */
  public boolean isAutorCnpj() {
    return cnpjAutor != null && !cnpjAutor.isBlank();
  }
}
