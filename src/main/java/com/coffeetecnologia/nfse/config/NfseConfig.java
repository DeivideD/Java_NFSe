package com.coffeetecnologia.nfse.config;

import com.coffeetecnologia.nfse.auth.CertificadoDigital;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuração central da biblioteca Java-NFS-e.
 *
 * <p>Use o builder para construir a configuração:
 * <pre>{@code
 * NfseConfig config = NfseConfig.builder()
 *     .certificado(CertificadoDigital.fromPfx("/cert.pfx", "senha"))
 *     .ambiente(Ambiente.HOMOLOGACAO)
 *     .build();
 * }</pre>
 */
@Getter
@Builder
public class NfseConfig {

  /** Certificado digital A1 ou A3 para autenticação e assinatura. */
  private final CertificadoDigital certificado;

  /** Ambiente de comunicação (homologação ou produção). */
  @Builder.Default
  private final Ambiente ambiente = Ambiente.PRODUCAO_RESTRITA;

  /** Timeout para requisições HTTP. */
  @Builder.Default
  private final Duration timeout = Duration.ofSeconds(30);

  /** Validar o XML contra os XSDs oficiais antes de enviar. */
  @Builder.Default
  private final boolean validarXml = true;

  /** Fuso horário padrão para datas da NFS-e. */
  @Builder.Default
  private final String zoneId = "America/Sao_Paulo";

  /**
   * Valida que as configurações obrigatórias estão presentes.
   */
  public void validar() {
    Objects.requireNonNull(certificado, "Certificado digital é obrigatório.");
    Objects.requireNonNull(ambiente, "Ambiente é obrigatório.");
  }
}