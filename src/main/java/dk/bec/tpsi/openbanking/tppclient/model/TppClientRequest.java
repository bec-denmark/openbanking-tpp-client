package dk.bec.tpsi.openbanking.tppclient.model;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class TppClientRequest {
    String path;
    Map<String, String[]> requestParams;
    String httpMethod;
    String requestBody;
    Map<String, List<String>> headers;

    public static class TppClientRequestBuilder {
        Map<String, List<String>> headers = new HashMap<>();

        public TppClientRequestBuilder addHeader(String name, String value){
            headers.put(name, Collections.singletonList(value));
            return this;
        }
    }
}
