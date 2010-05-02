package com.massfords.aws.sns;

import org.junit.Test;

import com.massfords.aws.sns.SNSUri;

public class SubscribeByTopicNameAndQueueArn extends AbstractUseCase {

    @Test
    public void test() throws Exception {
        
        // create the queue ARN
        String queueArn = createQueue();

        SNSUri consumer = createUri().withTopicName(mTopicName).withQueueArn(queueArn);
        SNSUri producer = createUri().withTopicName(mTopicName);
        
        SnsTester tester = new SnsTester(consumer, producer, mContext)
                .withPreStartDelay(POLICY_DELAY_MILLIS)
                .withPostStartDelay(OTHER_DELAY_MILLIS)
                .withAcceptedMessage("subject-1", "message body-1")
                .withAcceptedMessage("subject-2", "message body-2")
                .withPostSendDelay(OTHER_DELAY_MILLIS);

        doTest(tester);
    }

}
