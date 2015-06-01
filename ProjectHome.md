# SNS Component #
The SNS component allows messages to be sent to or consumed from an Amazon Simple Notification Topic. The implementation of the Amazon API is provided by the [AWS SDK](http://aws.amazon.com/sdkforjava/).

# URI Format #
```
sns://topicName/yourTopicName?options
```
The "topicName/" path is a flag that the value following the slash is a topic name that should be created or one that already exists. Alternatively, you can access existing topics by their Amazon Resource Name (ARN) as follows:

```
sns://arn:aws:sns:us-east-1:123456789012:yourTopicName?options
```

You can append query options to the URI in the following format, ?options=value&option2=value&...

# Endpoint Properties #
## Common Properties ##
| **Property** | **Required** | **Default** | **Description** |
|:-------------|:-------------|:------------|:----------------|
| accessKey    | yes          | -           | AWS access key  |
| secretKey    | yes          | -           | AWS secret key  |
| deleteTopicOnStop | -            | false       | Deletes the topic when the component stops |

## Consumer Only Properties ##
| **Property** | **Required** | **Default** | **Description** |
|:-------------|:-------------|:------------|:----------------|
| queueName    | -            | -           | Name of the queue to use for the callback |
| queueArn     | -            | -           | SQS ARN to use for the callback |
| idempotent   | -            | false       | If true, consumer will avoid processing messages that it has already processed |
| delay        | -            | 500         | Frequency of polling on the queue to check for new messages |
| deleteQueueOnStop | -            | false       | Deletes the queue when the component stops |
| verify       | -            | false       | Verifies each of the messages using the signature in the message. Invalid messages are deleted |

## Producer Only Properties ##
| **Property** | **Required** | **Default** | **Description** |
|:-------------|:-------------|:------------|:----------------|
| subject      | -            | -           | default subject to use for the message being sent if not overridden in the camel message header with "SNS:Subject"|

# Consumer Use Cases #

## Subscribing to a topic ##
| **Topic** | **Subscription Endpoint** | **Test Case** |
|:----------|:--------------------------|:--------------|
| topic name | SQS name                  | [SubscribeByTopicNameAndQueueNameTest.java](http://code.google.com/p/camel-sns/source/browse/trunk/camel-sns/src/test/java/com/massfords/aws/sns/SubscribeByTopicNameAndQueueNameTest.java) |
| topic name | SQS ARN                   | [SubscribeByTopicNameAndQueueArnTest.java](http://code.google.com/p/camel-sns/source/browse/trunk/camel-sns/src/test/java/com/massfords/aws/sns/SubscribeByTopicNameAndQueueArnTest.java) |
| topic ARN | SQS name                  | [SubscribeByTopicArnAndQueueNameTest.java](http://code.google.com/p/camel-sns/source/browse/trunk/camel-sns/src/test/java/com/massfords/aws/sns/SubscribeByTopicArnAndQueueNameTest.java) |
| topic ARN | SQS ARN                   | [SubscribeByTopicArnAndQueueArnTest.java](http://code.google.com/p/camel-sns/source/browse/trunk/camel-sns/src/test/java/com/massfords/aws/sns/SubscribeByTopicArnAndQueueArnTest.java) |

### Topic Permissions ###
If the topic is referenced by its name then the caller is assumed to be the owner of the topic. The create topic API call is used to create the topic by name or return the topic if it already exists.

If the topic is referenced by its Amazon Resource Name (ARN) then it is assumed that the caller has access to subscribe to the topic.

### Queue Permissions ###
The only supported subscriber endpoint at the moment is an Amazon Simple Queue Service (SQS). If the queue is referenced by its name, then the caller is assumed to be the owner of the queue. The create queue API call is used to create the queue by name or return the queue if it already exists. When the queue is created, its default policy is overridden to allow the SNS ARN to send messages to the queue.

Note that the documentation for SQS states that the policy may take up to **60 seconds** to propagate although in practice it appears to be taking over **90 seconds**. The result is that the SQS endpoint will not begin receiving messages until the policy is propagated. The unit tests that alter the policy of a queue sleep for 2 minutes to ensure that the policy has been applied. This will be revisited when the SQS service changes.

If the queue is references by its ARN then it is assumed that the queue exists and has sufficient permissions on it to receive messages from the topic.

## Unsubscribing ##
The consumer will automatically unsubscribe from the topic once it is stopped.

## Topic/Queue deletion ##
The consumer will optionally delete the topic or queue when it is stopped. See the above for info on controlling this and other behavior with endpoint properties. See [DeleteQueueOnStopTest.java](http://code.google.com/p/camel-sns/source/browse/trunk/camel-sns/src/test/java/com/massfords/aws/sns/DeleteQueueOnStopTest.java) and [DeleteTopicOnStopTest.java](http://code.google.com/p/camel-sns/source/browse/trunk/camel-sns/src/test/java/com/massfords/aws/sns/DeleteTopicOnStopTest.java) for examples.

## Message Mapping between SNS and Camel ##
The component will unwrap the message payload from its JSON format and put the message field into the camel message payload and add all of the other fields as headers prefixed with "SNS:". The available properties on the message are as follow:

|SNS:MessageId| A Universally Unique Identifier, unique for each notification published|
|:------------|:-----------------------------------------------------------------------|
|SNS:Timestamp| The time (in GMT) at which the notification was published.             |
|SNS:TopicArn | The topic to which this message was published                          |
|SNS:Type     | The type of the delivery message, set to “Notification” for notification deliveries.|
|SNS:UnsubscribeURL| A link to unsubscribe the end-point from this topic, and prevent receiving any further notifications.|
|SNS:Message  | The payload (body) of the message, as received from the publisher      |
|SNS:Subject  | The Subject field – if one was included as an optional parameter to the publish API call along with the message.|
|SNS:Signature| Base64-encoded “SHA1withRSA” signature of the Message, MessageId, Subject (if present), Type, Timestamp, and Topic values.|
|SNS:SignatureVersion| Version of the Amazon SNS signature used.                              |

These properties can be used to filter messages in the route. See [FilterBySubjectTest.java](http://code.google.com/p/camel-sns/source/browse/trunk/camel-sns/src/test/java/com/massfords/aws/sns/FilterBySubjectTest.java) for an example of filtering messages by subject.

# Producer Use Cases #
  * Publish by topic name
  * Publish by topic ARN

# AWS Unit Tests #
The unit tests that invoke the AWS services require valid credentials in order to work. These credentials are set in a file in the test directory (src/test/resources/awscreds.properties). Either modify this file to contain the correct accessKey and secretKey or pass these values in via the command line or export as system properties.

awscreds.properties

```
accessKey=${env.accessKey}
secretKey=${env.secretKey}
```

Sample command line:

```
mvn clean install -DaccessKey=YOUR-ACCESS-KEY -DsecreteKey=YOUR-SECRET-KEY
```
