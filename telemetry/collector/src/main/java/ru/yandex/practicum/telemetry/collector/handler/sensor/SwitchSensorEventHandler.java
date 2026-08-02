package ru.yandex.practicum.telemetry.collector.handler.sensor;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.telemetry.collector.dto.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.dto.sensor.SwitchSensorEvent;
import ru.yandex.practicum.telemetry.collector.handler.EventHandler;
import ru.yandex.practicum.telemetry.collector.kafka.EventProducer;
import ru.yandex.practicum.telemetry.collector.mapper.EventMapper;

@Component
@RequiredArgsConstructor
public class SwitchSensorEventHandler implements EventHandler<SensorEvent> {

    private final EventProducer producer;

    @Value("${collector.kafka.topics.sensors}")
    private String topic;

    @Override
    public boolean canHandle(SensorEvent event) {
        return event instanceof SwitchSensorEvent;
    }

    @Override
    public void handle(SensorEvent event) {
        SwitchSensorEvent switchEvent = (SwitchSensorEvent) event;

        producer.send(
                topic,
                switchEvent.getHubId(),
                switchEvent.getTimestamp().toEpochMilli(),
                EventMapper.toAvro(switchEvent)
        );
    }
}