package com.massfords.aws.sns;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;

/**
 * Standard component that creates SNSEndpoints given a uri and params.
 * 
 * @author markford
 */
public class SNSComponent extends DefaultComponent {

    protected Endpoint createEndpoint(String aUri, String aRemaining, Map aParams) throws Exception {
        return new SNSEndpoint(aUri, getCamelContext());
    }

}
