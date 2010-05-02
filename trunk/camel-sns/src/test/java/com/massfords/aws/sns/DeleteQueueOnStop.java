package com.massfords.aws.sns;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.massfords.aws.sns.SNSUri;

public class DeleteQueueOnStop extends DeleteUseCase {

    @Test
    public void test() throws Exception {
        
        SNSUri consumer = createUri().withTopicName(mTopicName).withQueueName(mQueueName).withDeleteQueueOnStop(true);

        startAndStopRoute(consumer);

        assertFalse("Topic should not have been deleted when consumer stopped", topicDeleted());
        
        assertTrue("Queue should have been deleted when consumer stopped", queueDeleted());
    }
}
