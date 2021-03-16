# Third Party Provider (TPP) client library

## Check build status
[![Java CI with Maven](https://github.com/bec-denmark/openbanking-tpp-client/actions/workflows/maven.yml/badge.svg)](https://github.com/bec-denmark/openbanking-tpp-client/actions/workflows/maven.yml)

## PSD2 - background and context information

With **[PSD2](https://en.wikipedia.org/wiki/Payment_Services_Directive#Revised_Directive_on_Payment_Services_(PSD2))** the European Union has published a [new directive on payment services in the
internal market](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A32015L2366). Member States were required to adopt this directive into their national law by the 
13th of January 2018. **PSD2** contains regulations of new services to be operated by so 
called **Third Party Payment Service Providers (TPP)** on behalf of a Payment Service User (PSU). 
The ideal behind this is enbling the consumers (PSUs) to access their accounts in the manner they prefer
not being tied to one bank's interface.

In order to be able to operate the new services for the PSUs a **TPP** needs to access the PSU's accounts, 
which is usually managed by another PSP called the Account Servicing Payment Service Provider (ASPSP).

## TPP - transport layer requirements
The communication between the TPP and the ASPSP is always secured by using a TLSconnection 
using TLS version 1.2 or higher. This TLS-connection is set up by the TPP. It is not necessary 
to set up a new TLS-connection for each transaction, however the ASPSP might terminate an existing 
TLS-connection if required by its security setting.

The TLS-connection has to be established always including client (i.e. TPP) authentication.
For this authentication the TPP has to use a qualified certificate for website authentication.
This qualified certificate has to be issued by a qualified trust service provider according 
to the [eIDAS regulation](https://en.wikipedia.org/wiki/EIDAS) (eIDAS). The content of the certificate has to be compliant with the
requirements of (EBA-RTS). The certificate of the TPP has to indicate all roles 
the TPP is authorised to use.

## TPP client library

### Purpose 

This utility library helps TPP developers to properly configure and establish a secure connection.
It also addresses all HTTP headers- and message signing- related requirements. 

### Basics

First, use TppClientCertParams to provide Website Authentication Certificate (wac) and signing (seal) 
client certificate file details(location, name, alias, password).

```java

    TppClientCertParams tppClientCertParams = TppClientCertParams.builder().keystorePath("keystore_path")
            .sealCertName("seal_certificate.p12")
            //the alias is optional, if not specified the first key is used 
            .sealKeyAlias("seal_certificate_alias")
            .sealCertPass("seal_cert_pass")
            .wacCertName("wac_certificate.p12")
            .wacCertPass("wac_certificate_pass")
            //the alias is optional, if not specified the first certificate is used
            .wacKeyAlias("wac_certificate_alias")
            .build(); 

```

Next, instantiate a TppClientService 
```java

    TppClientService tppClientService = new TppClientServiceImpl("https://some.gateway.url", tppClientCertParams);

```
Finally use TppClientRequest, TppClientService and TppClientResponse to handle the communications.

```java

    TppClientRequest request = TppClientRequest
            .builder()
            .httpMethod("GET")
            .path("/1.0/v1/accounts")
            .headers(headers)
            .requestParams(requestParams)
            .build();

    try {
        response = tppClientService.callGateway(request);

        if (HttpStatus.OK.value() != response.getStatus()) {
            // handle HTTP error 
        } else {
            //handle response.getBody();
        }
    } catch (TppClientException e) {
        // handle TppClientException 
    }

``` 
### Enrollmnt example
https://gist.github.com/kczechbec/7593f9556f76bfd2d799cb65d27e9afc
