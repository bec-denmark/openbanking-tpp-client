package dk.bec.tpsi.openbanking.tppclient.security;

public class SecurityProviderServiceException extends Exception {
    public SecurityProviderServiceException() {
    }

    public SecurityProviderServiceException(String message) {
        super(message);
    }

    public SecurityProviderServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecurityProviderServiceException(Throwable cause) {
        super(cause);
    }

    public SecurityProviderServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
