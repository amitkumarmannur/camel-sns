package com.massfords.aws.sns.support;

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
