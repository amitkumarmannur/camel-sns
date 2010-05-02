package com.massfords.aws.sns;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

public class SNSProducer extends DefaultProducer {
    
    private static final Log sLog = LogFactory.getLog(SNSProducer.class);

    public SNSProducer(Endpoint aEndpoint) {
        super(aEndpoint);
    }

    public void process(Exchange aExchange) throws Exception {
        
        SNSEndpoint endpoint = (SNSEndpoint) getEndpoint();
        
        String topicArn = endpoint.getTopicArn();
        String subject = endpoint.getSubject();
        String message = aExchange.getIn().getBody(String.class);
        
        if (sLog.isDebugEnabled())
            sLog.debug("producing sns message: subject | message=" + subject + " | " + message);
        
        AmazonSNSClient client = AmazonClientFactory.createSNSClient(endpoint.getAccessKey(), endpoint.getSecretKey());
        
        PublishResult result = client.publish(new PublishRequest().withTopicArn(topicArn).withMessage(message).withSubject(subject));
        sLog.debug("publish result:" + result.getMessageId());
    }
    
    public void stop() throws Exception {
        if (this.isStopped())
            return;
        SNSEndpoint endpoint = (SNSEndpoint) getEndpoint();
        endpoint.stop();
        super.stop();
    }

}
