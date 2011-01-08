package com.massfords.aws.sns;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.amazonaws.auth.BasicAWSCredentials;
import com.massfords.aws.sns.SNSUri;

public class SNSUriTest {
    @Test
    public void testTopicName_noExtraParams() {
        SNSUri uri = new SNSUri(new BasicAWSCredentials("1234", "abcd")).withTopicName("topic123");
        
        assertEquals("sns:topicName/topic123?accessKey=1234&secretKey=abcd&delay=2000&idempotent=true", uri.toString());
    }
    
    @Test
    public void testTopicName_someExtraParams() {
        SNSUri uri = new SNSUri(new BasicAWSCredentials("1234", "abcd")).withTopicName("topic123").withDeleteQueueOnStop(true);
        
        assertEquals("sns:topicName/topic123?accessKey=1234&secretKey=abcd&delay=2000&idempotent=true&deleteQueueOnStop=true", uri.toString());
    }

    @Test
    public void testTopicArn() {
        SNSUri uri = new SNSUri(new BasicAWSCredentials("1234", "abcd")).withTopicArn("arn:aws:sns:1234:5678");
        
        assertEquals("sns:arn:aws:sns:1234:5678?accessKey=1234&secretKey=abcd&delay=2000&idempotent=true", uri.toString());
    }
}
