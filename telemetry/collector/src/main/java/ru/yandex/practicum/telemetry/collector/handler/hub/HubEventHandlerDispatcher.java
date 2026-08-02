package ru.yandex.practicum.telemetry.collector.handler.hub;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.telemetry.collector.dto.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.handler.EventHandler;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HubEventHandlerDispatcher {

    private final List<EventHandler<HubEvent>> handlers;

    public void handle(HubEvent event) {
        handlers.stream()
                .filter(handler -> handler.canHandle(event))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No handler found for hub event: " +
                                event.getClass().getName()
                ))
                .handle(event);
    }
}