package com.massfords.aws.sns;

import org.junit.Test;

import com.massfords.aws.sns.SNSUri;

public class SubscribeByTopicArnAndQueueNameTest extends AbstractUseCase {

    /**
     * Create a topic in advance and then start the route with a queue name. The consumer
     * will use the existing topic and will create a queue and modify its policy. 
     * 
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        
        // create the topic
        String topicArn = createTopic();

        SNSUri consumer = createUri().withTopicArn(topicArn).withQueueName(mQueueName);
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
