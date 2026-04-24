# Java NFS-e 🇧🇷

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Biblioteca Java para comunicação com a **API Nacional de NFS-e** (Nota Fiscal de Serviço Eletrônica) da Receita Federal do Brasil.

## Funcionalidades

- ✅ **Emissão de NFS-e** via DPS (Documento Particular de Serviço)
- ✅ **Sincronização de DF-e** — busca NFS-e emitidas e recebidas pelo CNPJ
- ✅ **Consulta de NFS-e** pela chave de acesso
- ✅ **Autenticação mTLS** com certificado digital **A1** (`.pfx`) e **A3** (token/smartcard)
- ✅ **Geração de XML** conforme schema XSD v1.01 oficial da RFB
- ✅ **Assinatura digital** XMLDSig RSA-SHA256
- ✅ **Compressão GZip + Base64** conforme exigido pela API
- ✅ Suporte aos ambientes de **Produção Restrita** (homologação) e **Produção**

## APIs implementadas

| API | URL | Função |
|---|---|---|
| **Sefin Nacional** | `sefin.producaorestrita.nfse.gov.br/SefinNacional` | Emissão de NFS-e |
| **ADN Contribuintes** | `adn.nfse.gov.br/contribuintes` | Sincronização de DF-e |

## Instalação

```xml
<dependency>
    <groupId>io.github.deivided</groupId>
    <artifactId>java-nfse</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Uso rápido

### Sincronizar NFS-e recebidas

```java
NfseClient client = NfseClient.builder()
    .certificado(CertificadoDigital.fromPfx("/cert.pfx", "senha"))
    .ambiente(Ambiente.PRODUCAO)
    .build();

// Busca lote de documentos a partir do NSU 1
DfeResponse response = client.buscarLote(1L);

response.getLoteDFe().forEach(doc -> {
    System.out.println("NSU: " + doc.getNsu());
    System.out.println("Tipo: " + doc.getTipoDocumento());
    System.out.println("Chave: " + doc.getChaveAcesso());

    // Descomprime o XML
    String xml = client.descomprimirXml(doc.getArquivoXml());
    System.out.println(xml);
});

// Próxima página
long proximoNsu = response.getLoteDFe().stream()
    .mapToLong(DfeResponse.Documento::getNsu)
    .max().orElse(0) + 1;

DfeResponse pagina2 = client.buscarLote(proximoNsu);
```

### Emitir NFS-e

```java
NfseClient client = NfseClient.builder()
    .certificado(CertificadoDigital.fromPfx("/cert.pfx", "senha"))
    .ambiente(Ambiente.PRODUCAO_RESTRITA)
    .validarXml(false)
    .build();

Dps dps = Dps.builder()
    .numero("1")
    .serie("1")
    .prestador(Prestador.builder()
        .cnpj("00000000000191")
        .codigoMunicipio("2304400") // Fortaleza-CE
        .build())
    .tomador(Tomador.builder()
        .cnpj("00000000000100")
        .nome("Empresa Tomadora LTDA")
        .build())
    .servico(Servico.builder()
        .codigoServico("010101") // cTribNac 6 dígitos
        .descricao("Análise e desenvolvimento de sistemas")
        .build())
    .valores(Valores.builder()
        .valorServico(new BigDecimal("1500.00"))
        .aliquotaIss(new BigDecimal("0.02"))
        .build())
    .codigoMunicipio("2304400")
    .build();

Nfse nfse = client.emitir(dps);
System.out.println("Chave: " + nfse.getChaveAcesso());
```

### Certificado A3 (token/smartcard)

```java
CertificadoDigital cert = CertificadoDigital.fromA3(
    "/usr/lib/libpkcs11.so",
    "pin"
);
```

## Estrutura do projeto

```
src/main/java/com/coffeetecnologia/nfse/
├── NfseClient.java                  # Entrypoint principal
├── auth/
│   ├── CertificadoDigital.java      # A1 e A3
│   └── TokenManager.java
├── model/
│   ├── dps/                         # DPS, Prestador, Tomador, Serviço, Valores
│   └── nfse/                        # Nfse, SituacaoNfse
├── xml/
│   ├── XmlBuilder.java              # Geração do XML DPS (XSD v1.01)
│   ├── XmlSigner.java               # Assinatura XMLDSig RSA-SHA256
│   └── XmlValidator.java            # Validação contra XSD
├── api/
│   ├── NfseApiClient.java           # Sefin Nacional (emissão)
│   ├── DistribuicaoApiClient.java   # ADN Contribuintes (sincronização)
│   ├── request/                     # DTOs de request
│   └── response/                    # DTOs de response
└── config/
    ├── Ambiente.java                # PRODUCAO_RESTRITA / PRODUCAO
    └── NfseConfig.java              # Configuração central
```

## Requisitos técnicos

Conforme documentação oficial da RFB:

- **Protocolo:** TLS 1.2+ com autenticação mútua (mTLS)
- **Certificado:** ICP-Brasil A1 ou A3, CNPJ ou CPF
- **Formato dos documentos:** XML 1.0 com assinatura XMLDSIG
- **Compactação:** GZip com representação base64binary
- **Formato de mensagens:** JSON

## Referências

- [Portal Nacional NFS-e](https://www.gov.br/nfse)
- [Swagger Sefin Nacional (Produção Restrita)](https://sefin.producaorestrita.nfse.gov.br/SefinNacional/swagger/ui/index)
- [Swagger ADN Contribuintes](https://adn.producaorestrita.nfse.gov.br/contribuintes/docs/index.html)
- [Schemas XSD v1.01](https://www.gov.br/nfse/pt-br/biblioteca/documentacao-tecnica/documentacao-atual)
- Inspirado em [Java_NFe](https://github.com/Samuel-Oliveira/Java_NFe)

## Contribuindo

PRs são bem-vindos! Veja [CONTRIBUTING.md](CONTRIBUTING.md).

## Licença

[MIT](LICENSE)
