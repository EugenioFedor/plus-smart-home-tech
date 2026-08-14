package ru.yandex.practicum.telemetry.aggregator.config;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.serialization.AvroSerializer;

import java.util.Properties;

@Configuration
public class KafkaProducerConfig {

    @Bean(destroyMethod = "close")
    public Producer<String, SpecificRecordBase> kafkaProducer(
            @Value("${aggregator.kafka.bootstrap-servers}") String bootstrapServers) {

        Properties properties = new Properties();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                AvroSerializer.class
        );

        properties.put(
                ProducerConfig.ACKS_CONFIG,
                "all"
        );

        return new KafkaProducer<>(properties);
    }
}