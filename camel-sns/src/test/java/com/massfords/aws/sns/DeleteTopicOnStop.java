package com.massfords.aws.sns;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.massfords.aws.sns.SNSUri;

public class DeleteTopicOnStop extends DeleteUseCase {

    @Test
    public void test() throws Exception {
        
        SNSUri consumer = createUri().withTopicName(mTopicName).withQueueName(mQueueName).withDeleteTopicOnStop(true);
        
        startAndStopRoute(consumer);

        assertTrue("Topic should have been deleted when consumer stopped", topicDeleted());
        
        assertFalse("Queue should not have been deleted when consumer stopped", queueDeleted());
    }

}
