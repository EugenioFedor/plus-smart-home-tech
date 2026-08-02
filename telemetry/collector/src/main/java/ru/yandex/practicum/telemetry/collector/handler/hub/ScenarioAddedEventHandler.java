package ru.yandex.practicum.telemetry.collector.handler.hub;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.telemetry.collector.dto.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.dto.hub.ScenarioAddedEvent;
import ru.yandex.practicum.telemetry.collector.handler.EventHandler;
import ru.yandex.practicum.telemetry.collector.kafka.EventProducer;
import ru.yandex.practicum.telemetry.collector.mapper.EventMapper;

@Component
@RequiredArgsConstructor
public class ScenarioAddedEventHandler implements EventHandler<HubEvent> {

    private final EventProducer producer;

    @Value("${collector.kafka.topics.hubs}")
    private String topic;

    @Override
    public boolean canHandle(HubEvent event) {
        return event instanceof ScenarioAddedEvent;
    }

    @Override
    public void handle(HubEvent event) {
        ScenarioAddedEvent scenarioEvent = (ScenarioAddedEvent) event;

        producer.send(
                topic,
                scenarioEvent.getHubId(),
                scenarioEvent.getTimestamp().toEpochMilli(),
                EventMapper.toAvro(scenarioEvent)
        );
    }
}