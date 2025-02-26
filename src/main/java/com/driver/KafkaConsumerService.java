package com.driver;

import org.apache.kafka.clients.consumer.*;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

/**
 * Kafka Consumer that listens to events.
 * - Runs independently from Main.java
 */
public class KafkaConsumerService {
    private final Consumer<String, String> consumer;

    public KafkaConsumerService(String bootstrapServers, String topic, String groupId) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", groupId);
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "earliest");

        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));
    }

    public void startListening() {
        Logger.log("KAFKA_CONSUMER", "Kafka Consumer started. Listening for messages...");

        while (true) {  // Run indefinitely (can be stopped manually)
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));

            for (ConsumerRecord<String, String> record : records) {
                Logger.log("KAFKA_CONSUMER", "Received event: " + record.value());
            }
        }
    }
}
