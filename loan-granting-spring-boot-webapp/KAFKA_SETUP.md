# Kafka Integration for Credit Score Checking

## Overview

This project has been refactored to use Apache Kafka for asynchronous credit score checking:

1. **Handler** (`creditScoreChecker`): Receives external task, stores it, and sends a message to Kafka topic `credit-score-requests`
2. **Consumer** (`CreditScoreConsumer`): Listens on Kafka, processes the credit score calculation, and completes the external task back in Camunda

## Architecture

```
Camunda Process Instance
    ↓
Task "Check credit score" (External Task)
    ↓
creditScoreChecker Handler
    ├─ Stores ExternalTask in ExternalTaskStore
    ├─ Sends KafkaPayload to "credit-score-requests" topic
    └─ Returns (task NOT completed yet)
    ↓
Kafka Topic: "credit-score-requests"
    ↓
CreditScoreConsumer
    ├─ Receives KafkaPayload
    ├─ Calculates credit scores
    ├─ Retrieves stored ExternalTask
    └─ Calls externalTaskService.complete(...)
    ↓
External Task Completed in Camunda
    ↓
Process continues to SubProcess
```

## Setup

### Prerequisites

- Kafka running on `localhost:9092`
- Camunda Platform running
- SQL Server database configured

### Steps

1. **Install Kafka** (if not already running):

   ```bash
   # Download and extract Kafka
   # Start Zookeeper
   bin/zookeeper-server-start.sh config/zookeeper.properties

   # Start Kafka broker
   bin/kafka-server-start.sh config/server.properties
   ```

2. **Create Kafka Topic**:

   ```bash
   bin/kafka-topics.sh --create --topic credit-score-requests \
     --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
   ```

3. **Build and Run the Application**:

   ```bash
   mvn clean package -DskipTests
   java -jar target/loan-granting-spring-boot-webapp-0.0.1-SNAPSHOT.jar
   ```

4. **Start a Process Instance** (optional, if not using timer):
   ```bash
   curl -X POST -H "Content-Type: application/json" \
     http://localhost:8080/start-loan
   ```

## Key Components

### `KafkaPayload`

- DTO sent to Kafka containing: taskId, processInstanceId, defaultScore

### `ExternalTaskStore`

- In-memory map storing pending ExternalTask + ExternalTaskService pairs
- Keyed by taskId
- **Note**: In production, use a persistent store (database, Redis)

### `CreditScoreConsumer`

- `@KafkaListener` on topic `credit-score-requests`
- Retrieves stored task, calculates scores, and completes it in Camunda
- Handles errors by calling `externalTaskService.handleFailure(...)`

### `HandlerConfiguration` (refactored)

- `creditScoreChecker()`: Sends to Kafka instead of completing inline
- Now injected with `ExternalTaskStore` and `KafkaTemplate`

## Configuration (application.yml)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: credit-score-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
```

## Monitoring

1. **Check Kafka messages** (consume from topic):

   ```bash
   bin/kafka-console-consumer.sh --topic credit-score-requests \
     --from-beginning --bootstrap-server localhost:9092
   ```

2. **Check application logs**:
   - Handler: `External task X sent to Kafka for credit score checking`
   - Consumer: `Received credit score request: {...}`
   - Consumer: `External task X completed with credit scores: [...]`

## Production Considerations

1. **Replace ExternalTaskStore**:

   - Use a database table or Redis to persist pending tasks
   - Enables worker restart without losing tasks

2. **Error Handling**:

   - Implement dead-letter queue for failed Kafka messages
   - Add retry logic with exponential backoff

3. **Monitoring**:

   - Add metrics (Micrometer) for task completion rate
   - Monitor Kafka consumer lag

4. **Security**:
   - Enable SSL/TLS for Kafka
   - Add SASL authentication
   - Restrict topic access

## Example Workflow

1. Timer or REST endpoint triggers process start
2. Process reaches "Check credit score" external task
3. Handler stores task and sends to Kafka
4. Consumer receives, processes, and completes task
5. Process continues to SubProcess (loan decision)
6. For each score, "Grant loan" or "Reject" tasks are created
7. Process completes
