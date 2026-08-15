package ru.yandex.practicum.telemetry.analyzer.processor;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.analyzer.service.HubEventService;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HubEventProcessor implements Runnable {

    private final Consumer<String, HubEventAvro> consumer;
    private final HubEventService hubEventService;
    private final String topic;

    public HubEventProcessor(
            @Qualifier("hubEventConsumer") Consumer<String, HubEventAvro> consumer,
            HubEventService hubEventService,
            @Value("${analyzer.kafka.topics.hubs}") String topic) {
        this.consumer = consumer;
        this.hubEventService = hubEventService;
        this.topic = topic;
    }

    @Override
    public void run() {
        consumer.subscribe(List.of(topic));
        try {
            while (true) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    try {
                        hubEventService.handle(record.value());
                        commitRecord(record);
                    } catch (Exception e) {
                        log.error("Failed to process hub event at {}-{} offset {}",
                                record.topic(), record.partition(), record.offset(), e);
                    }
                }
            }
        } catch (WakeupException ignored) {
            // normal shutdown
        } finally {
            consumer.close();
        }
    }

    private void commitRecord(ConsumerRecord<String, HubEventAvro> record) {
        TopicPartition partition = new TopicPartition(record.topic(), record.partition());
        consumer.commitSync(Map.of(partition, new OffsetAndMetadata(record.offset() + 1)));
    }

    @PreDestroy
    public void stop() {
        consumer.wakeup();
    }
}
