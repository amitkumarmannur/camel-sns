/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.aws.sns;

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
        String subject = (String) aExchange.getIn().getHeader("SNS:Subject");
        if (subject == null)
            subject = endpoint.getSubject();
        String message = aExchange.getIn().getBody(String.class);
        
        if (sLog.isDebugEnabled()) {
            sLog.debug("producing sns message: subject | message=" + subject + " | " + message);
        }

        AmazonSNSClient client = AmazonClientFactory.createSNSClient(endpoint.getAccessKey(), endpoint.getSecretKey());
        
        PublishResult result = client.publish(new PublishRequest().withTopicArn(topicArn).withMessage(message).withSubject(subject));
        if (sLog.isDebugEnabled()) {
        sLog.debug("publish result:" + result.getMessageId());
        }
    }
    
    public void stop() throws Exception {
        if (this.isStopped()) {
            return;
        }
        SNSEndpoint endpoint = (SNSEndpoint) getEndpoint();
        endpoint.stop();
        super.stop();
    }

}
