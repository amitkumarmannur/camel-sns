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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.Producer;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;

/**
 * Helper class for testing the producing and consuming messages on the topic. 
 * 
 * The class is configured with a URI for the subscription and another for producing 
 * messages. The callers add messages that should be sent and also indicate whether
 * the message should be received by the subscription or filtered out. The route
 * to use for the test is also configurable with the default route being one 
 * without any filtering.
 * 
 * There are three points in the test process that have the possibility for introducing
 * a delay (Thread.sleep). The main purpose for this delay is to allow for changes made
 * to a SQS policy to propagate. For example, there is a 90+ second delay on setting the
 * policy on a queue and having it take effect. The points for a delay are as follows:
 * - pre start: sleep before starting the context
 * - post start: sleep after starting the context
 * - post send: sleep after sending the messages
 * 
 * @author markford
 */
public class SnsTester {
    
    long mPreStartDelay;
    long mPostStartDelay;
    long mPostSendDelay;
    
    SNSUri mConsumerUri;
    SNSUri mProducerUri;
    CamelContext mContext;
    List<String[]> mData = new LinkedList();
    List<String> mExpectedMessages = new LinkedList();
    RouteBuilder mRouteBuilder;
    
    public SnsTester(SNSUri aConsumerUri, SNSUri aProducerUri, CamelContext aContext) {
        mConsumerUri = aConsumerUri;
        mProducerUri = aProducerUri;
        mContext = aContext;
    }
    
    protected Iterator<String[]> getMessages() {
        return mData.iterator();
    }
    
    protected List<String> getExpectedMessageBodies() {
        return mExpectedMessages;
    }
    
    protected SnsTester withAcceptedMessage(String aSubject, String aBody) {
        return withMessage(aSubject, aBody, true);
    }

    protected SnsTester withFilteredMessage(String aSubject, String aBody) {
        return withMessage(aSubject, aBody, false);
    }

    protected SnsTester withMessage(String aSubject, String aBody, boolean aAccepted) {
        mData.add(new String[] {aSubject, aBody});
        if (aAccepted)
            mExpectedMessages.add(aBody);
        return this;
    }
    
    protected SnsTester withPreStartDelay(long aPreStartDelay) {
        setPreStartDelay(aPreStartDelay);
        return this;
    }
    
    protected SnsTester withPostStartDelay(long aPostStartDelay) {
        setPostStartDelay(aPostStartDelay);
        return this;
    }

    protected SnsTester withPostSendDelay(long aPostSendDelay) {
        setPostSendDelay(aPostSendDelay);
        return this;
    }

    protected long getPreStartDelay() {
        return mPreStartDelay;
    }

    protected void setPreStartDelay(long aPreStartDelay) {
        mPreStartDelay = aPreStartDelay;
    }

    protected long getPostStartDelay() {
        return mPostStartDelay;
    }

    protected void setPostStartDelay(long aPostStartDelay) {
        mPostStartDelay = aPostStartDelay;
    }

    protected long getPostSendDelay() {
        return mPostSendDelay;
    }

    protected void setPostSendDelay(long aPostSendDelay) {
        mPostSendDelay = aPostSendDelay;
    }
    
    protected void send() throws Exception {

        mContext.addRoutes(getRouteBuilder());
        
        MockEndpoint sink = getMockEndpoint();
        sink.expectedBodiesReceivedInAnyOrder(getExpectedMessageBodies());
        
        System.out.println("pre-start-delay:" + getPreStartDelay());
        Thread.sleep(getPreStartDelay());

        mContext.start();
        
        System.out.println("post-start-delay:" + getPostStartDelay());
        Thread.sleep(getPostStartDelay());

        sendBodies();
        
        // assert that we get the message
        System.out.println("post-send-delay:" + getPostSendDelay());
        Thread.sleep(getPostSendDelay());
    }
    
    protected RouteBuilder getRouteBuilder() {
        if (mRouteBuilder == null) {
            return new RouteBuilder() {

                @Override
                public void configure() throws Exception {
                    from(mConsumerUri.toString()).to(AbstractUseCase.MOCK_SINK);
                }
            };
        }
        return mRouteBuilder;
    }
    
    protected void setRouteBuilder(RouteBuilder aRouteBuilder) {
        mRouteBuilder = aRouteBuilder;
    }

    private void sendBodies() throws Exception {
        if (mProducerUri == null)
            return;
        SNSEndpoint endpoint = (SNSEndpoint) mContext.getEndpoint(mProducerUri.toString());
        Producer producer = endpoint.createProducer();
        producer.start();
        
        for(Iterator it = getMessages(); it.hasNext();) {
            Exchange exchange = producer.createExchange(ExchangePattern.InOnly);
            Message message = exchange.getIn();
            String[] messageData = (String[]) it.next();
            message.setHeader("SNS:Subject", messageData[0]);
            message.setBody(messageData[1]);
            producer.process(exchange);
        }
    }

    protected MockEndpoint getMockEndpoint() {
        MockEndpoint sink = (MockEndpoint) mContext.getEndpoint(AbstractUseCase.MOCK_SINK);
        return sink;
    }

    protected SNSEndpoint getConsumerEndpoint() {
        SNSEndpoint snsEndpoint = (SNSEndpoint) mContext.getEndpoint(mConsumerUri.toString());
        return snsEndpoint;
    }
}
