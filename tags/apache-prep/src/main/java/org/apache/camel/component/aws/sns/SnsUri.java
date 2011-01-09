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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.auth.AWSCredentials;

public class SnsUri {
    /** query params */
    Map<String,String> mQueryProps = new LinkedHashMap();
    /** topic arn that we're targeting */
    String mTopicArn;
    
    public SnsUri(SnsUri aUri) {
        mQueryProps.putAll(aUri.mQueryProps);
        mTopicArn = aUri.mTopicArn;
    }
    
    public SnsUri(AWSCredentials aCredentials) {
        addProperty("accessKey", aCredentials.getAWSAccessKeyId());
        addProperty("secretKey", aCredentials.getAWSSecretKey());
        addProperty("delay", "2000");
        addProperty("idempotent", "true");
    }
    
    public SnsUri withQueueName(String aQueueName) {
        addProperty("queueName", aQueueName);
        return this;
    }
    
    public SnsUri withQueueArn(String aQueueArn) {
        addProperty("queueArn", aQueueArn);
        return this;
    }
    
    public SnsUri withDeleteQueueOnStop(boolean aDeleteQueueOnStop) {
        addProperty("deleteQueueOnStop", String.valueOf(aDeleteQueueOnStop));
        return this;
    }

    public SnsUri withDeleteTopicOnStop(boolean aDeleteTopicOnStop) {
        addProperty("deleteTopicOnStop", String.valueOf(aDeleteTopicOnStop));
        return this;
    }

    public SnsUri withDelay(long aMillis) {
        addProperty("delay", String.valueOf(aMillis));
        return this;
    }

    public void addProperty(String aName, String aValue) {
        mQueryProps.put(aName, aValue);
    }
    
    public SnsUri withProperty(String aName, String aValue) {
        addProperty(aName, aValue);
        return this;
    }
    
    public SnsUri withTopicName(String aTopicName) {
        setTopicName(aTopicName);
        return this;
    }
    
    public void setTopicName(String aTopicName) {
        mTopicArn = "topicName/" + aTopicName;
    }
    
    public void setTopicArn(String aTopicArn) {
        mTopicArn = aTopicArn;
    }
    
    public SnsUri withTopicArn(String aTopicArn) {
        setTopicArn(aTopicArn);
        return this;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder("sns:").append(mTopicArn);
        if (!mQueryProps.isEmpty()) {
            sb.append('?');
            
            String delim = "";
            for(Entry<String, String> entry : mQueryProps.entrySet()) {
                sb.append(delim);
                sb.append(entry.getKey()).append('=').append(entry.getValue());
                delim = "&";
            }
        }
        return sb.toString();
    }
}
