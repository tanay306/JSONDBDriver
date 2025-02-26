package com.driver;

import org.apache.kafka.clients.producer.*;
import java.util.Properties;

public class KafkaProducerService {
    private final Producer<String, String> producer;
    private final String topic;

    public KafkaProducerService(String bootstrapServers, String topic) {
        this.topic = topic;

        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        this.producer = new KafkaProducer<>(props);
    }

    public void publishEvent(String key, String message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, message);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                Logger.log("ERROR", "Failed to send Kafka message: " + exception.getMessage());
            } else {
                Logger.log("KAFKA", "Event sent to topic " + metadata.topic() +
                        " | partition " + metadata.partition() +
                        " | offset " + metadata.offset());
            }
        });
    }

    public void close() {
        producer.close();
    }
}
