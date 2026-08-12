package ru.yandex.practicum.telemetry.collector.handler.hub;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.telemetry.collector.handler.EventHandler;
import ru.yandex.practicum.telemetry.collector.kafka.EventProducer;
import ru.yandex.practicum.telemetry.collector.mapper.EventMapper;

@Component
@RequiredArgsConstructor
public class ScenarioAddedEventHandler implements EventHandler<HubEventProto> {

    private final EventProducer producer;

    @Value("${collector.kafka.topics.hubs}")
    private String topic;

    @Override
    public boolean canHandle(HubEventProto event) {
        return event.getPayloadCase()
                == HubEventProto.PayloadCase.SCENARIO_ADDED;
    }

    @Override
    public void handle(HubEventProto event) {
        producer.send(
                topic,
                event.getHubId(),
                toEpochMillis(event.getTimestamp()),
                EventMapper.toAvro(event)
        );
    }

    private long toEpochMillis(Timestamp timestamp) {
        return timestamp.getSeconds() * 1000
                + timestamp.getNanos() / 1_000_000;
    }
}