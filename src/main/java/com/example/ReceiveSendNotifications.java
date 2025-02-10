package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@Component
public class ReceiveSendNotifications {

    private static final Logger LOG = LoggerFactory.getLogger(ReceiveSendNotifications.class);

    private static final String SOURCE_EMAIL = "no-reply@localstack.cloud";

    @Autowired
    private SqsClient sqsClient;

    @Autowired
    private SesClient sesClient;

    @Autowired
    private String notificationQueueUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<String> processNotifications() {
        ReceiveMessageResponse receiveMessageResponse = sqsClient.receiveMessage(
                request -> request.queueUrl(notificationQueueUrl).maxNumberOfMessages(10)
        );

        if (!receiveMessageResponse.hasMessages()) {
            return Collections.emptyList();
        }

        List<Message> sqsMessages = receiveMessageResponse.messages();
        List<Notification> notificationsToSend = new ArrayList<>(sqsMessages.size());
        List<String> notificationReceipts = new ArrayList<>(sqsMessages.size());
        for (Message message : sqsMessages) {
            String body = message.body();

            try {
                HashMap snsEvent = objectMapper.readValue(body, HashMap.class);
                LOG.info("processing snsEvent {}", snsEvent);
                String notificationString = snsEvent.get("Message").toString();
                Notification notification = objectMapper.readValue(notificationString, Notification.class);
                notificationsToSend.add(notification);
                notificationReceipts.add(message.receiptHandle());
            } catch (JsonProcessingException e) {
                LOG.error("error processing message body {}. Send to DLQ", body, e);
            }
        }

        List<String> sentMessages = new ArrayList<>();
        for (int i = 0; i < notificationsToSend.size(); i++) {
            Notification notification = notificationsToSend.get(i);
            String receiptHandle = notificationReceipts.get(i);

            try {
                String messageId = sendNotificationAsEmail(notification);
                LOG.info("successfully sent notification as email, message id = {}", messageId);
                sentMessages.add(messageId);
            } catch (Exception e) {
                LOG.error("could not send notification as email {}", notification, e);
                continue;
            }

            sqsClient.deleteMessage(builder -> {
                builder.queueUrl(notificationQueueUrl).receiptHandle(receiptHandle);
            });
        }

        return sentMessages;
    }

    public String sendNotificationAsEmail(Notification notification) {
        return sesClient.sendEmail(notificationToEmail(notification)).messageId();
    }

    public SendEmailRequest notificationToEmail(Notification notification) {
        return SendEmailRequest.builder().applyMutation(email -> {
            email.message(msg -> {
                msg.body(body -> {
                    body.text(text -> {
                        text.data(notification.getBody());
                    });
                }).subject(subject -> {
                    subject.data(notification.getSubject());
                });
            }).destination(dest -> {
                dest.toAddresses(notification.getAddress());
            }).source(SOURCE_EMAIL);
        }).build();
    }

    public List<HashMap<String, String>> listMessages() {
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(notificationQueueUrl)
                .visibilityTimeout(0)
                .maxNumberOfMessages(10)
                .build();

        ReceiveMessageResponse receiveMessageResponse = sqsClient.receiveMessage(receiveRequest);
        if (!receiveMessageResponse.hasMessages()) {
            return Collections.emptyList();
        }
        return receiveMessageResponse.messages().stream().map(Message::body).map(str -> {
            try {
                return (HashMap<String, String>) objectMapper.readValue(str, HashMap.class);
            } catch (JsonProcessingException e) {
                LOG.error("error processing message body {}", str, e);
                HashMap<String, String> map = new HashMap<>();
                map.put("body", str);
                return map;
            }
        }).collect(Collectors.toList());
    }

    public void purgeQueue() {
        sqsClient.purgeQueue(builder -> {
            builder.queueUrl(notificationQueueUrl);
        });
    }
}