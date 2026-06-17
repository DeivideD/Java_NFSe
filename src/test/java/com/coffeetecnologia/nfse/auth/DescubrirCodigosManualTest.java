package com.coffeetecnologia.nfse.auth;

import com.coffeetecnologia.nfse.NfseClient;
import com.coffeetecnologia.nfse.config.Ambiente;
import com.coffeetecnologia.nfse.exception.NfseException;
import com.coffeetecnologia.nfse.model.dps.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Varre códigos cTribNac para descobrir quais Fortaleza (2304400) aceita.
 */
public class DescubrirCodigosManualTest {

  public static void main(String[] args) {
    String caminho = "/home/deivide/Downloads/certificado/coffee.pfx";
    String senha   = "123456";

    List<String> codigos = List.of(
        // Grupo 02 — Pesquisa
        "020101", "020201",
        // Grupo 03 — Cessão de direito de uso
        "030101",
        // Grupo 04 — Saúde
        "040101", "040201", "040301", "040401", "040501", "040601", "040701",
        // Grupo 05 — Medicina veterinária
        "050101",
        // Grupo 06 — Cuidados pessoais
        "060101", "060201",
        // Grupo 07 — Engenharia e construção
        "070101", "070201", "070301", "070401", "070501",
        // Grupo 08 — Educação
        "080101", "080201",
        // Grupo 09 — Hospedagem
        "090101", "090201",
        // Grupo 10 — Intermediação
        "100101", "100201",
        // Grupo 11 — Guarda e estacionamento
        "110101",
        // Grupo 12 — Diversão
        "120101", "120201",
        // Grupo 13 — Fonografia/Fotografia
        "130101",
        // Grupo 14 — Manutenção
        "140101", "140201",
        // Grupo 15 — Financeiro
        "150101", "150201",
        // Grupo 16 — Transporte
        "160101", "160201",
        // Grupo 17 — Apoio
        "170101", "170201",
        // Grupo 18 — Regulação
        "180101",
        // Grupo 19 — Distribuição
        "190101",
        // Grupo 20 — Portuário
        "200101",
        // Grupo 21 — Aeroportuário
        "210101",
        // Grupo 22 — Ferroviário
        "220101",
        // Grupo 23 — Rodoviário
        "230101",
        // Grupo 24 — Marítimo
        "240101",
        // Grupo 25 — Leilão
        "250101",
        // Grupo 26 — Serviços tributários
        "260101",
        // Grupo 27 — Inteligência/investigação
        "270101",
        // Grupo 28 — Músicos
        "280101",
        // Grupo 29 — Composição gráfica
        "290101",
        // Grupo 30 — Vigilância
        "300101",
        // Grupo 31 — Limpeza
        "310101",
        // Grupo 32 — Cortejo
        "320101",
        // Grupo 33 — Serviços de diversão
        "330101",
        // Grupo 34 — Alfandegário
        "340101",
        // Grupo 35 — Advogados
        "350101",
        // Grupo 36 — Veterinários
        "360101",
        // Grupo 37 — Engenheiros
        "370101",
        // Grupo 38 — Médicos
        "380101",
        // Grupo 39 — Dentistas
        "390101",
        // Grupo 40 — Contadores
        "400101"
    );

    try {
      NfseClient client = NfseClient.builder()
          .certificado(CertificadoDigital.fromPfx(caminho, senha))
          .ambiente(Ambiente.PRODUCAO_RESTRITA)
          .validarXml(false)
          .build();

      System.out.println("Varrendo códigos para Fortaleza (2304400)...\n");

      for (String codigo : codigos) {
        Dps dps = Dps.builder()
            .numero("1").serie("1")
            .prestador(Prestador.builder()
                .cnpj("42742743000187")
                .codigoMunicipio("2304400")
                .build())
            .tomador(Tomador.builder()
                .cnpj("22513364000108")
                .nome("SOWAL COMERCIO E SERVICOS LTDA")
                .build())
            .servico(Servico.builder()
                .codigoServico(codigo)
                .descricao("Servico de teste")
                .build())
            .valores(Valores.builder()
                .valorServico(new BigDecimal("100.00"))
                .aliquotaIss(new BigDecimal("0.02"))
                .build())
            .codigoMunicipio("2304400")
            .build();

        try {
          client.emitir(dps);
          System.out.println("✅ CÓDIGO ACEITO: " + codigo);
        } catch (NfseException e) {
          String msg = e.getMessage();
          if (msg != null && msg.contains("E0312")) {
            System.out.println("✗ " + codigo + " — não administrado");
          } else {
            System.out.println("? " + codigo + " — outro erro: " + msg.replace("\n", " "));
          }
        }

        Thread.sleep(300);
      }

    } catch (Exception e) {
      System.out.println("Erro geral: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
