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
package org.apache.camel.component.aws.sns.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

public class SQSObject {
    
    Log sLog = LogFactory.getLog(SQSObject.class);
    
    JSONObject mJO;
    
    public SQSObject(JSONObject aJson) {
        mJO = aJson;
    }
    public String getMessageId() {
        return getString("MessageId");
    }
    public String getTimestamp() {
        return getString("Timestamp");
    }
    public String getTopicArn() {
        return getString("TopicArn");
    }
    public String getType() {
        return getString("Type");
    }
    public String getUnsubscribe() {
        return getString("Unsubscribe");
    }
    public String getMessage() {
        return getString("Message");
    }
    public String getSubject() {
        return getString("Subject");
    }
    public String getSignature() {
        return getString("Signature");
    }
    public String getSignatureVersion() {
        return getString("SignatureVersion");
    }
    public String toString() {
        try {
            return mJO.toString(3);
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }
    public String getString(String prop) {
        try {
            return mJO.getString(prop);
        } catch (JSONException e) {
            sLog.trace("field not found", e);
            return "";
        }
    }
}
