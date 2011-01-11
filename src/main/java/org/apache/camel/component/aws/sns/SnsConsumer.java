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

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.component.aws.sns.support.SnsSqsObject;
import org.apache.camel.component.aws.sns.support.SnsSqsTypeConverter;
import org.apache.camel.impl.ScheduledPollConsumer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.amazonaws.services.sns.model.UnsubscribeRequest;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;

/**
 * Consumer subscribes to a topic (optionally creating it) and sends the
 * messages it receives as part of the subscription to the its processor.
 *
 * @author markford
 */
public class SnsConsumer extends ScheduledPollConsumer {

    private static Log sLog = LogFactory.getLog(SnsConsumer.class);

    /**
     * Headers on the message received by from the topic
     */
    private static String[] HEADERS = {"MessageId",
            "Timestamp",
            "TopicArn",
            "Type",
            "UnsubscribeURL",
            "Message",
            "Subject",
            "Signature",
            "SignatureVersion"};

    /**
     * subscription arn is kept in order to unsubscribe when the consumer is
     * stopped
     */
    private String mSubscriptionArn;
    /**
     * Set of previously processed messages kept when the idempotent flag is set
     */
    private LinkedHashSet mAlreadyProcessed = new LinkedHashSet();

    public SnsConsumer(SnsEndpoint aEndpoint, Processor aProcessor) {
        super(aEndpoint, aProcessor);
    }

    @Override
    public String toString() {
        return "Consumer[" + SnsEndpoint.stripCredentials(getEndpoint().getEndpointUri()) + "]";
    }

    public SnsEndpoint getEndpoint() {
        return (SnsEndpoint) super.getEndpoint();
    }

    public void start() throws Exception {

        sLog.debug("starting");

        SnsEndpoint endpoint = getEndpoint();

        AmazonSNSClient client = AmazonClientFactory.createSNSClient(endpoint.getAccessKey(),
                endpoint.getSecretKey());

        String topicArn = endpoint.getTopicArn();
        if (sLog.isDebugEnabled()) {
            sLog.debug("topicArn:" + topicArn);
        }
        // create the queue if it doesn't exist
        AmazonSQSClient qClient = AmazonClientFactory.createSQSClient(endpoint.getAccessKey(),
                endpoint.getSecretKey());

        if (endpoint.getQueueArn() == null) {
            sLog.debug("queueArn not set, creating queue from queue name");
            String queueURL = qClient.createQueue(
                    new CreateQueueRequest().withQueueName(endpoint.getQueueName())).getQueueUrl();
            endpoint.setQueueURL(queueURL);
            if (sLog.isDebugEnabled()) {
                sLog.debug("queueURL=" + queueURL);
            }
            String queueArn = getQueueArn(qClient, queueURL);
            if (sLog.isDebugEnabled()) {
                sLog.debug("queueArn=" + queueArn);
            }
            endpoint.setQueueArn(queueArn);

            // set policy
            Map<String, String> policyMap = new HashMap();
            String policy = getPolicy(topicArn, queueArn, queueURL);
            policyMap.put("Policy", policy);
            if (sLog.isDebugEnabled()) {
                sLog.debug("setting policy on newly created queue:" + policy);
            }
            qClient.setQueueAttributes(new SetQueueAttributesRequest().withQueueUrl(queueURL).withAttributes(
                    policyMap));
        } else {
            String queueArn = endpoint.getQueueArn();
            String queueURL = SnsEndpoint.toQueueURL(queueArn);
            endpoint.setQueueURL(queueURL);
            if (sLog.isDebugEnabled()) {
                sLog.debug("queueArn=" + queueArn);
                sLog.debug("queueURL=" + queueURL);
            }
        }

        // subscribe
        SubscribeResult subresult = client.subscribe(new SubscribeRequest().withProtocol("sqs").withTopicArn(
                topicArn).withEndpoint(endpoint.getQueueArn()));
        if (sLog.isDebugEnabled()) {
            sLog.debug("subscribed to topic: " + subresult.getSubscriptionArn());
        }
        setSubscriptionArn(subresult.getSubscriptionArn());

        super.start();
    }

    public void stop() throws Exception {
        if (this.isStopped()) {
            sLog.debug("consumer already stopped, why is this being called twice?");
            return;
        }
        sLog.debug("stopping consumer...");
        SnsEndpoint endpoint = (SnsEndpoint) getEndpoint();
        endpoint.stop();

        if (!endpoint.isDeleteTopicOnStop()) {
            sLog.debug("unsubscribing from topic");
            AmazonSNSClient client = AmazonClientFactory.createSNSClient(endpoint.getAccessKey(),
                    endpoint.getSecretKey());
            client.unsubscribe(new UnsubscribeRequest().withSubscriptionArn(mSubscriptionArn));
        }

        super.stop();
    }

    protected static String getQueueArn(AmazonSQSClient qClient, String queueURL) {
        String queueArn = qClient.getQueueAttributes(
                new GetQueueAttributesRequest().withQueueUrl(queueURL).withAttributeNames(
                        "QueueArn")).getAttributes().get("QueueArn");
        return queueArn;
    }

    protected static String getPolicy(String aTopicArn, String aQueueArn, String aQueueURL)
            throws Exception {
        // FIXME can make this more efficient
        String s = IOUtils.toString(SnsConsumer.class.getResourceAsStream("/default-sqs-policy-template.json"));
        s = s.replace("$SNS_ARN", aTopicArn);
        s = s.replace("$SQS_ARN", aQueueArn);
        s = s.replace("$SQS_URL", new URI(aQueueURL).getPath());
        s = s.replace("\n", " ");
        s = s.replace("\r", " ");
        return s;
    }

    protected boolean alreadyProcessed(String aMessageId) {
        boolean alreadyProcessed = !mAlreadyProcessed.add(aMessageId);
        if (mAlreadyProcessed.size() > 100) {
            Object oldest = mAlreadyProcessed.iterator().next();
            mAlreadyProcessed.remove(oldest);
        }
        return alreadyProcessed;
    }

    protected void poll() throws Exception {

        SnsEndpoint endpoint = (SnsEndpoint) getEndpoint();
        String queueURL = endpoint.getQueueURL();

        sLog.trace("polling queue...");

        AmazonSQSClient qClient = AmazonClientFactory.createSQSClient(endpoint.getAccessKey(),
                endpoint.getSecretKey());
        ReceiveMessageResult result = qClient.receiveMessage(new ReceiveMessageRequest().withQueueUrl(
                queueURL).withMaxNumberOfMessages(1));
        if (!result.getMessages().isEmpty()) {

            sLog.debug("received message");

            Message message = result.getMessages().get(0);
            String receiptHandle = message.getReceiptHandle();

            String messageBody = message.getBody();
            SnsSqsObject snsSqsObject = SnsSqsTypeConverter.toSQSObject(messageBody);

            boolean verified = true;
            if (endpoint.isVerify() && !verifyMessage(snsSqsObject)) {
                sLog.debug("message failed verification, deleting");
                verified = false;
            }

            if (verified) {
                String messageId = snsSqsObject.getMessageId();
                if (endpoint.isIdempotent() & alreadyProcessed(messageId)) {
                    sLog.debug("message already processed and idempotent flag set, ignoring message");
                    return;
                }

                Exchange exchange = endpoint.createExchange(ExchangePattern.InOnly);
                if (sLog.isTraceEnabled())
                    sLog.trace(snsSqsObject.toString());
                org.apache.camel.Message camelMessage = exchange.getIn();
                camelMessage.setBody(snsSqsObject.getMessage());
                for (String header : HEADERS) {
                    camelMessage.setHeader("SNS:" + header, snsSqsObject.getString(header));
                }

                getProcessor().process(exchange);
            }

            qClient.deleteMessage(new DeleteMessageRequest().withQueueUrl(queueURL).withReceiptHandle(
                    receiptHandle));
        }
    }

    public static boolean verifyMessage(SnsSqsObject aSnsSqsObject) throws Exception {
        return SnsSqsTypeConverter.verify(aSnsSqsObject);
    }

    protected String getSubscriptionArn() {
        return mSubscriptionArn;
    }

    protected void setSubscriptionArn(String aSubscriptionArn) {
        mSubscriptionArn = aSubscriptionArn;
    }

}
