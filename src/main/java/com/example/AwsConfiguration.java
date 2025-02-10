package com.example;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfiguration {

    @Value("${aws-localstack-localhost}")
    private String ENDPOINT_URL;

    @Value("${aws.accessKeyId}")
    private String accessKey;

    @Value("${aws.secretKey}")
    private String secretKey;

    private static final Region DEFAULT_REGION = Region.US_EAST_1;

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(DEFAULT_REGION)
                .credentialsProvider(StaticCredentialsProvider
                        .create(AwsBasicCredentials.create(accessKey, secretKey)))
                .applyMutation(builder -> {
                    builder.endpointOverride(URI.create(ENDPOINT_URL));
                })
                .build();
    }

    @Bean
    public SesClient sesClient() {
        return SesClient.builder()
                .region(DEFAULT_REGION)
                .credentialsProvider(StaticCredentialsProvider
                        .create(AwsBasicCredentials.create(accessKey, secretKey)))
                .applyMutation(builder -> {
                    builder.endpointOverride(URI.create(ENDPOINT_URL));
                })
                .build();
    }

    @Bean
    public String notificationQueueUrl(SqsClient sqsClient) {
        return sqsClient.getQueueUrl(builder -> {
            builder.queueName("email-notification-queue");
        }).queueUrl();
    }
}
