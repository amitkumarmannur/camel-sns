package com.massfords.aws.sns;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.language.XPathExpression;
import org.apache.log4j.PropertyConfigurator;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;

public class SnsDemo {
    
    private AWSCredentials mCredentials;
    
    public SnsDemo(AWSCredentials aCredentials) {
        mCredentials = aCredentials;
    }
    
    public void runDemo(final File aInputDir, final File aOutputDir) throws Exception {
        
        final String outputFilesUri = aOutputDir.getAbsoluteFile().toURI().toString();
        final String inputFilesUri = aInputDir.getAbsoluteFile().toURI() + "?noop=true&initialDelay=2000";

        final SNSUri toEndpoint = new SNSUri(mCredentials)
            .withTopicName("final-project-topic");
        
        final SNSUri fromEndpoint_noFilter = new SNSUri(mCredentials)
            .withTopicName("final-project-topic")
            .withQueueName("final-project-queue-noFilter");

        final SNSUri fromEndpoint_filterHeader = new SNSUri(mCredentials)
        .withTopicName("final-project-topic")
        .withQueueName("final-project-queue-filterHeader");

        final SNSUri fromEndpoint_filterBody = new SNSUri(mCredentials)
        .withTopicName("final-project-topic")
        .withQueueName("final-project-queue-filterBody");

        CamelContext context = new DefaultCamelContext();
        context.addRoutes(new RouteBuilder() {

            public void configure() throws Exception {
                
                // setup route to read files from directory and place on topic
                from(inputFilesUri)
                    .process(new FilenameAsSubject())
                    .to(toEndpoint.toString());
                
                // read w/o any filters
                from(fromEndpoint_noFilter.toString())
                    .process(new SubjectAsFilename("no-filter-"))
                    .to(outputFilesUri);

                // read with a filter on the header
                from(fromEndpoint_filterHeader.toString())
                    .filter(header("SNS:Subject")
                            .isEqualTo("message-1.xml"))
                    .process(new SubjectAsFilename("header-filter-"))
                    .to(outputFilesUri);

                // read with a filter on the body
                from(fromEndpoint_filterBody.toString())
                    .filter(new XPathExpression("//Priority = 'Rush'"))
                    .process(new SubjectAsFilename("body-filter-"))
                    .to(outputFilesUri);
            }
        });
        
        context.start();
        
        waitForQuitSignal();
        
        context.stop();
        
        System.out.println("End");
    }

    private void waitForQuitSignal() throws IOException {
        System.out.println("Type [Qq] to quit");
        
        byte[] b = new byte[1];
        while(true) {
            System.in.read(b);
            if (b[0] == 'Q' || b[0] == 'q')
                break;
        }
    }
    
    /**
     * Custom processor that uses the subject from the SNS notification as the filename
     * 
     * @author markford
     */
    private static class SubjectAsFilename implements Processor {
        
        private String mPrefix;
        
        public SubjectAsFilename(String aPrefix) {
            mPrefix = aPrefix;
        }

        public void process(Exchange aExchange) throws Exception {
            String filename = (String) aExchange.getIn().getHeader("SNS:Subject");
            aExchange.getIn().setHeader("camelfilename", mPrefix + filename);
        }
    }
    
    /**
     * Custom processor that uses the filename as the subject of the notification
     * 
     * @author markford
     */
    private static class FilenameAsSubject implements Processor {

        public void process(Exchange aExchange) throws Exception {
            String filename = (String) aExchange.getIn().getHeader("camelfilenameonly");
            aExchange.getIn().setHeader("SNS:Subject", filename);
        }
    }

    public static void main(String[] args) throws Exception {
        
        PropertyConfigurator.configure(SnsDemo.class.getResource("/log4j.properties"));
        
        Map<String,String> props = new HashMap();
        for(int i=0; i<args.length; i+=2) {
            String key = args[i];
            String value = args[i+1];
            props.put(key, value);
        }
        
        String accessKey = props.get("--access-key");
        String secretKey = props.get("--secret-key");
        
        SnsDemo demo = new SnsDemo(new BasicAWSCredentials(accessKey, secretKey));
        demo.runDemo(new File("inputFiles"), new File("outputFiles"));
    }
}
