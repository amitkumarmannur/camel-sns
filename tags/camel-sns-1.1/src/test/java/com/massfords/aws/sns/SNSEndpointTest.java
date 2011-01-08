package com.massfords.aws.sns;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.massfords.aws.sns.SNSEndpoint;

public class SNSEndpointTest {
    
    @Test
    public void testGetARN_withSlashes() throws Exception {
        String uri = "sns://arn:aws:sns:us-east-1:266383121696:final-project-topic-junit-2f4cc273-5169-45b0-828b-6a9f44c5f971?accessKey=0WF1B9HYMMT28HGSN9G2&delay=500&queueName=final-project-queue-junit-511a58a0-f47e-4245-9298-be5fdb07e3ec&secretKey=Kx7QDyjxxYg7gutC27eAd9Uf6cFCTwWnKh5XeQwq";
        String expected = "arn:aws:sns:us-east-1:266383121696:final-project-topic-junit-2f4cc273-5169-45b0-828b-6a9f44c5f971";
        String actual = SNSEndpoint.getArn(uri);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetARN_validARN() throws Exception {
        String arn = "arn:aws:sns:us-east-1:266383121696:massfordsTopic";
        String endpointURI = "sns:" + arn + "?someParam=foo";
        String actual = SNSEndpoint.getArn(endpointURI);
        assertEquals(arn, actual);
    }
    
    @Test
    public void testGetARN_createByTopic() throws Exception {
        String endpointURI = "sns:topicName/myTopic?someParam=foo";
        String actual = SNSEndpoint.getArn(endpointURI);
        assertEquals("topicName/myTopic", actual);
    }
    
    @Test
    public void testToQueueURL() throws Exception {
        String queueARN = "arn:aws:sqs:us-east-1:266383121696:final-project-queue-junit-f0842ce2-6896-4df7-905e-f22efa401878";
        String expectedURL = "https://queue.amazonaws.com/266383121696/final-project-queue-junit-f0842ce2-6896-4df7-905e-f22efa401878";
        
        String actual = SNSEndpoint.toQueueURL(queueARN);
        assertEquals(expectedURL, actual);
    }
    
    @Test
    public void stripCreds() throws Exception {
        String endpointURI = "sns:topicName/myTopic?someParam=foo&accessKey=SOMEACCESSKEY&secretKey=SomeAcessKey";
        String expected = "sns:topicName/myTopic?someParam=foo&accessKey=hidden&secretKey=hidden";
        String actual = SNSEndpoint.stripCredentials(endpointURI);
        assertEquals(expected, actual);
    }
}
