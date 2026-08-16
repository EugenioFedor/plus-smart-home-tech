package ru.yandex.practicum.telemetry.collector.handler.sensor;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.telemetry.collector.kafka.EventProducer;

@Component
public class SwitchSensorEventHandler extends BaseSensorEventHandler {

    public SwitchSensorEventHandler(EventProducer producer) {
        super(producer);
    }

    @Override
    public boolean canHandle(SensorEventProto event) {
        return event.getPayloadCase()
                == SensorEventProto.PayloadCase.SWITCH_SENSOR;
    }
}