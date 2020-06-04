package dk.bec.tpsi.openbanking.tppclient.httpclient;

import dk.bec.tpsi.openbanking.tppclient.model.TppClientRequest;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;

import java.util.stream.Stream;

public class TppRequestBuilder {
    RequestBuilder requestBuilder;

    private TppRequestBuilder(RequestBuilder requestBuilder) {
        this.requestBuilder = requestBuilder;
    }

    public static TppRequestBuilder create(String method) {
        return new TppRequestBuilder(RequestBuilder.create(method));
    }

    public static TppRequestBuilder create(TppClientRequest request) {
        TppRequestBuilder tppRequestBuilder = new TppRequestBuilder(RequestBuilder.create(request.getHttpMethod()));
        return tppRequestBuilder.addTppRequest(request);
    }

    public RequestBuilder delegate() {
        return requestBuilder;
    }

    public TppRequestBuilder addHeaders(TppClientRequest request) {
        if (MapUtils.isEmpty(request.getHeaders())) {
            return this;
        }

        request.getHeaders().forEach((k, vl) -> vl.forEach(v -> requestBuilder.addHeader(k, v)));
        return this;
    }

    public TppRequestBuilder addTppRequest(TppClientRequest request) {
        return this
                .addHeaders(request)
                .addParams(request)
                .addBody(request);
    }

    public TppRequestBuilder addParams(TppClientRequest request) {
        if (MapUtils.isEmpty(request.getRequestParams())) {
            return this;
        }

        requestBuilder.addParameters(request
                .getRequestParams()
                .entrySet()
                .stream()
                .flatMap(e ->
                        Stream.of(e.getValue()).map(v -> new BasicNameValuePair(e.getKey(), v))
                )
                .toArray(NameValuePair[]::new));
        return this;

    }

    public TppRequestBuilder addBody(TppClientRequest request) {
        if (StringUtils.isBlank(request.getRequestBody())) {
            return this;
        }

        requestBuilder.setEntity(new StringEntity(request.getRequestBody(), ContentType.APPLICATION_JSON));
        return this;
    }

    public TppRequestBuilder setUri(String uriToCall) {
        requestBuilder.setUri(uriToCall);
        return this;
    }

    public TppRequestBuilder replaceHeader(String name, String value) {
        requestBuilder.removeHeaders(name).addHeader(name, value);
        return this;
    }

    public HttpUriRequest build() {
        return requestBuilder.build();
    }
}
