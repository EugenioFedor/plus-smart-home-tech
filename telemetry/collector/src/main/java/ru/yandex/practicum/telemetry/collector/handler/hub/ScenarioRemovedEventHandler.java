package ru.yandex.practicum.telemetry.collector.handler.hub;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.telemetry.collector.dto.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.dto.hub.ScenarioRemovedEvent;
import ru.yandex.practicum.telemetry.collector.handler.EventHandler;
import ru.yandex.practicum.telemetry.collector.kafka.EventProducer;
import ru.yandex.practicum.telemetry.collector.mapper.EventMapper;

@Component
@RequiredArgsConstructor
public class ScenarioRemovedEventHandler implements EventHandler<HubEvent> {

    private final EventProducer producer;

    @Value("${collector.kafka.topics.hubs}")
    private String topic;

    @Override
    public boolean canHandle(HubEvent event) {
        return event instanceof ScenarioRemovedEvent;
    }

    @Override
    public void handle(HubEvent event) {
        ScenarioRemovedEvent scenarioEvent = (ScenarioRemovedEvent) event;

        producer.send(
                topic,
                scenarioEvent.getHubId(),
                scenarioEvent.getTimestamp().toEpochMilli(),
                EventMapper.toAvro(scenarioEvent)
        );
    }
}