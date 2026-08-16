package ru.yandex.practicum.telemetry.analyzer.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.serialization.HubEventDeserializer;
import ru.yandex.practicum.kafka.telemetry.serialization.SensorsSnapshotDeserializer;

import java.util.Properties;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    @Qualifier("hubEventConsumer")
    public Consumer<String, HubEventAvro> hubEventConsumer(
            @Value("${analyzer.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${analyzer.kafka.hubs-group-id}") String groupId,
            @Value("${analyzer.kafka.hubs-client-id}") String clientId) {

        Properties properties = baseProperties(
                bootstrapServers,
                groupId,
                clientId
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                HubEventDeserializer.class
        );

        return new KafkaConsumer<>(properties);
    }

    @Bean
    @Qualifier("snapshotConsumer")
    public Consumer<String, SensorsSnapshotAvro> snapshotConsumer(
            @Value("${analyzer.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${analyzer.kafka.snapshots-group-id}") String groupId,
            @Value("${analyzer.kafka.snapshots-client-id}") String clientId) {

        Properties properties = baseProperties(
                bootstrapServers,
                groupId,
                clientId
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                SensorsSnapshotDeserializer.class
        );

        return new KafkaConsumer<>(properties);
    }

    private Properties baseProperties(
            String bootstrapServers,
            String groupId,
            String clientId) {

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
                ConsumerConfig.CLIENT_ID_CONFIG,
                clientId
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        return properties;
    }
}