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
            @Value("${analyzer.kafka.hubs-group-id}") String groupId) {
        Properties properties = baseProperties(bootstrapServers, groupId);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, HubEventDeserializer.class);
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "analyzer-hub-events");
        return new KafkaConsumer<>(properties);
    }

    @Bean
    @Qualifier("snapshotConsumer")
    public Consumer<String, SensorsSnapshotAvro> snapshotConsumer(
            @Value("${analyzer.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${analyzer.kafka.snapshots-group-id}") String groupId) {
        Properties properties = baseProperties(bootstrapServers, groupId);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SensorsSnapshotDeserializer.class);
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "analyzer-snapshots");
        return new KafkaConsumer<>(properties);
    }

    private Properties baseProperties(String bootstrapServers, String groupId) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return properties;
    }
}
