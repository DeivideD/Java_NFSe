package com.coffeetecnologia.nfse.xml;

import com.coffeetecnologia.nfse.exception.NfseException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Valida o XML do DPS contra os schemas XSD oficiais da RFB.
 *
 * <p>Os XSDs devem estar na pasta {@code schemas/} do projeto e no classpath
 * da aplicação. Baixe os schemas em: https://www.nfse.gov.br/
 *
 * <p>Uso:
 * <pre>{@code
 * XmlValidator validator = new XmlValidator();
 * List<String> erros = validator.validar(xmlString);
 * if (!erros.isEmpty()) {
 *     erros.forEach(System.err::println);
 * }
 * }</pre>
 */
public class XmlValidator {

  /** Caminho do XSD do DPS no classpath (coloque em src/main/resources/schemas/) */
  private static final String XSD_DPS = "/schemas/dps_v1.00.xsd";

  /** Caminho do XSD da NFS-e no classpath */
  private static final String XSD_NFSE = "/schemas/nfse_v1.00.xsd";

  private final Schema schemaDps;
  private final Schema schemaNfse;

  public XmlValidator() {
    SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    // Proteção contra XXE no schema
    try {
      sf.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    } catch (SAXException ignored) {
      // Alguns parsers não suportam, não é crítico
    }

    this.schemaDps = carregarSchema(sf, XSD_DPS);
    this.schemaNfse = carregarSchema(sf, XSD_NFSE);
  }

  /**
   * Valida o XML do DPS contra o XSD oficial.
   *
   * @param xmlDps string com o XML do DPS (com ou sem assinatura)
   * @return lista de erros de validação; lista vazia indica XML válido
   */
  public List<String> validarDps(String xmlDps) {
    return validar(schemaDps, xmlDps);
  }

  /**
   * Valida o XML da NFS-e retornada pela API.
   *
   * @param xmlNfse string com o XML da NFS-e
   * @return lista de erros de validação; lista vazia indica XML válido
   */
  public List<String> validarNfse(String xmlNfse) {
    return validar(schemaNfse, xmlNfse);
  }

  /**
   * Valida o XML e lança exceção se houver erros.
   *
   * @param xmlDps string com o XML do DPS
   * @throws NfseException se o XML for inválido, com lista de erros na mensagem
   */
  public void validarOuLancar(String xmlDps) {
    List<String> erros = validarDps(xmlDps);
    if (!erros.isEmpty()) {
      throw new NfseException("XML do DPS inválido. Erros:\n" + String.join("\n", erros));
    }
  }

  // ========================
  // Implementação interna
  // ========================

  private List<String> validar(Schema schema, String xml) {
    List<String> erros = new ArrayList<>();

    if (schema == null) {
      erros.add("Schema XSD não encontrado no classpath. Verifique a pasta schemas/.");
      return erros;
    }

    try {
      Validator validator = schema.newValidator();
      // Coleta todos os erros sem parar no primeiro
      validator.setErrorHandler(new org.xml.sax.ErrorHandler() {
        @Override
        public void warning(SAXParseException e) {
          erros.add("AVISO [linha " + e.getLineNumber() + "]: " + e.getMessage());
        }

        @Override
        public void error(SAXParseException e) {
          erros.add("ERRO [linha " + e.getLineNumber() + "]: " + e.getMessage());
        }

        @Override
        public void fatalError(SAXParseException e) {
          erros.add("ERRO FATAL [linha " + e.getLineNumber() + "]: " + e.getMessage());
        }
      });

      validator.validate(new StreamSource(new StringReader(xml)));

    } catch (SAXException | IOException e) {
      erros.add("Exceção durante validação: " + e.getMessage());
    }

    return erros;
  }

  private Schema carregarSchema(SchemaFactory sf, String path) {
    URL resource = getClass().getResource(path);
    if (resource == null) {
      // Schema não encontrado — validação será pulada com aviso
      return null;
    }
    try (InputStream is = resource.openStream()) {
      return sf.newSchema(new StreamSource(is));
    } catch (Exception e) {
      throw new NfseException("Erro ao carregar schema XSD: " + path, e);
    }
  }
}