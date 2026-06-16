package com.coffeetecnologia.nfse.auth;

import com.coffeetecnologia.nfse.NfseClient;
import com.coffeetecnologia.nfse.config.Ambiente;
import com.coffeetecnologia.nfse.model.evento.MotivoCancelamento;
import com.coffeetecnologia.nfse.model.evento.PedidoEvento;
import com.coffeetecnologia.nfse.model.evento.ResultadoEvento;

public class CancelarNfseManualTest {

  public static void main(String[] args) {

    String caminho = "CAMINHO_SEU_CERTIFICADO";
    String senha   = "SEUA_SENHA";
    String chaveAcesso = "CHAVE_ACESSO_50_DIGITOS";

    System.out.println("=== Cancelando NFS-e em Homologação ===\n");

    try {
      NfseClient client = NfseClient.builder()
          .certificado(CertificadoDigital.fromPfx(caminho, senha))
          .ambiente(Ambiente.PRODUCAO_RESTRITA)
          .build();
      System.out.println("✅ Cliente criado.");

      PedidoEvento pedido = PedidoEvento.builder()
          .chaveNfse(chaveAcesso)
          .cnpjAutor("44372492000111")
          .cMotivo(MotivoCancelamento.ERRO_EMISSAO.getCodigo())
          .xMotivo("Erro na emissão: dados incorretos do tomador.")
          .build();

      System.out.println("⏳ Enviando evento de cancelamento...");
      ResultadoEvento resultado = client.cancelar(chaveAcesso, pedido);

      System.out.println("\n✅ Evento registrado!");
      System.out.println("Protocolo : " + resultado.getProtocolo());
      System.out.println("Chave     : " + resultado.getChaveNfse());
      System.out.println("cStat     : " + resultado.getCStat());
      System.out.println("xMotivo   : " + resultado.getXMotivo());
      System.out.println("Sucesso   : " + resultado.isSucesso());

    } catch (Exception e) {
      System.out.println("❌ Erro: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
