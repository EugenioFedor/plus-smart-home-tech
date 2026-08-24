package ru.yandex.practicum.telemetry.collector.handler.sensor;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.telemetry.collector.handler.EventHandler;
import ru.yandex.practicum.telemetry.collector.kafka.EventProducer;
import ru.yandex.practicum.telemetry.collector.mapper.EventMapper;

import java.time.Instant;

@RequiredArgsConstructor
public abstract class BaseSensorEventHandler
        implements EventHandler<SensorEventProto> {

    protected final EventProducer producer;

    @Override
    public void handle(SensorEventProto event) {
        producer.sendSensorEvent(
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