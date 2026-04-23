package com.coffeetecnologia.nfse.xml;

import com.coffeetecnologia.nfse.auth.CertificadoDigital;
import com.coffeetecnologia.nfse.exception.AssinaturaException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

/**
 * Assina digitalmente o XML do DPS utilizando o padrão XMLDSig (XML Digital Signature).
 *
 * <p>Utiliza o algoritmo RSA-SHA256 para assinatura e C14N (Canonical XML 1.0)
 * para canonicalização, conforme exigido pela API Nacional NFS-e.
 *
 * <p>A assinatura é inserida como filho do elemento raiz {@code <DPS>} com
 * referência ao atributo {@code Id} do documento.
 */
public class XmlSigner {

  private final XMLSignatureFactory signatureFactory;

  public XmlSigner() {
    this.signatureFactory = XMLSignatureFactory.getInstance("DOM");
  }

  /**
   * Assina o Document DOM do DPS e retorna o XML assinado como String.
   *
   * @param document    documento DOM gerado pelo {@link XmlBuilder}
   * @param certificado certificado digital do prestador
   * @return XML do DPS com assinatura digital embutida
   */
  public String assinar(Document document, CertificadoDigital certificado) {
    try {
      validarCertificado(certificado);

      PrivateKey privateKey = certificado.getPrivateKey();
      X509Certificate cert = certificado.getCertificado();

      // Obtém o Id do elemento raiz para referência na assinatura
      String id = resolverIdElementoRaiz(document);

      // 1. Define a referência ao elemento assinado (URI="#Id_do_DPS")
      Reference reference = criarReferencia(id);

      // 2. Define o método de assinatura RSA-SHA256
      SignedInfo signedInfo = criarSignedInfo(reference);

      // 3. Cria o KeyInfo com o certificado X.509 (obrigatório pela RFB)
      KeyInfo keyInfo = criarKeyInfo(cert);

      // 4. Localiza onde inserir a assinatura no DOM
      Element elementoRaiz = document.getDocumentElement();
      DOMSignContext signContext = new DOMSignContext(privateKey, elementoRaiz);

      // 5. Cria e executa a assinatura
      XMLSignature signature = signatureFactory.newXMLSignature(signedInfo, keyInfo);
      signature.sign(signContext);

      return documentToString(document);

    } catch (AssinaturaException e) {
      throw e;
    } catch (Exception e) {
      throw new AssinaturaException("Erro ao assinar o XML do DPS.", e);
    }
  }

  // ========================
  // Construção da assinatura
  // ========================

  private Reference criarReferencia(String id) throws Exception {
    // Transform 1: Enveloped Signature (remove a própria assinatura antes de calcular o hash)
    Transform envelopedTransform = signatureFactory.newTransform(
        Transform.ENVELOPED,
        (TransformParameterSpec) null
    );

    // Transform 2: C14N (canonicalização sem comentários)
    Transform c14nTransform = signatureFactory.newTransform(
        CanonicalizationMethod.INCLUSIVE,
        (TransformParameterSpec) null
    );

    DigestMethod digestMethod = signatureFactory.newDigestMethod(
        DigestMethod.SHA256, null
    );

    return signatureFactory.newReference(
        "#" + id,
        digestMethod,
        List.of(envelopedTransform, c14nTransform),
        null,
        null
    );
  }

  private SignedInfo criarSignedInfo(Reference reference) throws Exception {
    CanonicalizationMethod c14n = signatureFactory.newCanonicalizationMethod(
        CanonicalizationMethod.INCLUSIVE,
        (C14NMethodParameterSpec) null
    );

    SignatureMethod signatureMethod = signatureFactory.newSignatureMethod(
        SignatureMethod.RSA_SHA256, null
    );

    return signatureFactory.newSignedInfo(c14n, signatureMethod, List.of(reference));
  }

  private KeyInfo criarKeyInfo(X509Certificate cert) {
    KeyInfoFactory kif = signatureFactory.getKeyInfoFactory();

    X509Data x509Data = kif.newX509Data(Collections.singletonList(cert));

    return kif.newKeyInfo(Collections.singletonList(x509Data));
  }

  // ========================
  // Utilitários
  // ========================

  private String resolverIdElementoRaiz(Document document) {
    // O Id fica em <infDPS>, filho do elemento raiz <DPS>
    NodeList nodes = document.getElementsByTagNameNS(
        "http://www.sped.fazenda.gov.br/nfse", "infDPS"
    );

    if (nodes.getLength() == 0) {
      throw new AssinaturaException(
          "Elemento <infDPS> não encontrado no XML do DPS."
      );
    }

    Element infDps = (Element) nodes.item(0);
    String id = infDps.getAttribute("Id");

    if (id == null || id.isBlank()) {
      throw new AssinaturaException(
          "Elemento <infDPS> não possui atributo 'Id'. " +
              "A assinatura XMLDSig requer um Id para referenciar o elemento assinado."
      );
    }

    // Registra como tipo ID para o XMLDSig resolver a referência URI="#DPS..."
    infDps.setIdAttribute("Id", true);
    return id;
  }

  private void validarCertificado(CertificadoDigital certificado) {
    if (certificado.isVencido()) {
      throw new AssinaturaException(
          "Certificado digital vencido em: " + certificado.getValidade() +
              ". Renove o certificado antes de emitir NFS-e."
      );
    }
  }

  private String documentToString(Document document) {
    try {
      TransformerFactory tf = TransformerFactory.newInstance();
      tf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
      tf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

      Transformer transformer = tf.newTransformer();
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.setOutputProperty(OutputKeys.INDENT, "no");

      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(document), new StreamResult(writer));
      return writer.toString();
    } catch (Exception e) {
      throw new AssinaturaException("Erro ao serializar XML assinado.", e);
    }
  }
}