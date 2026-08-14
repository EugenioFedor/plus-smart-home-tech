package ru.yandex.practicum.telemetry.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final Consumer<String, SensorEventAvro> consumer;
    private final Producer<String, SpecificRecordBase> producer;
    private final SnapshotAggregator snapshotAggregator;

    @Value("${aggregator.kafka.topics.sensors}")
    private String sensorsTopic;

    @Value("${aggregator.kafka.topics.snapshots}")
    private String snapshotsTopic;

    public void start() {
        Runtime.getRuntime()
                .addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribe(List.of(sensorsTopic));

            while (true) {
                ConsumerRecords<String, SensorEventAvro> records =
                        consumer.poll(Duration.ofSeconds(1));

                for (ConsumerRecord<String, SensorEventAvro> record : records) {

                    Optional<SensorsSnapshotAvro> snapshot =
                            snapshotAggregator.updateState(record.value());

                    snapshot.ifPresent(this::sendSnapshot);
                }

                consumer.commitAsync();
            }

        } catch (WakeupException ignored) {
            // приложение завершается
        } catch (Exception e) {
            log.error("Ошибка во время агрегации событий", e);

        } finally {
            try {
                producer.flush();
                consumer.commitSync();
            } finally {
                consumer.close();
                producer.close();
            }
        }
    }

    private void sendSnapshot(SensorsSnapshotAvro snapshot) {
        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(
                        snapshotsTopic,
                        null,
                        snapshot.getTimestamp().toEpochMilli(),
                        snapshot.getHubId().toString(),
                        snapshot
                );

        producer.send(record);
    }
}