package com.coffeetecnologia.nfse.xml;

import com.coffeetecnologia.nfse.config.Leiaute;
import com.coffeetecnologia.nfse.exception.NfseException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Valida o XML do DPS contra o XSD oficial (schema v1.01) do leiaute em uso.
 *
 * <p>Os XSDs vêm empacotados na lib, em {@code /schemas/nt004/} (NT 004 v2.0) e {@code /schemas/nt009/}
 * (NT 009 v1.01). O schema é carregado pela URL para que os {@code include}/{@code import} relativos
 * (tiposComplexos, tiposSimples, xmldsig) resolvam dentro do próprio jar.
 */
public class XmlValidator {

  private static final Map<Leiaute, String> XSD_DPS = Map.of(
      Leiaute.NT_004, "/schemas/nt004/DPS_v1.01.xsd",
      Leiaute.NT_009, "/schemas/nt009/DPS_v1.01.xsd");

  private final Map<Leiaute, Schema> schemas = new EnumMap<>(Leiaute.class);

  public XmlValidator() {
    SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    try {
      // Sem DTD externo; schemas só do próprio classpath (arquivo ou jar), nunca da rede.
      sf.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar");
    } catch (SAXException ignored) {
      // Implementação sem suporte a JAXP 1.5
    }
    XSD_DPS.forEach((leiaute, path) -> schemas.put(leiaute, carregarSchema(sf, path)));
  }

  /** Valida no leiaute da NT 004 (compatibilidade). */
  public List<String> validarDps(String xmlDps) {
    return validarDps(xmlDps, Leiaute.NT_004);
  }

  /** Erros de esquema do DPS no leiaute informado; lista vazia quando válido. */
  public List<String> validarDps(String xmlDps, Leiaute leiaute) {
    return validar(schemas.get(leiaute), xmlDps);
  }

  public void validarOuLancar(String xmlDps) {
    validarOuLancar(xmlDps, Leiaute.NT_004);
  }

  public void validarOuLancar(String xmlDps, Leiaute leiaute) {
    List<String> erros = validarDps(xmlDps, leiaute);
    if (!erros.isEmpty()) {
      throw new NfseException("XML do DPS inválido. Erros:\n" + String.join("\n", erros));
    }
  }

  private List<String> validar(Schema schema, String xml) {
    List<String> erros = new ArrayList<>();
    if (schema == null) {
      erros.add("Schema XSD não encontrado no classpath. Verifique a pasta schemas/.");
      return erros;
    }
    try {
      Validator validator = schema.newValidator();
      validator.setErrorHandler(new org.xml.sax.ErrorHandler() {
        @Override
        public void warning(SAXParseException e) {
          // avisos não reprovam o DPS
        }

        @Override
        public void error(SAXParseException e) {
          erros.add(e.getMessage());
        }

        @Override
        public void fatalError(SAXParseException e) {
          erros.add(e.getMessage());
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
      return null;
    }
    try {
      return sf.newSchema(resource);
    } catch (Exception e) {
      throw new NfseException("Erro ao carregar schema XSD: " + path, e);
    }
  }
}
