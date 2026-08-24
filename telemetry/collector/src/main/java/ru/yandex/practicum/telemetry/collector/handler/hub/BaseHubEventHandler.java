package ru.yandex.practicum.telemetry.collector.handler.hub;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.telemetry.collector.handler.EventHandler;
import ru.yandex.practicum.telemetry.collector.kafka.EventProducer;
import ru.yandex.practicum.telemetry.collector.mapper.EventMapper;

import java.time.Instant;

@RequiredArgsConstructor
public abstract class BaseHubEventHandler
        implements EventHandler<HubEventProto> {

    protected final EventProducer producer;

    @Override
    public void handle(HubEventProto event) {
        producer.sendHubEvent(
                event.getHubId(),
                toEpochMillis(event.getTimestamp()),
                EventMapper.toAvro(event)
        );
    }

    private long toEpochMillis(Timestamp timestamp) {
        return Instant.ofEpochSecond(
                timestamp.getSeconds(),
                timestamp.getNanos()
        ).toEpochMilli();
    }
}