package ru.yandex.practicum.telemetry.collector.handler.hub;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.telemetry.collector.handler.EventHandler;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HubEventHandlerDispatcher {

    private final List<EventHandler<HubEventProto>> handlers;

    public void handle(HubEventProto event) {
        handlers.stream()
                .filter(handler -> handler.canHandle(event))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No handler found for hub event: "
                                + event.getPayloadCase()
                ))
                .handle(event);
    }
}