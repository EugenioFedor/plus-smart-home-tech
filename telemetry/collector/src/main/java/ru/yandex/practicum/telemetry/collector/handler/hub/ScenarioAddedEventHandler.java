package ru.yandex.practicum.telemetry.collector.handler.hub;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.telemetry.collector.kafka.EventProducer;

@Component
public class ScenarioAddedEventHandler extends BaseHubEventHandler {

    public ScenarioAddedEventHandler(EventProducer producer) {
        super(producer);
    }

    @Override
    public boolean canHandle(HubEventProto event) {
        return event.getPayloadCase()
                == HubEventProto.PayloadCase.SCENARIO_ADDED;
    }
}