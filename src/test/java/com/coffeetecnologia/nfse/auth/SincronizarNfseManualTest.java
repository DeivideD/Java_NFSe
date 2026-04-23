package com.coffeetecnologia.nfse.auth;

import com.coffeetecnologia.nfse.NfseClient;
import com.coffeetecnologia.nfse.api.response.DfeResponse;
import com.coffeetecnologia.nfse.config.Ambiente;

public class SincronizarNfseManualTest {

  public static void main(String[] args) throws Exception {

    String caminho = "CAMINHO_SEU_CERTIFICADO";
    String senha   = "SEUA_SENHA";

    NfseClient client = NfseClient.builder()
        .certificado(CertificadoDigital.fromPfx(caminho, senha))
        .ambiente(Ambiente.PRODUCAO_RESTRITA)
        .build();

    System.out.println("⏳ Buscando documentos...");
    DfeResponse response = client.buscarLote(52L);

    System.out.println("Status: " + response.getStatusProcessamento());

    if (response.temDocumentos()) {
      System.out.println("✅ " + response.getLoteDFe().size() + " documento(s):\n");
      for (DfeResponse.Documento doc : response.getLoteDFe()) {
        System.out.println("NSU: " + doc.getNsu()
            + " | Tipo: " + doc.getTipoDocumento()
            + " | Chave: " + doc.getChaveAcesso());
        DfeResponse.Documento nota = response.getLoteDFe().get(10);
        String xml = client.descomprimirXml(nota.getArquivoXml());
        System.out.println(xml);
      }
    } else {
      System.out.println("Nenhum documento.");
    }
  }
}