package dk.bec.tpsi.openbanking.tppclient.security;

import com.google.common.collect.Sets;
import dk.bec.tpsi.openbanking.tppclient.model.TppClientCertParams;
import dk.bec.tpsi.openbanking.tppclient.model.TppClientRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;


import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class SecurityProviderServiceImpl implements SecurityProviderService {
    private static final String BC = "BC";
    private static final String PKCS_12 = "pkcs12";
    public static final String TPP_REDIRECT_URI = "tpp-redirect-uri";
    public static final String PSU_CORPORATE_ID = "psu-corporate-id";
    public static final String PSU_ID = "psu-id";
    public static final String X_REQUEST_ID = "x-request-id";
    public static final String DIGEST = "digest";
    public static final String SHA_256 = "SHA-256";
    private final static Set<String> DONT_RELAY = new HashSet<>(
            Arrays.asList("content-length", "cache-control", "accept", "user-agent", "connection", "host",
                    "accept-encoding", "x-forwarded-host", "cookie", "x-forwarded-proto", "x-forwarded-port", "x-forwarded-for"));

    private static final Set<String> SIGN_HEADERS = Sets.newHashSet(DIGEST, X_REQUEST_ID, PSU_ID, PSU_CORPORATE_ID, TPP_REDIRECT_URI);
    private KeyStore sealKeystore;
    private KeyStore wacKeystore;
    private TppClientCertParams certParams;

    public SecurityProviderServiceImpl(TppClientCertParams certParams) throws SecurityProviderServiceException {
        try {
            if (Security.getProvider(BC) == null) {
                // insert at specific position
                Security.insertProviderAt(new BouncyCastleProvider(), 1);
            }
            sealKeystore = loadKeyStore(certParams.getKeystorePath(), certParams.getSealCertName(), certParams.getSealCertPass(), certParams.getSealKeyAlias());
            wacKeystore = loadKeyStore(certParams.getKeystorePath(), certParams.getWacCertName(), certParams.getWacCertPass(), certParams.getWacKeyAlias());
            this.certParams = certParams;
        } catch (Exception e) {
            throw new SecurityProviderServiceException(e);
        }
    }

    private KeyStore loadKeyStore(String keystorePath, String keystoreName, String keystorePass, String keyAlias) throws SecurityProviderServiceException {
        try (InputStream instream = this.getClass().getClassLoader().getResourceAsStream(keystorePath + keystoreName)) {
            KeyStore keyStore = KeyStore.getInstance(PKCS_12, BC);
            keyStore.load(instream, keystorePass.toCharArray());
            if (keyStore.size() == 0) {
                throw new SecurityProviderServiceException("Empty keystore");
            }

            if (StringUtils.isNoneBlank(keyAlias) && !keyStore.containsAlias(keyAlias)){
                throw new SecurityProviderServiceException(String.format("Key with alias %s does not exist", keyAlias));
            }

            return keyStore;
        } catch (KeyStoreException | NoSuchAlgorithmException | NoSuchProviderException | IOException | CertificateException e) {
            throw new SecurityProviderServiceException(e);
        }
    }

    @Override
    public String createSignature(TppClientRequest request) throws SecurityProviderServiceException {
        if (MapUtils.isEmpty(request.getHeaders())) {
            return "";
        }

        String signingString = signingString(request);
        log.debug("Signing string is : {}", signingString);
        String signature = sign(signingString);

        return String.format(
                "keyId=\"%s\",algorithm=\"rsa-sha256\",headers=\"%s\",signature=\"%s\"",
                keyId(), signingHeaders(request), signature);
    }

    @Override
    public String digestBody(TppClientRequest request) throws SecurityProviderServiceException {
        try {
            String body = StringUtils.isBlank(request.getRequestBody()) ? "" : request.getRequestBody();

            MessageDigest sha256 = MessageDigest.getInstance(SHA_256);
            byte[] encoded = sha256.digest(body.getBytes(StandardCharsets.UTF_8));
            String digest = SHA_256 + "=" + Base64.getEncoder().encodeToString(encoded);
            log.debug("Digest is: {}", digest);
            request.getHeaders().put(DIGEST, Collections.singletonList(digest));
            return digest;
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityProviderServiceException(e);
        }
    }

    @Override
    public void filterHeaders(TppClientRequest request) {
        if (MapUtils.isEmpty(request.getHeaders())) {
            return;
        }

        request.setHeaders(request.getHeaders()
                .entrySet()
                .stream()
                .filter(h -> !DONT_RELAY.contains(h.getKey().toLowerCase()))
                .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue)));
    }

    private String signingHeaders(TppClientRequest tppClientRequest) {
        return tppClientRequest.getHeaders().keySet().stream()
                .filter(SIGN_HEADERS::contains)
                .collect(Collectors.joining(" "));
    }

    private String keyId() throws SecurityProviderServiceException {
        try {
            X509Certificate cert = (X509Certificate) getSealCertificate();
            BigInteger serialNumber = cert.getSerialNumber();
            String issuerName = cert.getIssuerX500Principal().getName();
            return String.format("SN=%s,CA=%s", serialNumber.toString(16), issuerName);
        } catch (KeyStoreException e) {
            throw new SecurityProviderServiceException(e);
        }
    }



    private String sign(String string) throws SecurityProviderServiceException {
        try {
            PrivateKey key = getSealPrivateKey();

            String signatureAlg = "SHA256withRSA";
            Signature sign = Signature.getInstance(signatureAlg);
            sign.initSign(key);
            sign.update(string.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(sign.sign());
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | InvalidKeyException | SignatureException e) {
            throw new SecurityProviderServiceException(e);
        }
    }

    private Certificate getSealCertificate() throws KeyStoreException {
        return sealKeystore.getCertificate(getSealKeyAlias());
    }

    private String getSealKeyAlias() throws KeyStoreException {
        if (StringUtils.isNoneBlank(certParams.getSealKeyAlias())){
            return certParams.getSealKeyAlias();
        }
        //get first
        return sealKeystore.aliases().nextElement();
    }

    private PrivateKey getSealPrivateKey() throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        return (PrivateKey) sealKeystore.getKey(getSealKeyAlias(), certParams.getSealCertPass().toCharArray());
    }

    private String signingString(TppClientRequest request) {
        return request.getHeaders().entrySet().stream()
                .filter(m -> SIGN_HEADERS.contains(m.getKey()))
                .map(m -> String.format("%s: %s", m.getKey(), String.join(",", m.getValue())))
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String getSignatureCertificate() throws SecurityProviderServiceException {
        try {
            Certificate cert = getSealCertificate();
            return Base64.getEncoder().encodeToString(cert.getEncoded());
        } catch (Exception e) {
            throw new SecurityProviderServiceException(e);
        }
    }

    @Override
    public KeyStore getWacKey() {
        return wacKeystore;
    }

    @Override
    public String getTrustStorePath() {
        return this.certParams.getTrustStorePath();
    }
}
