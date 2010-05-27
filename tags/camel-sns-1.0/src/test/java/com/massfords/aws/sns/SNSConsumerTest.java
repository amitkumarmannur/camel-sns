package com.massfords.aws.sns;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Test;

import com.massfords.aws.sns.support.SQSObject;

public class SNSConsumerTest {
    
    @Test
    public void testVerify() throws Exception {
        String validMessageJson = IOUtils.toString(getClass().getResourceAsStream("/valid-message.json"));
        JSONObject json = new JSONObject(validMessageJson);
        SQSObject sqsObject = new SQSObject(json);
        
        assertTrue("Expected verification to pass since this message was valid", SNSConsumer.verifyMessage(sqsObject));
    }
    
    @Test
    public void testVerify_fails() throws Exception {
        String validMessageJson = IOUtils.toString(getClass().getResourceAsStream("/invalid-message.json"));
        JSONObject json = new JSONObject(validMessageJson);
        
        // introduce a change in the message
        SQSObject sqsObject = new SQSObject(json);
        
        assertFalse("Expected sig verification to fail since we tweaked the message", SNSConsumer.verifyMessage(sqsObject));
        
    }
}
