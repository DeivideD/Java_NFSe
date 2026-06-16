package com.coffeetecnologia.nfse.xml;

import com.coffeetecnologia.nfse.exception.NfseException;
import com.coffeetecnologia.nfse.model.evento.PedidoEvento;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Constrói o XML do Pedido de Registro de Evento conforme XSD v1.01 da RFB.
 *
 * Estrutura gerada:
 * <pre>{@code
 * <pedRegEvento versao="1.01" xmlns="http://www.sped.fazenda.gov.br/nfse">
 *   <infPedReg Id="PRE{chave50}101101">
 *     <tpAmb>2</tpAmb>
 *     <verAplic>java-nfse-1.1.0</verAplic>
 *     <dhEvento>2026-06-15T23:00:00-03:00</dhEvento>
 *     <CNPJAutor>14digitsCNPJ</CNPJAutor>
 *     <chNFSe>50digitsChave</chNFSe>
 *     <e101101>
 *       <xDesc>Cancelamento de NFS-e</xDesc>
 *       <cMotivo>1</cMotivo>
 *       <xMotivo>Texto do motivo</xMotivo>
 *     </e101101>
 *   </infPedReg>
 * </pedRegEvento>
 * }</pre>
 */
public class XmlEventoBuilder {

  private static final String NS = "http://www.sped.fazenda.gov.br/nfse";
  private static final String VERSAO = "1.01";
  private static final String VER_APLIC = "java-nfse-1.1.0";
  private static final ZoneId ZONE_BR = ZoneId.of("America/Sao_Paulo");
  private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

  private final DocumentBuilder documentBuilder;

  public XmlEventoBuilder() {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      this.documentBuilder = factory.newDocumentBuilder();
    } catch (Exception e) {
      throw new NfseException("Erro ao inicializar XmlEventoBuilder.", e);
    }
  }

  /**
   * Gera o Document DOM do pedido de registro de evento.
   */
  public Document buildDocument(PedidoEvento pedido) {
    try {
      Document doc = documentBuilder.newDocument();
      Element root = buildPedRegEvento(doc, pedido);
      doc.appendChild(root);
      return doc;
    } catch (NfseException e) {
      throw e;
    } catch (Exception e) {
      throw new NfseException("Erro ao gerar Document do evento.", e);
    }
  }

  public String toXmlString(Document doc) {
    try {
      TransformerFactory tf = TransformerFactory.newInstance();
      tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

      Transformer t = tf.newTransformer();
      t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      t.setOutputProperty(OutputKeys.INDENT, "no");

      StringWriter sw = new StringWriter();
      t.transform(new DOMSource(doc), new StreamResult(sw));
      return sw.toString();
    } catch (Exception e) {
      throw new NfseException("Erro ao serializar XML do evento.", e);
    }
  }

  // ========================
  // <pedRegEvento versao="1.01">
  // ========================

  private Element buildPedRegEvento(Document doc, PedidoEvento pedido) {
    Element el = doc.createElementNS(NS, "pedRegEvento");
    el.setAttribute("versao", VERSAO);
    el.appendChild(buildInfPedReg(doc, pedido));
    return el;
  }

  // ========================
  // <infPedReg Id="PRE...">
  // ========================

  private Element buildInfPedReg(Document doc, PedidoEvento pedido) {
    Element el = doc.createElementNS(NS, "infPedReg");
    el.setAttribute("Id", gerarId(pedido));

    addEl(doc, el, "tpAmb", "2");
    addEl(doc, el, "verAplic", VER_APLIC);

    ZonedDateTime agora = ZonedDateTime.now(ZONE_BR);
    addEl(doc, el, "dhEvento", agora.format(FMT_DATETIME));

    if (pedido.isAutorCnpj()) {
      addEl(doc, el, "CNPJAutor", pedido.getCnpjAutor());
    } else {
      addEl(doc, el, "CPFAutor", pedido.getCpfAutor());
    }

    addEl(doc, el, "chNFSe", pedido.getChaveNfse());

    el.appendChild(buildDetEvento(doc, pedido));

    return el;
  }

  // ========================
  // <e101101> — Cancelamento
  // ========================

  private Element buildDetEvento(Document doc, PedidoEvento pedido) {
    Element el = doc.createElementNS(NS, "e101101");
    addEl(doc, el, "xDesc", "Cancelamento de NFS-e");
    addEl(doc, el, "cMotivo", pedido.getCMotivo());
    if (pedido.getXMotivo() != null && !pedido.getXMotivo().isBlank()) {
      addEl(doc, el, "xMotivo", pedido.getXMotivo());
    }
    return el;
  }

  // ========================
  // Geração do Id
  // ========================

  /**
   * Id format: "PRE" + chaveNfse(50) + "101101" = 59 chars.
   */
  public String gerarId(PedidoEvento pedido) {
    return "PRE" + pedido.getChaveNfse() + "101101";
  }

  // ========================
  // Utilitários DOM
  // ========================

  private void addEl(Document doc, Element parent, String tag, String value) {
    if (value == null || value.isBlank()) return;
    Element el = doc.createElementNS(NS, tag);
    el.setTextContent(value);
    parent.appendChild(el);
  }
}
