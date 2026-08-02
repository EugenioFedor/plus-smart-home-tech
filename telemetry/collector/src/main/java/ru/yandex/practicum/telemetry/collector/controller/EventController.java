package ru.yandex.practicum.telemetry.collector.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.telemetry.collector.dto.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.dto.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.handler.hub.HubEventHandlerDispatcher;
import ru.yandex.practicum.telemetry.collector.handler.sensor.SensorEventHandlerDispatcher;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final SensorEventHandlerDispatcher sensorDispatcher;
    private final HubEventHandlerDispatcher hubDispatcher;

    @PostMapping("/sensors")
    public ResponseEntity<Void> collectSensorEvent(
            @Valid @RequestBody SensorEvent event
    ) {
        sensorDispatcher.handle(event);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/hubs")
    public ResponseEntity<Void> collectHubEvent(
            @Valid @RequestBody HubEvent event
    ) {
        hubDispatcher.handle(event);
        return ResponseEntity.ok().build();
    }
}