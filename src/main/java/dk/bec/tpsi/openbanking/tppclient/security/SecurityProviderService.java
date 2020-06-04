package dk.bec.tpsi.openbanking.tppclient.security;

import dk.bec.tpsi.openbanking.tppclient.model.TppClientRequest;

import java.security.KeyStore;

public interface SecurityProviderService {
    String createSignature(TppClientRequest request) throws SecurityProviderServiceException;

    String digestBody(TppClientRequest request) throws SecurityProviderServiceException;

    void filterHeaders(TppClientRequest request);

    String getSignatureCertificate() throws SecurityProviderServiceException;

    KeyStore getWacKey();

    String getTrustStorePath();
}
