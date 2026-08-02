package ru.yandex.practicum.telemetry.collector.handler;

public interface EventHandler<T> {

    boolean canHandle(T event);

    void handle(T event);
}