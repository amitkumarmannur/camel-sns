package com.massfords.aws.sns;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
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
import com.massfords.aws.sns.support.SQSObject;
import com.massfords.aws.sns.support.SQSTypeConverter;

/**
 * Consumer subscribes to a topic (optionally creating it) and sends the messages it receives
 * as part of the subscription to the its processor.
 * 
 * @author markford
 */
public class SNSConsumer extends ScheduledPollConsumer {

    private static Log sLog = LogFactory.getLog(SNSConsumer.class);

    /** Headers on the message received by from the topic */
    private static String[] HEADERS = { "MessageId", 
                                        "Timestamp", 
                                        "TopicArn", 
                                        "Type", 
                                        "UnsubscribeURL",
                                         "Message", 
                                         "Subject", 
                                         "Signature", 
                                         "SignatureVersion" };
    
    /** subscription arn is kept in order to unsubscribe when the consumer is stopped */
    private String mSubscriptionArn;
    /** Set of previously processed messages kept when the idempotent flag is set */
    private LinkedHashSet mAlreadyProcessed = new LinkedHashSet();

    public SNSConsumer(SNSEndpoint aEndpoint, Processor aProcessor) {
        super(aEndpoint, aProcessor);
    }

    public void start() throws Exception {
        
        sLog.debug("starting");

        SNSEndpoint endpoint = (SNSEndpoint) getEndpoint();

        AmazonSNSClient client = AmazonClientFactory.createSNSClient(endpoint.getAccessKey(),
                endpoint.getSecretKey());

        String topicArn = endpoint.getTopicArn();
        sLog.debug("topicArn:" + topicArn);
        // create the queue if it doesn't exist
        AmazonSQSClient qClient = AmazonClientFactory.createSQSClient(endpoint.getAccessKey(),
                endpoint.getSecretKey());
        
        if (endpoint.getQueueArn() == null) {
            sLog.debug("queueArn not set, creating queue from queue name");
            String queueURL = qClient.createQueue(
                    new CreateQueueRequest().withQueueName(endpoint.getQueueName())).getQueueUrl();
            endpoint.setQueueURL(queueURL);
            sLog.debug("queueURL=" + queueURL);
    
            String queueArn = getQueueArn(qClient, queueURL);
            sLog.debug("queueArn=" + queueArn);
            endpoint.setQueueArn(queueArn);

            // set policy
            Map<String, String> policyMap = new HashMap();
            String policy = getPolicy(topicArn, queueArn, queueURL);
            policyMap.put("Policy", policy);
            sLog.debug("setting policy on newly created queue:" + policy);
            qClient.setQueueAttributes(new SetQueueAttributesRequest().withQueueUrl(queueURL).withAttributes(
                    policyMap));
        } else {
            String queueArn = endpoint.getQueueArn();
            String queueURL = SNSEndpoint.toQueueURL(queueArn);
            endpoint.setQueueURL(queueURL);
            sLog.debug("queueArn=" + queueArn);
            sLog.debug("queueURL=" + queueURL);
        }

        // subscribe
        SubscribeResult subresult = client.subscribe(new SubscribeRequest().withProtocol("sqs").withTopicArn(
                topicArn).withEndpoint(endpoint.getQueueArn()));
        sLog.debug("subscribed to topic: " + subresult.getSubscriptionArn());
        setSubscriptionArn(subresult.getSubscriptionArn());

        super.start();
    }
    
    public void stop() throws Exception {
        if (this.isStopped()) {
            sLog.debug("consumer already stopped, why is this being called twice?");
            return;
        }
        sLog.debug("stopping consumer...");
        SNSEndpoint endpoint = (SNSEndpoint) getEndpoint();
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
        String s = IOUtils.toString(SNSConsumer.class.getResourceAsStream(
                "/default-sqs-policy-template.json"));
        s = s.replace("$SNS_ARN", aTopicArn);
        s = s.replace("$SQS_ARN", aQueueArn);
        s = s.replace("$SQS_URL", new URI(aQueueURL).getPath());
        System.out.println("policy=\n" + s);
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

        SNSEndpoint endpoint = (SNSEndpoint) getEndpoint();
        String queueURL = endpoint.getQueueURL();

        sLog.debug("polling queue...");

        AmazonSQSClient qClient = AmazonClientFactory.createSQSClient(endpoint.getAccessKey(),
                endpoint.getSecretKey());
        ReceiveMessageResult result = qClient.receiveMessage(new ReceiveMessageRequest().withQueueUrl(queueURL).withMaxNumberOfMessages(1));
        if (!result.getMessages().isEmpty()) {

            sLog.debug("received message");

            Message message = result.getMessages().get(0);
            String receiptHandle = message.getReceiptHandle();
            
            String messageBody = message.getBody();
            SQSObject sqsObject = SQSTypeConverter.toSQSObject(messageBody);

            String messageId = sqsObject.getMessageId();
            if (endpoint.isIdempotent() & alreadyProcessed(messageId)) {
                sLog.debug("message already processed and idempotent flag set, ignoring message");
                return;
            }

            Exchange exchange = endpoint.createExchange(ExchangePattern.InOnly);
            if (sLog.isTraceEnabled())
                sLog.trace(sqsObject.toString());
            org.apache.camel.Message camelMessage = exchange.getIn();
            camelMessage.setBody(sqsObject.getMessage());
            for(String header : HEADERS) {
                camelMessage.setHeader("SNS:" + header, sqsObject.getString(header));
            }

            getProcessor().process(exchange);

            qClient.deleteMessage(new DeleteMessageRequest().withQueueUrl(queueURL).withReceiptHandle(
                    receiptHandle));
        }
    }

    protected String getSubscriptionArn() {
        return mSubscriptionArn;
    }

    protected void setSubscriptionArn(String aSubscriptionArn) {
        mSubscriptionArn = aSubscriptionArn;
    }

}
