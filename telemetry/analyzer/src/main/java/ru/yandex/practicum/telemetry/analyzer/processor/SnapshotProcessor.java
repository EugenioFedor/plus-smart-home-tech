package ru.yandex.practicum.telemetry.analyzer.processor;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.analyzer.service.SnapshotAnalysisService;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SnapshotProcessor {

    private final Consumer<String, SensorsSnapshotAvro> consumer;
    private final SnapshotAnalysisService analysisService;
    private final String topic;

    public SnapshotProcessor(
            @Qualifier("snapshotConsumer") Consumer<String, SensorsSnapshotAvro> consumer,
            SnapshotAnalysisService analysisService,
            @Value("${analyzer.kafka.topics.snapshots}") String topic) {
        this.consumer = consumer;
        this.analysisService = analysisService;
        this.topic = topic;
    }

    public void start() {
        consumer.subscribe(List.of(topic));

        int processed = 0;

        try {
            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records =
                        consumer.poll(Duration.ofSeconds(1));

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    try {
                        analysisService.analyze(record.value());
                        processed++;

                        if (processed % 100 == 0) {
                            consumer.commitAsync();
                        }

                    } catch (Exception e) {
                        log.error(
                                "Failed to analyze snapshot at {}-{} offset {}",
                                record.topic(),
                                record.partition(),
                                record.offset(),
                                e
                        );
                    }
                }
            }

        } catch (WakeupException ignored) {
            // normal shutdown

        } finally {
            try {
                consumer.commitSync();
            } finally {
                consumer.close();
            }
        }
    }

    private void commitRecord(ConsumerRecord<String, SensorsSnapshotAvro> record) {
        TopicPartition partition = new TopicPartition(record.topic(), record.partition());
        consumer.commitSync(Map.of(partition, new OffsetAndMetadata(record.offset() + 1)));
    }

    @PreDestroy
    public void stop() {
        consumer.wakeup();
    }
}
