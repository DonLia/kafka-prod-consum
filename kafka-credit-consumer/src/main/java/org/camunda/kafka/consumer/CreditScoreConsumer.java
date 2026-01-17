package org.camunda.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Kafka consumer that processes credit score checking requests
 * and completes the external task via Camunda REST API.
 */
@Service
public class CreditScoreConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(CreditScoreConsumer.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${camunda.rest.base-url}")
    private String camundaBaseUrl;

    /**
     * Listens to "credit-score-requests" topic and processes credit score calculation.
     * Completes the external task via REST API.
     */
    @KafkaListener(topics = "credit-score-requests", groupId = "credit-score-group")
    public void processCreditScoreRequest(KafkaPayload payload) {
        LOG.info("Received credit score request: {}", payload);

        try {
            // Generate 4 random integers between 1-10
            List<Integer> randomScores = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                randomScores.add(random.nextInt(10) + 1); // nextInt(10) gives 0-9, +1 makes it 1-10
            }
            
            // Calculate average as the final credit score
            int creditScore = (int) randomScores.stream().mapToInt(Integer::intValue).average().orElse(5);

            LOG.info("Generated random scores {} for task {}, average credit score: {}", 
                     randomScores, payload.getTaskId(), creditScore);

            // Prepare the completion request
            // IMPORTANT: Use the SAME worker ID that locked the task (spring-boot-client)
            Map<String, Object> completionRequest = new HashMap<>();
            completionRequest.put("workerId", "spring-boot-client");
            
            Map<String, Object> variables = new HashMap<>();
            
            // Return single credit score as an integer
            Map<String, Object> scoreVar = new HashMap<>();
            scoreVar.put("value", creditScore);
            scoreVar.put("type", "Integer");
            variables.put("score", scoreVar);
            
            // Also return the list of random scores for reference - serialize to JSON string
            String scoresJson = objectMapper.writeValueAsString(randomScores);
            Map<String, Object> scoresListVar = new HashMap<>();
            scoresListVar.put("value", scoresJson);
            scoresListVar.put("type", "Object");
            scoresListVar.put("valueInfo", Map.of(
                "objectTypeName", "java.util.ArrayList",
                "serializationDataFormat", "application/json"
            ));
            variables.put("creditScores", scoresListVar);
            
            completionRequest.put("variables", variables);

            // Complete the external task via REST API
            String url = camundaBaseUrl + "/external-task/" + payload.getTaskId() + "/complete";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(completionRequest, headers);
            
            restTemplate.postForObject(url, request, String.class);

            LOG.info("Completed external task {} with credit score: {} (from scores: {})", 
                     payload.getTaskId(), creditScore, randomScores);

        } catch (Exception e) {
            LOG.error("Error processing credit score request: {}", payload, e);
            // Optionally implement retry logic or failure handling
        }
    }
}
