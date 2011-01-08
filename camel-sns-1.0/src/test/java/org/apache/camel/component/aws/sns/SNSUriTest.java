/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.aws.sns;

import static org.junit.Assert.assertEquals;

import org.apache.camel.component.aws.sns.SNSUri;
import org.junit.Test;

import com.amazonaws.auth.BasicAWSCredentials;

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
