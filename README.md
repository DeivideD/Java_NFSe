# Java-NFS-e

Biblioteca Java para comunicação com a **API Nacional de NFS-e** (Nota Fiscal de Serviço Eletrônica) da Receita Federal do Brasil.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![CI](https://github.com/seu-usuario/java-nfse/actions/workflows/ci.yml/badge.svg)](https://github.com/seu-usuario/java-nfse/actions)

## Sobre

Implementa a nova API REST do Padrão Nacional NFS-e (SPED/RFB), com suporte a:

- ✅ Emissão de NFS-e (via DPS - Documento Particular de Serviço)
- ✅ Consulta de NFS-e por número e por prestador
- ✅ Cancelamento de NFS-e
- ✅ Autenticação com certificado digital **A1** (`.pfx`) e **A3** (token/smartcard)
- ✅ Geração e validação de XML conforme XSD oficial da RFB
- ✅ Assinatura digital XMLDSig com RSA-SHA256
- ✅ Suporte aos ambientes de **Homologação** e **Produção**

> **Referência**: [nfse.gov.br](https://www.nfse.gov.br) | [Swagger da API](https://www.nfse.gov.br/swagger)

---

## Instalação

### Maven

```xml
<dependency>
    <groupId>io.github.nfse</groupId>
    <artifactId>java-nfse</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Schemas XSD

Baixe os schemas oficiais da RFB e coloque em `src/main/resources/schemas/`:

- `dps_v1.00.xsd` — Schema do DPS
- `nfse_v1.00.xsd` — Schema da NFS-e

Download: [nfse.gov.br/schemas](https://www.nfse.gov.br)

---

## Uso rápido

```java
// 1. Configurar o cliente
NfseClient client = NfseClient.builder()
    .certificado(CertificadoDigital.fromPfx("/path/cert.pfx", "senha"))
    .ambiente(Ambiente.HOMOLOGACAO)
    .build();

// 2. Montar o DPS
Dps dps = Dps.builder()
    .numero("1")
    .prestador(Prestador.builder()
        .cnpj("00000000000191")
        .inscricaoMunicipal("12345")
        .codigoMunicipio("3550308") // São Paulo
        .build())
    .tomador(Tomador.builder()
        .cpf("00000000000")
        .nome("João Silva")
        .build())
    .servico(Servico.builder()
        .codigoServico("0107")
        .descricao("Desenvolvimento de software")
        .build())
    .valores(Valores.builder()
        .valorServico(new BigDecimal("1500.00"))
        .aliquotaIss(new BigDecimal("0.05"))
        .build())
    .build();

// 3. Emitir
Nfse nfse = client.emitir(dps);
System.out.println("NFS-e emitida: " + nfse.getNumero());

// 4. Consultar
Nfse consultada = client.consultar(nfse.getNumero());

// 5. Cancelar
client.cancelar(nfse.getNumero(), "Erro na emissão");
```

### Certificado A3 (token)

```java
NfseClient client = NfseClient.builder()
    .certificado(CertificadoDigital.fromA3("/usr/lib/libpkcs11.so", "pin"))
    .ambiente(Ambiente.PRODUCAO)
    .build();
```

---

## Estrutura do projeto

```
src/
├── main/java/br/com/nfse/
│   ├── NfseClient.java          # Entrypoint principal
│   ├── auth/
│   │   ├── CertificadoDigital   # A1 e A3
│   │   └── TokenManager         # JWT automático
│   ├── model/
│   │   ├── dps/                 # DPS, Prestador, Tomador, Serviço, Valores
│   │   └── nfse/                # Nfse, SituacaoNfse
│   ├── xml/
│   │   ├── XmlBuilder           # Geração do XML
│   │   ├── XmlSigner            # Assinatura XMLDSig
│   │   └── XmlValidator         # Validação contra XSD
│   ├── api/
│   │   ├── NfseApiClient        # HTTP REST
│   │   ├── request/             # DTOs de request
│   │   └── response/            # DTOs de response
│   ├── config/
│   │   ├── Ambiente             # HOMOLOGACAO / PRODUCAO
│   │   └── NfseConfig           # Configuração central
│   └── exception/               # Exceções tipadas
└── test/
    └── ...
```

---

## Contribuindo

Veja [CONTRIBUTING.md](CONTRIBUTING.md).

## Licença

[MIT](LICENSE)
