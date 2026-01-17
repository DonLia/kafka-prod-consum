package org.camunda.bpm.spring.boot.example;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload sent to Kafka for credit score checking.
 * The consumer processes this and returns the credit scores.
 */
public class KafkaPayload {

    @JsonProperty("taskId")
    private String taskId;

    @JsonProperty("processInstanceId")
    private String processInstanceId;

    @JsonProperty("defaultScore")
    private int defaultScore;

    @JsonProperty("creditScores")
    private java.util.List<Integer> creditScores;

    public KafkaPayload() {
    }

    public KafkaPayload(String taskId, String processInstanceId, int defaultScore) {
        this.taskId = taskId;
        this.processInstanceId = processInstanceId;
        this.defaultScore = defaultScore;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public int getDefaultScore() {
        return defaultScore;
    }

    public void setDefaultScore(int defaultScore) {
        this.defaultScore = defaultScore;
    }

    public java.util.List<Integer> getCreditScores() {
        return creditScores;
    }

    public void setCreditScores(java.util.List<Integer> creditScores) {
        this.creditScores = creditScores;
    }

    @Override
    public String toString() {
        return "KafkaPayload{" +
                "taskId='" + taskId + '\'' +
                ", processInstanceId='" + processInstanceId + '\'' +
                ", defaultScore=" + defaultScore +
                ", creditScores=" + creditScores +
                '}';
    }
}
