package ru.yandex.practicum.telemetry.collector.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventProducer {

    private final Producer<String, SpecificRecordBase> producer;

    @Value("${collector.kafka.topics.hubs}")
    private String hubsTopic;

    @Value("${collector.kafka.topics.sensors}")
    private String sensorsTopic;

    public void sendHubEvent(
            String key,
            long timestamp,
            HubEventAvro event
    ) {
        send(hubsTopic, key, timestamp, event);
    }

    public void sendSensorEvent(
            String key,
            long timestamp,
            SensorEventAvro event
    ) {
        send(sensorsTopic, key, timestamp, event);
    }

    private void send(
            String topic,
            String key,
            long timestamp,
            SpecificRecordBase event
    ) {
        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(
                        topic,
                        null,
                        timestamp,
                        key,
                        event
                );

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error(
                        "Failed to send event to topic {} with key {}",
                        topic,
                        key,
                        exception
                );
                return;
            }

            log.debug(
                    "Event sent to topic {}, partition {}, offset {}",
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset()
            );
        });
    }
}