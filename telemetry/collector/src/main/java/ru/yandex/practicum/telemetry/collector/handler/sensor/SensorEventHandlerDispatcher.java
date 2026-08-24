package ru.yandex.practicum.telemetry.collector.handler.sensor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.telemetry.collector.handler.EventHandler;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SensorEventHandlerDispatcher {

    private final List<EventHandler<SensorEventProto>> handlers;

    public void handle(SensorEventProto event) {
        handlers.stream()
                .filter(handler -> handler.canHandle(event))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No handler found for sensor event: " +
                                event.getPayloadCase()
                ))
                .handle(event);
    }
}