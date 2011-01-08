package com.massfords.aws.sns;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQSClient;

/**
 * Factory for creating client instances. Handles the application of the proxy values in case you're running behind a
 * proxy server. The standard environment variables http.proxyHost and http.proxyPort are consulted.
 * 
 * @author markford
 */
public class AmazonClientFactory {
    
    private static Log sLog = LogFactory.getLog(AmazonClientFactory.class);
    
    public static AmazonSNSClient createSNSClient(AWSCredentials aCredentials) {
        return createSNSClient(aCredentials.getAWSAccessKeyId(), aCredentials.getAWSSecretKey());
    }

    public static AmazonSNSClient createSNSClient(String aAccessKey, String aSecretKey) {
        AmazonSNSClient client = new AmazonSNSClient(new BasicAWSCredentials(aAccessKey, aSecretKey), new ClientConfiguration().withProxyHost(getProxyHost()).withProxyPort(getProxyPort()));
        return client;
    }
    
    public static AmazonSQSClient createSQSClient(AWSCredentials aCredentials) {
        return createSQSClient(aCredentials.getAWSAccessKeyId(), aCredentials.getAWSSecretKey());
    }

    public static AmazonSQSClient createSQSClient(String aAccessKey, String aSecretKey) {
        AmazonSQSClient client = new AmazonSQSClient(new BasicAWSCredentials(aAccessKey, aSecretKey), new ClientConfiguration().withProxyHost(getProxyHost()).withProxyPort(getProxyPort()));
        return client;
    }
    
    private static String getProxyHost() {
        String proxyHost = System.getProperty("http.proxyHost");
        if (sLog.isTraceEnabled())
            sLog.trace("proxy host=" + proxyHost);
        return proxyHost;
    }
    
    private static int getProxyPort() {
        String port = System.getProperty("http.proxyPort");
        if (port == null || port.length() == 0)
            return -1;
        if (sLog.isTraceEnabled())
            sLog.trace("proxy port=" + port);
        return Integer.parseInt(port);
    }
}
