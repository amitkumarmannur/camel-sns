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

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Before;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.DeleteTopicRequest;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;

// FIXME need test to show that the unsubscribe call is happening.

public abstract class AbstractUseCase {
    protected static final String MOCK_SINK = "mock:sink";
    static final long POLICY_DELAY_MILLIS = 120000;
    static final long OTHER_DELAY_MILLIS = 10000;
    
    DefaultCamelContext mContext = new DefaultCamelContext();
    AmazonSNSClient mClient;
    AmazonSQSClient mQClient;
    String mTopicName = "final-project-topic-junit-" + UUID.randomUUID().toString();
    String mQueueName = "final-project-queue-junit-" + UUID.randomUUID().toString();
    AWSCredentials mCredentials;
    
    String mQueueURL;
    String mTopicArn;

    SnsTester mTester;

    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("/awscreds.properties"));
        String accessKey = props.getProperty("accessKey");
        String secretKey = props.getProperty("secretKey");
        // if the keys aren't found look in the environment
        if ("${env.accessKey}".equals(accessKey)) {
            accessKey = System.getenv("accessKey");
            secretKey = System.getenv("secretKey");
        }
        assertNotNull("accessKey must be provided as an environment variable or configured in awscreds.properties", accessKey);
        assertNotNull("secretKey must be provided as an environment variable or configured in awscreds.properties", secretKey);
        mCredentials = new BasicAWSCredentials(accessKey, secretKey);
        mClient = AmazonClientFactory.createSNSClient(mCredentials);
        mQClient = AmazonClientFactory.createSQSClient(mCredentials);
        assertNotNull(getClass().getResource("/META-INF/services/org/apache/camel/component/sns"));
        PropertyConfigurator.configure(getClass().getResource("/log4j.properties"));
    }
    
    @After
    public void tearDown() throws Exception {
        
        if (!mContext.isStopped()) {
            mContext.stop();
        }
        
        if (mTester == null) {
            return;
        }
        
        // delete all topics
        System.out.println("deleting topic created for test");
        if (mTopicArn != null) {
            try {
                System.out.println("deleting topic:" + mTopicArn);
                mClient.deleteTopic(new DeleteTopicRequest().withTopicArn(mTopicArn));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // delete all queues
        System.out.println("deleting queue created for test");
        if(mQueueURL != null) {
            try {
                System.out.println("deleting q:" + mQueueURL);
                mQClient.deleteQueue(new DeleteQueueRequest().withQueueUrl(mQueueURL));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // FIXME need test to show that the unsubscribe call is happening.

    protected String createQueue() throws IOException {
        CreateQueueResult result = mQClient.createQueue(new CreateQueueRequest().withQueueName(mQueueName));
        String queueUrl = result.getQueueUrl();
        mQueueURL = queueUrl;
        String queueArn = SnsConsumer.getQueueArn(mQClient, queueUrl);
        
        String policyStr = IOUtils.toString(getClass().getResourceAsStream("/open-sqs-policy-template.json"));
        policyStr = policyStr.replace("$SQS_ARN", queueArn);
        
        Map<String,String> attribs = new HashMap();
        attribs.put("Policy", policyStr);
        mQClient.setQueueAttributes(new SetQueueAttributesRequest().withQueueUrl(queueUrl).withAttributes(attribs));
        return queueArn;
    }

    protected String createTopic() {
        mTopicArn = mClient.createTopic(new CreateTopicRequest().withName(mTopicName)).getTopicArn();
        return mTopicArn;
    }

    protected void setTester(SnsTester aTester) {
        mTester = aTester;
    }
    
    protected SnsUri createUri() {
        SnsUri uri = new SnsUri(mCredentials);
        return uri;
    }

    protected void doTest(SnsTester aTester) throws Exception {
        setTester(aTester);
        aTester.send();
        
        SnsEndpoint snsEndpoint = aTester.getConsumerEndpoint();
        mTopicArn = snsEndpoint.getTopicArn();
        mQueueURL = snsEndpoint.getQueueURL();

        aTester.getMockEndpoint().assertIsSatisfied();
        mContext.stop();
    }
}
