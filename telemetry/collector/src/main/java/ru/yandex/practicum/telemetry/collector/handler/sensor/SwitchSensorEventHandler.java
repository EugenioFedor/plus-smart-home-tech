package ru.yandex.practicum.telemetry.collector.handler.sensor;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.telemetry.collector.handler.EventHandler;
import ru.yandex.practicum.telemetry.collector.kafka.EventProducer;
import ru.yandex.practicum.telemetry.collector.mapper.EventMapper;

@Component
@RequiredArgsConstructor
public class SwitchSensorEventHandler implements EventHandler<SensorEventProto> {

    private final EventProducer producer;

    @Value("${collector.kafka.topics.sensors}")
    private String topic;

    @Override
    public boolean canHandle(SensorEventProto event) {
        return event.getPayloadCase()
                == SensorEventProto.PayloadCase.LIGHT_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
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