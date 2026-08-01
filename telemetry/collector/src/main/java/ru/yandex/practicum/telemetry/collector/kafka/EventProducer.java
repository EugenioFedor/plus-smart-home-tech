package ru.yandex.practicum.telemetry.collector.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.telemetry.collector.dto.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.dto.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.mapper.EventMapper;

@Component
@RequiredArgsConstructor
public class EventProducer {
    private final Producer<String, SpecificRecordBase> producer;
    private final EventMapper mapper;

    @Value("${collector.kafka.topics.sensors}")
    private String sensorsTopic;

    @Value("${collector.kafka.topics.hubs}")
    private String hubsTopic;

    public void send(SensorEvent event) {
        producer.send(new ProducerRecord<>(sensorsTopic, event.getHubId(), mapper.toAvro(event)));
    }

    public void send(HubEvent event) {
        producer.send(new ProducerRecord<>(hubsTopic, event.getHubId(), mapper.toAvro(event)));
    }
}
