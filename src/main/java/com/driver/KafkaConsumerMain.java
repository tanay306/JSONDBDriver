package com.driver;

/**
 * Separate Kafka Consumer Execution
 * - This runs independently of Main.java
 * - Only listens to Kafka messages and logs them.
 */
public class KafkaConsumerMain {
    public static void main(String[] args) {
        String kafkaServers = "localhost:9092";
        String kafkaTopic = "json_database_events";
        String kafkaConsumerGroup = "json_db_group";

        KafkaConsumerService consumerService = new KafkaConsumerService(kafkaServers, kafkaTopic, kafkaConsumerGroup);

        // Start Kafka Consumer
        consumerService.startListening();
    }
}
