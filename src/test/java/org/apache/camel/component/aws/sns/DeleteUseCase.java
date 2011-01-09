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

import java.util.HashSet;
import java.util.Set;

import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Topic;
import com.amazonaws.services.sqs.model.ListQueuesRequest;
import com.amazonaws.services.sqs.model.ListQueuesResult;

public abstract class DeleteUseCase extends AbstractUseCase {

    protected Set<String> listTopicArns() {
        ListTopicsResult result = mClient.listTopics();
        Set<String> topicArns = new HashSet();
        for(Topic t : result.getTopics()) {
            topicArns.add(t.getTopicArn());
        }
        return topicArns;
    }

    protected ListQueuesResult listQueues() {
        ListQueuesResult qResult = mQClient.listQueues(new ListQueuesRequest().withQueueNamePrefix(mQueueName));
        return qResult;
    }

    protected boolean queueDeleted() {
        ListQueuesResult qResult = listQueues();
        return qResult.getQueueUrls().isEmpty();
    }

    protected boolean topicDeleted() {
        Set<String> topicArns = listTopicArns();
        return !topicArns.contains(mTopicArn);
    }

    protected void startAndStopRoute(SnsUri aConsumer) throws Exception {
        SnsTester tester = new SnsTester(aConsumer, null, mContext);

        doTest(tester);

        Thread.sleep(2000);
    }
}
