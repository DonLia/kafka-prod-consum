# Kafka Credit Consumer

This is a standalone Spring Boot microservice that consumes credit score requests from Kafka topics and processes them asynchronously.

## Overview

This consumer service:
- Listens to the `credit-score-requests` Kafka topic
- Processes credit score calculation logic
- Completes external tasks in the Camunda workflow engine

## Architecture

This service is designed to work alongside the `loan-granting-spring-boot-webapp`:
- The webapp produces credit score requests to Kafka
- This consumer processes them asynchronously
- The consumer completes the external tasks via Camunda's REST API

## Prerequisites

- Java 17 or higher
- Apache Kafka running on `localhost:9092`
- Camunda Platform running on `localhost:8080`

## Configuration

Edit `src/main/resources/application.yml` to configure:
- Kafka bootstrap servers
- Camunda engine REST API URL
- Consumer group ID

## Running

```bash
mvn clean install
mvn spring-boot:run
```

Or run the packaged JAR:
```bash
java -jar target/kafka-credit-consumer-0.0.1-SNAPSHOT.jar
```

## How It Works

1. The consumer listens to Kafka topic `credit-score-requests`
2. When a message arrives with task details
3. It calculates credit scores (simulated logic)
4. It completes the corresponding external task in Camunda via REST API
5. The workflow in the webapp continues with the results
