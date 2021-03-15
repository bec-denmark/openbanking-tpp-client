package dk.bec.tpsi.openbanking.tppclient.util;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HeaderUtils {
    public static Map<String, List<String>> collectHeaders(CloseableHttpResponse response) {
        return Stream
                .of(response.getAllHeaders())
                .collect(Collectors.toMap(NameValuePair::getName, HeaderUtils::getHeaderValueList, (listOne, listTwo) ->
                        Stream.of(listOne, listTwo)
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList())
                ));
    }

    public static List<String> getHeaderValueList(Header h) {
        return Collections.singletonList(h.getValue());
    }

}
