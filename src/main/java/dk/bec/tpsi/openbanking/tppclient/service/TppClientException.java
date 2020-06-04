package dk.bec.tpsi.openbanking.tppclient.service;

public class TppClientException extends Exception {
    public TppClientException() {
    }

    public TppClientException(String message) {
        super(message);
    }

    public TppClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public TppClientException(Throwable cause) {
        super(cause);
    }

    public TppClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
