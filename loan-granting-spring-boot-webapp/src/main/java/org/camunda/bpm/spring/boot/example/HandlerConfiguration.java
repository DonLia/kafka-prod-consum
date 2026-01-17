/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.spring.boot.example;

import org.camunda.bpm.client.spring.SpringTopicSubscription;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.spring.boot.starter.ClientProperties;
import org.camunda.bpm.client.spring.event.SubscriptionInitializedEvent;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class HandlerConfiguration {

  protected static Logger LOG = LoggerFactory.getLogger(HandlerConfiguration.class);

  protected String workerId;

  private final KafkaTemplate<String, KafkaPayload> kafkaTemplate;

  public HandlerConfiguration(ClientProperties properties,
      KafkaTemplate<String, KafkaPayload> kafkaTemplate) {
    workerId = properties.getWorkerId();
    this.kafkaTemplate = kafkaTemplate;
  }

  @ExternalTaskSubscription("creditScoreChecker")
  @Bean
  public ExternalTaskHandler creditScoreChecker() {
    return (externalTask, externalTaskService) -> {

      // retrieve a variable from the Process Engine
      int defaultScore = externalTask.getVariable("defaultScore");

      // Get task ID
      String taskId = externalTask.getId();

      // Create Kafka payload and send task ID to Kafka topic
      KafkaPayload payload = new KafkaPayload(
          taskId,
          externalTask.getProcessInstanceId(),
          defaultScore);

      kafkaTemplate.send("credit-score-requests", taskId, payload);

      LOG.info("{}: External task {} sent to Kafka", workerId, taskId);

      // Extend lock to give consumer time to process and complete
      // This keeps the task locked by THIS worker but gives consumer time
      externalTaskService.extendLock(externalTask, 300000); // Extend by 5 minutes

      LOG.info("{}: Extended lock for task {} - Kafka consumer will complete it", workerId, taskId);
    };
  }

  @ExternalTaskSubscription("loanGranter")
  @Bean
  public ExternalTaskHandler loanGranter() {
    return (externalTask, externalTaskService) -> {
      int score = externalTask.getVariable("score");
      externalTaskService.complete(externalTask);

      LOG.info("{}: The External Task {} has been granted with score {}!", workerId, externalTask.getId(), score);

    };
  }

  @ExternalTaskSubscription("requestRejecter")
  @Bean
  public ExternalTaskHandler requestRejecter() {
    return (externalTask, externalTaskService) -> {
      int score = externalTask.getVariable("score");
      externalTaskService.complete(externalTask);

      LOG.info("{}: The External Task {} has been rejected with score {}!", workerId, externalTask.getId(), score);

    };
  }

  @EventListener(SubscriptionInitializedEvent.class)
  public void catchSubscriptionInitEvent(SubscriptionInitializedEvent event) {

    SpringTopicSubscription topicSubscription = event.getSource();
    if (!topicSubscription.isAutoOpen()) {

      // open topic in case it is not opened already
      topicSubscription.open();

      LOG.info("Subscription with topic name '{}' has been opened!",
          topicSubscription.getTopicName());
    }
  }

}
