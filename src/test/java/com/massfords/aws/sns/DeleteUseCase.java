package com.massfords.aws.sns;

import java.util.HashSet;
import java.util.Set;

import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Topic;
import com.amazonaws.services.sqs.model.ListQueuesRequest;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.massfords.aws.sns.SNSUri;

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

    protected void startAndStopRoute(SNSUri aConsumer) throws Exception {
        SnsTester tester = new SnsTester(aConsumer, null, mContext);

        doTest(tester);

        Thread.sleep(2000);
    }
}
