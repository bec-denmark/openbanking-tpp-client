package dk.bec.tpsi.openbanking.tppclient.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class TppClientResponse {
    int status;
    String body;
    Map<String, List<String>> headers;
}
