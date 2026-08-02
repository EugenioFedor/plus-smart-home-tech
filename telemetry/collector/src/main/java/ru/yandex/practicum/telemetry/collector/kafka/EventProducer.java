package ru.yandex.practicum.telemetry.collector.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventProducer {

    private final Producer<String, SpecificRecordBase> producer;

    public void send(
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