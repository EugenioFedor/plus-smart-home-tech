package ru.yandex.practicum.telemetry.aggregator.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.serialization.SensorEventDeserializer;

import java.util.Properties;

@Configuration
public class KafkaConsumerConfig {

    @Bean(destroyMethod = "close")
    public Consumer<String, SensorEventAvro> kafkaConsumer(
            @Value("${aggregator.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${aggregator.kafka.consumer-group-id}") String groupId) {

        Properties properties = new Properties();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                groupId
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                SensorEventDeserializer.class
        );

        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        return new KafkaConsumer<>(properties);
    }
}