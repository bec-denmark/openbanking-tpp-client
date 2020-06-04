package dk.bec.tpsi.openbanking.tppclient.service;

import dk.bec.tpsi.openbanking.tppclient.model.TppClientRequest;
import dk.bec.tpsi.openbanking.tppclient.model.TppClientResponse;

public interface TppClientService {

    TppClientResponse callGateway(String gatewayUrl, TppClientRequest request) throws TppClientException;

    TppClientResponse callGateway(TppClientRequest request) throws TppClientException;

}
