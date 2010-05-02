/**
 * 
 */
package com.massfords.aws.sns;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;

import com.massfords.aws.sns.SNSEndpoint;
import com.massfords.aws.sns.SNSUri;

class SnsTester {
    
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

    private void sendBodies() {
        ProducerTemplate producer = mContext.createProducerTemplate();
        for(Iterator it = getMessages(); it.hasNext();) {
            String[] message = (String[]) it.next();
            SNSUri uri = new SNSUri(mProducerUri);
            uri.withProperty("subject", message[0]);
            producer.sendBody(uri.toString(), message[1]);
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