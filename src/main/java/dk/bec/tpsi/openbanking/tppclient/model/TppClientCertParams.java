package dk.bec.tpsi.openbanking.tppclient.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TppClientCertParams implements Serializable {
    String keystorePath;
    String trustStorePath;
    String wacCertPass;
    String wacCertName;
    String wacKeyAlias;
    String sealCertPass;
    String sealCertName;
    String sealKeyAlias;
}
