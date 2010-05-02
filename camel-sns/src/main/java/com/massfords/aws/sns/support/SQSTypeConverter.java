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

package com.massfords.aws.sns.support;

import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

/**
 * SQS Type Converter is a strategy that converts objects to 
 * and from {@link SQSObject}s.
 * 
 */
@Converter
public class SQSTypeConverter {
	private static final transient Log LOG = LogFactory
			.getLog(SQSTypeConverter.class);

	@Converter
	public static String toString(SQSObject value, Exchange exchange) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("converting sqsObject to String: " + value);
		}
		String retValue = value.toString();
		return retValue;
	}

	@Converter
	public static SQSObject toSQSObject(String value, Exchange exchange) throws Exception {
		return toSQSObject(value);
	}

    public static SQSObject toSQSObject(String aMessage) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("raw SQSMessage: " + aMessage.toString());
		}
		
		// FIXME replace with commons-io
		byte[] decoded = Base64.decodeBase64(aMessage);
		JSONObject json = new JSONObject(new String(decoded, "UTF-8"));
		
		SQSObject sqsObject = new SQSObject(json);

		return sqsObject;
	}

}
