package org.camunda.kafka.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Kafka Credit Score Consumer Application
 * This microservice consumes credit score requests from Kafka,
 * processes them, and completes the corresponding external tasks in Camunda.
 */
@SpringBootApplication
@EnableKafka
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
