package com.massfords.aws.sns;

import org.junit.Test;

import com.massfords.aws.sns.SNSUri;

public class PublishByTopicArnTest extends AbstractUseCase {
    @Test
    public void test() throws Exception {
        
        String topicArn = createTopic();

        SNSUri consumer = createUri().withTopicArn(topicArn).withQueueName(mQueueName);

        SNSUri producer = new SNSUri(mCredentials).withTopicArn(topicArn);
        
        SnsTester tester = new SnsTester(consumer, producer, mContext)
                .withPreStartDelay(0)
                .withPostStartDelay(POLICY_DELAY_MILLIS)
                .withAcceptedMessage("subject-1", "message body-1")
                .withAcceptedMessage("subject-2", "message body-2")
                .withPostSendDelay(OTHER_DELAY_MILLIS);
        
        doTest(tester);
        
    }
}
