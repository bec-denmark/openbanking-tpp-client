package dk.bec.tpsi.openbanking.tppclient.service;

import dk.bec.tpsi.openbanking.tppclient.httpclient.TppRequestBuilder;
import dk.bec.tpsi.openbanking.tppclient.model.TppClientCertParams;
import dk.bec.tpsi.openbanking.tppclient.model.TppClientRequest;
import dk.bec.tpsi.openbanking.tppclient.model.TppClientResponse;
import dk.bec.tpsi.openbanking.tppclient.security.SecurityProviderService;
import dk.bec.tpsi.openbanking.tppclient.security.SecurityProviderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static dk.bec.tpsi.openbanking.tppclient.util.HeaderUtils.collectHeaders;

@Slf4j
public class TppClientServiceImpl implements TppClientService {
    private static final String SIGNATURE = "signature";
    private static final String TPP_SIGNATURE_CERTIFICATE = "tpp-signature-certificate";

    private SecurityProviderService securityProviderService;
    private SSLContext sslContext;
    private String gatewayUrl;

    public TppClientServiceImpl(String gatewayUrl, TppClientCertParams certParams) throws TppClientException {
        this.gatewayUrl = gatewayUrl;
        parseParams(certParams);
    }

    public TppClientServiceImpl(TppClientCertParams certParams) throws TppClientException {
        parseParams(certParams);
    }

    private void parseParams(TppClientCertParams certParams) throws TppClientException {
        try {
            securityProviderService = new SecurityProviderServiceImpl(certParams);
            SSLContextBuilder sslBuilder = new SSLContextBuilder();

            String trustStorePath = securityProviderService.getTrustStorePath();
            if (StringUtils.isNotEmpty(trustStorePath)) {
                log.debug("Setting custom SSL builder trust store from path: {}", trustStorePath);
                sslBuilder.loadTrustMaterial(new File(trustStorePath));
            }

            if (Objects.nonNull(securityProviderService.getWacKey())) {
                sslBuilder.loadKeyMaterial(securityProviderService.getWacKey(), certParams.getWacCertPass().toCharArray());
            }

            sslContext = sslBuilder.build();

        } catch (Exception e) {
            log.error("Error creating tpp client service", e);
            throw new TppClientException(e);
        }
    }

    private CloseableHttpClient getHttpClient() {
        return HttpClientBuilder
                .create()
                .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                .setSSLContext(sslContext)
                .build();
    }

    @Override
    public TppClientResponse callGateway(String gatewayUrl, TppClientRequest request) throws TppClientException {
        HttpUriRequest httpRequest = buildRequest(gatewayUrl, request);

        return callTargetGateway(httpRequest);
    }

    @Override
    public TppClientResponse callGateway(TppClientRequest request) throws TppClientException {
        HttpUriRequest httpRequest = buildRequest(gatewayUrl, request);

        return callTargetGateway(httpRequest);
    }

    private TppClientResponse callTargetGateway(HttpUriRequest httpRequest) throws TppClientException {
        TppClientResponse tppClientResponse;
        try (CloseableHttpClient httpClient = getHttpClient(); CloseableHttpResponse response = httpClient.execute(httpRequest)) {
            tppClientResponse = TppClientResponse
                    .builder()
                    .body(bodyToString(response))
                    .status(response.getStatusLine().getStatusCode())
                    .headers(collectHeaders(response))
                    .build();
        } catch (Exception e) {
            log.error("Error calling gateway", e);
            throw new TppClientException(e);
        }
        return tppClientResponse;
    }

    private HttpUriRequest buildRequest(String gatewayUrl, TppClientRequest request) throws TppClientException {
        HttpUriRequest httpRequest;
        try {
            String uriToCall = buildUri(gatewayUrl, request);
            log.debug("Calling {}", uriToCall);
            securityProviderService.filterHeaders(request);
            securityProviderService.digestBody(request);

            httpRequest = TppRequestBuilder
                    .create(request)
                    .replaceHeader(SIGNATURE, securityProviderService.createSignature(request))
                    .replaceHeader(TPP_SIGNATURE_CERTIFICATE, securityProviderService.getSignatureCertificate())
                    .setUri(uriToCall)
                    .build();
        } catch (Exception e) {
            log.error("Error calling gateway", e);
            throw new TppClientException(e);
        }
        return httpRequest;
    }

    private String bodyToString(CloseableHttpResponse response) throws IOException {
        if (Objects.isNull(response.getEntity()) || Objects.isNull(response.getEntity().getContent())) {
            return "";
        }

        return IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8.name());
    }

    private String buildUri(String gatewayUrl, TppClientRequest request) {
        return gatewayUrl + request.getPath();
    }

}
