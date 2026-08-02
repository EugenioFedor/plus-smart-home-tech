package ru.yandex.practicum.telemetry.collector.handler.hub;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.telemetry.collector.dto.hub.DeviceAddedEvent;
import ru.yandex.practicum.telemetry.collector.dto.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.handler.EventHandler;
import ru.yandex.practicum.telemetry.collector.kafka.EventProducer;
import ru.yandex.practicum.telemetry.collector.mapper.EventMapper;

@Component
@RequiredArgsConstructor
public class DeviceAddedEventHandler implements EventHandler<HubEvent> {

    private final EventProducer producer;

    @Value("${collector.kafka.topics.hubs}")
    private String topic;

    @Override
    public boolean canHandle(HubEvent event) {
        return event instanceof DeviceAddedEvent;
    }

    @Override
    public void handle(HubEvent event) {
        DeviceAddedEvent deviceEvent = (DeviceAddedEvent) event;

        producer.send(
                topic,
                deviceEvent.getHubId(),
                deviceEvent.getTimestamp().toEpochMilli(),
                EventMapper.toAvro(deviceEvent)
        );
    }
}