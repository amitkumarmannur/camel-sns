package com.massfords.aws.sns;

import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;

import com.massfords.aws.sns.SNSUri;

public class FilterBySubjectTest extends AbstractUseCase {
    @Test
    public void test() throws Exception {
        
        final SNSUri consumer = createUri().withTopicName(mTopicName).withQueueName(mQueueName).withDelay(500);
        SNSUri producer = createUri().withTopicName(mTopicName);
        
        SnsTester tester = new SnsTester(consumer, producer, mContext)
                .withPreStartDelay(0)
                .withPostStartDelay(POLICY_DELAY_MILLIS)
                .withFilteredMessage("on the right subject", "message body-0")
                .withAcceptedMessage("ok-subject", "message body-1")
                .withAcceptedMessage("ok-subject", "message body-2")
                .withFilteredMessage("will get filtered", "message body-3")
                .withFilteredMessage("also filtered", "message body-4")
                .withAcceptedMessage("ok-subject", "message body-5")
                .withAcceptedMessage("ok-subject", "message body-6")
                .withPostSendDelay(OTHER_DELAY_MILLIS * 2);
        
        tester.setRouteBuilder(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from(consumer.toString()).filter(header("SNS:Subject").isEqualTo("ok-subject")).to(MOCK_SINK);
            }
        });
        
        doTest(tester);
    }

}
