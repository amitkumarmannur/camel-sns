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

import org.junit.Test;

public class SubscribeByTopicArnAndQueueNameIntegrationTest extends AbstractUseCase {

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

        SnsUri consumer = createUri().withTopicArn(topicArn).withQueueName(mQueueName);
        SnsUri producer = createUri().withTopicName(mTopicName);
        
        SnsTester tester = new SnsTester(consumer, producer, mContext)
                .withPreStartDelay(0)
                .withPostStartDelay(POLICY_DELAY_MILLIS)
                .withAcceptedMessage("subject-1", "message body-1")
                .withAcceptedMessage("subject-2", "message body-2")
                .withPostSendDelay(OTHER_DELAY_MILLIS);
        
        doTest(tester);
    }
}
