package com.coffeetecnologia.nfse.api;

import com.coffeetecnologia.nfse.NfseClient;
import com.coffeetecnologia.nfse.auth.CertificadoDigital;
import com.coffeetecnologia.nfse.config.Ambiente;
import com.coffeetecnologia.nfse.model.dps.*;
import com.coffeetecnologia.nfse.model.nfse.Nfse;

import java.math.BigDecimal;

/**
 * Manual: emite uma NFS-e de teste na Produção Restrita (sem valor fiscal) para conferir o leiaute
 * vigente na Sefin. Resposta diferente de E1235 = o XML passou no esquema.
 * Variáveis: CERT_PATH, CERT_SENHA, PRESTADOR_CNPJ, PRESTADOR_IM (opcional), TOMADOR_CNPJ, TOMADOR_NOME.
 * Excluído do build (*ManualTest).
 */
public class EmissaoRestritaManualTest {

  public static void main(String[] args) {
    NfseClient client = NfseClient.builder()
        .certificado(CertificadoDigital.fromPfx(System.getenv("CERT_PATH"), System.getenv("CERT_SENHA")))
        .ambiente(Ambiente.PRODUCAO_RESTRITA)
        .build();

    Dps dps = Dps.builder()
        .serie("900")
        .numero(String.valueOf(System.currentTimeMillis() % 1_000_000_000L))
        .codigoMunicipio("2304400")
        .prestador(Prestador.builder()
            .cnpj(System.getenv("PRESTADOR_CNPJ"))
            .inscricaoMunicipal(System.getenv("PRESTADOR_IM"))
            .codigoMunicipio("2304400")
            .opcaoSimplesNacional(OpcaoSimplesNacional.ME_EPP)
            .regimeApuracaoSimplesNacional(RegimeApuracaoSimplesNacional.values()[0])
            .build())
        .tomador(Tomador.builder()
            .cnpj(System.getenv("TOMADOR_CNPJ"))
            .nome(System.getenv("TOMADOR_NOME"))
            .endereco(Tomador.Endereco.builder()
                .logradouro("Av. Frei Cirilo").numero("4531").bairro("Messejana")
                .codigoMunicipio("2304400").uf("CE").cep("60840285").build())
            .build())
        .servico(Servico.builder().codigoServico("110501").descricao("Teste de leiaute NT 009").build())
        .valores(Valores.builder()
            .valorServico(new BigDecimal("1.00"))
            .percentualTributosSimplesNacional(new BigDecimal("6.00"))
            .build())
        .build();

    try {
      Nfse nfse = client.emitir(dps);
      System.out.println("AUTORIZADA. Chave: " + nfse.getChaveAcesso());
    } catch (RuntimeException e) {
      System.out.println("RECUSADA: " + e.getMessage());
    }
  }
}
