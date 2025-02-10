AWS Messaging: Spring Boot on LocalStack with CloudFormation IOC
========================================

This sample Spring Boot application project demonstrates how to: 

* Provision CloudFormation infrastructure on LocalStack
* Configure SNS SQS subscriptions with CloudFormation
* Receive SQS messages with the AWS Java SDK
* Send SES message with the AWS Java SDK

## About the project

* Java 17+
* Maven 3+
* LocalStack
* LocalStack AWS CLI

## Application Instructions

### Build the application

The application is a simple Spring Boot application that you can build by running

    mvn clean install

### Spin up the infrastructure on localstack

Resources are deployed via a CloudFormation template in `src/main/resources/email-infra.yml`.

Start LocalStack and the SMTP server using localstack [token](https://app.localstack.cloud/workspace/auth-tokens):

    LOCALSTACK_AUTH_TOKEN=<localstack-auth-token> docker-compose up -d

Deploy cloudformation stack

    aws --endpoint-url=http://localhost:4566 cloudformation deploy \
        --template-file src/main/resources/email-infra.yml \
        --stack-name email-infra

### Test the application

Verify the sender email address configured in the app

    aws --endpoint-url=http://localhost:4566 ses verify-email-identity --email-address no-reply@localstack.cloud

Send a message to the topic

    aws --endpoint-url=http://localhost:4566 sns publish \
        --topic arn:aws:sns:us-east-1:000000000000:email-notifications \
        --message '{"subject":"test", "address": "test-localstack@example.com", "body": "test 123"}'

Run `notifications/list` endpoint for queued messages.

Run the `notifications/process` endpoint to send the queued notifications as emails

Verify that the email has been sent:

* Check MailHog via the UI http://localhost:8025/

Reference: https://docs.localstack.cloud/tutorials/java-notification-app/


