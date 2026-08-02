package ru.yandex.practicum.telemetry.collector.handler.sensor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.telemetry.collector.dto.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.handler.EventHandler;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SensorEventHandlerDispatcher {

    private final List<EventHandler<SensorEvent>> handlers;

    public void handle(SensorEvent event) {
        handlers.stream()
                .filter(handler -> handler.canHandle(event))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No handler found for sensor event: " +
                                event.getClass().getName()
                ))
                .handle(event);
    }
}