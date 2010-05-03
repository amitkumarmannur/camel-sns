package com.massfords.aws.sns;

import org.junit.Test;

import com.massfords.aws.sns.SNSUri;

/**
 * Tests subscribing to a topic by name and registering an SQS queue for the
 * notification by name.
 * 
 * Sample uri: sns://topicName/testTopic?queueName=testQueue
 * 
 * @author markford
 */
public class SubscribeByTopicNameAndQueueNameTest extends AbstractUseCase {

    @Test
    public void test() throws Exception {
        
        SNSUri consumer = createUri().withTopicName(mTopicName).withQueueName(mQueueName);
        SNSUri producer = createUri().withTopicName(mTopicName);
        
        SnsTester tester = new SnsTester(consumer, producer, mContext)
                .withPreStartDelay(0)
                .withPostStartDelay(POLICY_DELAY_MILLIS)
                .withAcceptedMessage("subject-1", "message body-1")
                .withAcceptedMessage("subject-2", "message body-2")
                .withPostSendDelay(OTHER_DELAY_MILLIS);
        
        doTest(tester);
    }
}
