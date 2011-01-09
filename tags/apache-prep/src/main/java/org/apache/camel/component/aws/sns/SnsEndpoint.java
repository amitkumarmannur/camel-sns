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
import java.util.concurrent.ExecutorService;

import org.apache.camel.CamelContext;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.ScheduledPollEndpoint;
import org.apache.camel.util.concurrent.ExecutorServiceHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.DeleteTopicRequest;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;

public class SnsEndpoint extends ScheduledPollEndpoint {

    private static final Log sLog = LogFactory.getLog(SnsEndpoint.class);
    
    private String mQueueName;
    private String mSecretKey;
    private String mAccessKey;
    private String mQueueURL;
    private String mQueueArn;
    private String mSubject;
    private String mTopicArn;
    private boolean mDeleteTopicOnStop;
    private boolean mDeleteQueueOnStop;
    private boolean mIdempotent;
    private boolean mVerify;
    
    public SnsEndpoint(String aUri, CamelContext aContext) {
        super(aUri, aContext);
    }

    public Consumer createConsumer(Processor aProcessor) throws Exception {
        if (sLog.isDebugEnabled()) {
        sLog.debug("creating consumer for endpoint:" + stripCredentials(getEndpointUri()));
        }
        SnsConsumer consumer = new SnsConsumer(this, aProcessor);
        configureConsumer(consumer);
        return consumer;
    }

    @Override
    public String toString() {
        return "Endpoint[" + stripCredentials(getEndpointUri()) + "]";
    }
    
    @Override
    public synchronized ExecutorService getExecutorService() {
        return ExecutorServiceHelper.newScheduledThreadPool(10, stripCredentials(getEndpointUri()), true);
    }

    protected static String stripCredentials(String aUri) {
        return aUri.replaceAll("(accessKey=)[A-Za-z0-9]+", "$1hidden")
                   .replaceAll("(secretKey=)[A-Za-z0-9]+", "$1hidden");
    }
    
    protected String createEndpointUri() {
        return getEndpointUri();
    }

    public Producer createProducer() throws Exception {
        SnsProducer producer = new SnsProducer(this);
        return producer;
    }

    public boolean isSingleton() {
        return true;
    }
    
    public String getTopicArn() throws Exception {

        if (mTopicArn == null) {
            // the uri is either an Arn or a shortcut to create a topic by name
            String uri = getEndpointUri();
            String topicArn = getArn(uri);
            if (isAutoCreateByTopic(topicArn)) {
                
                String topic = topicArn.substring(topicArn.lastIndexOf('/') + 1);

                if (sLog.isDebugEnabled()) {
                sLog.debug("creating a new topic by name:" + topic);
                }

                AmazonSNSClient client = AmazonClientFactory.createSNSClient(getAccessKey(), getSecretKey());
                CreateTopicResult result = client.createTopic(new CreateTopicRequest().withName(topic));
                topicArn = result.getTopicArn();
            } else {
                if (sLog.isDebugEnabled()) {
                sLog.debug("topicArn provided in uri:" + topicArn);
                }
            }
            mTopicArn = topicArn;
        }
        return mTopicArn;
    }
    
    protected void stop() {
        sLog.debug("stopping endpoint");
        if (isDeleteTopicOnStop()) {
            try {
                String topicArn = getTopicArn();
                if (sLog.isDebugEnabled()) {
                sLog.debug("deleting topic on consumer stop:" + topicArn);
                }
                AmazonSNSClient client = AmazonClientFactory.createSNSClient(getAccessKey(),
                        getSecretKey());
                client.deleteTopic(new DeleteTopicRequest().withTopicArn(topicArn));
            } catch (Exception e) {
                sLog.error("error deleting topic during stop", e);
            }
        }
        
        if (isDeleteQueueOnStop()) {
            if (sLog.isDebugEnabled()) {
            sLog.debug("deleting queue on consumer stop:" + getQueueURL());
            }
            try {
                AmazonSQSClient client = AmazonClientFactory.createSQSClient(getAccessKey(), getSecretKey());
                client.deleteQueue(new DeleteQueueRequest().withQueueUrl(getQueueURL()));
            } catch (Exception e) {
                sLog.error("error deleting queue during stop", e);
            }
        }
    }
    
    protected static String toQueueURL(String aQueueArn) {
        
        String[] values = aQueueArn.split(":");
        
        StringBuilder sb = new StringBuilder();
        String accountId = values[values.length - 2];
        String queueName = values[values.length - 1];
        sb.append("https://queue.amazonaws.com/").append(accountId).append('/').append(queueName);
        String queueURL = sb.toString();
        return queueURL;
    }

    static String getArn(String aURI) throws Exception {
        URI uri = new URI(aURI);
        String schemeSpecificPart = uri.getSchemeSpecificPart();
        String arn = schemeSpecificPart.substring(0, schemeSpecificPart.lastIndexOf('?'));
        if (arn.startsWith("//"))
            return arn.substring(2);
        return arn;
    }
    
    private boolean isAutoCreateByTopic(String aArn) {
        return !aArn.startsWith("arn:aws");
    }
    
    public String getQueueName() {
        return mQueueName;
    }

    public void setQueueName(String aQueueName) {
        mQueueName = aQueueName;
    }

    public String getSecretKey() {
        return mSecretKey;
    }

    public void setSecretKey(String aSecretKey) {
        mSecretKey = aSecretKey;
    }

    public String getAccessKey() {
        return mAccessKey;
    }

    public void setAccessKey(String aAccessKey) {
        mAccessKey = aAccessKey;
    }

    public String getQueueURL() {
        return mQueueURL;
    }

    public void setQueueURL(String aQueueURL) {
        mQueueURL = aQueueURL;
    }

    public String getQueueArn() {
        return mQueueArn;
    }

    public void setQueueArn(String aQueueArn) {
        mQueueArn = aQueueArn;
    }

    public String getSubject() {
        return mSubject;
    }

    public void setSubject(String aSubject) {
        mSubject = aSubject;
    }

    public boolean isDeleteTopicOnStop() {
        return mDeleteTopicOnStop;
    }

    public void setDeleteTopicOnStop(boolean aDeleteTopicOnStop) {
        mDeleteTopicOnStop = aDeleteTopicOnStop;
    }

    public boolean isDeleteQueueOnStop() {
        return mDeleteQueueOnStop;
    }

    public void setDeleteQueueOnStop(boolean aDeleteQueueOnStop) {
        mDeleteQueueOnStop = aDeleteQueueOnStop;
    }

    public boolean isIdempotent() {
        return mIdempotent;
    }

    public void setIdempotent(boolean aIdempotent) {
        mIdempotent = aIdempotent;
    }

    public boolean isVerify() {
        return mVerify;
    }

    public void setVerify(boolean aVerify) {
        mVerify = aVerify;
    }

}
