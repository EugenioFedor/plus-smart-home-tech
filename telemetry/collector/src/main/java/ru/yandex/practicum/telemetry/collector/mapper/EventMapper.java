package ru.yandex.practicum.telemetry.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.collector.dto.hub.*;
import ru.yandex.practicum.telemetry.collector.dto.sensor.*;

import java.util.List;

@Component
public class EventMapper {

    public SensorEventAvro toAvro(SensorEvent event) {
        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(toSensorPayload(event))
                .build();
    }

    public HubEventAvro toAvro(HubEvent event) {
        return HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp().toEpochMilli())
                .setPayload(toHubPayload(event))
                .build();
    }

    private Object toSensorPayload(SensorEvent event) {
        return switch (event) {
            case ClimateSensorEvent e -> ClimateSensorAvro.newBuilder()
                    .setTemperatureC(e.getTemperatureC())
                    .setHumidity(e.getHumidity())
                    .setCo2Level(e.getCo2Level())
                    .build();
            case LightSensorEvent e -> LightSensorAvro.newBuilder()
                    .setLinkQuality(e.getLinkQuality())
                    .setLuminosity(e.getLuminosity())
                    .build();
            case MotionSensorEvent e -> MotionSensorAvro.newBuilder()
                    .setLinkQuality(e.getLinkQuality())
                    .setMotion(e.getMotion())
                    .setVoltage(e.getVoltage())
                    .build();
            case SwitchSensorEvent e -> SwitchSensorAvro.newBuilder()
                    .setState(e.getState())
                    .build();
            case TemperatureSensorEvent e -> TemperatureSensorAvro.newBuilder()
                    .setTemperatureC(e.getTemperatureC())
                    .setTemperatureF(e.getTemperatureF())
                    .build();
            default -> throw new IllegalArgumentException("Unsupported sensor event: " + event.getClass());
        };
    }

    private Object toHubPayload(HubEvent event) {
        return switch (event) {
            case DeviceAddedEvent e -> DeviceAddedEventAvro.newBuilder()
                    .setId(e.getId())
                    .setDeviceType(DeviceTypeAvro.valueOf(e.getDeviceType().name()))
                    .build();
            case DeviceRemovedEvent e -> DeviceRemovedEventAvro.newBuilder()
                    .setId(e.getId())
                    .build();
            case ScenarioAddedEvent e -> ScenarioAddedEventAvro.newBuilder()
                    .setName(e.getName())
                    .setConditions(mapConditions(e.getConditions()))
                    .setActions(mapActions(e.getActions()))
                    .build();
            case ScenarioRemovedEvent e -> ScenarioRemovedEventAvro.newBuilder()
                    .setName(e.getName())
                    .build();
            default -> throw new IllegalArgumentException("Unsupported hub event: " + event.getClass());
        };
    }

    private List<ScenarioConditionAvro> mapConditions(List<ScenarioCondition> conditions) {
        return conditions.stream()
                .map(c -> ScenarioConditionAvro.newBuilder()
                        .setSensorId(c.getSensorId())
                        .setType(ConditionTypeAvro.valueOf(c.getType().name()))
                        .setOperation(ConditionOperationAvro.valueOf(c.getOperation().name()))
                        .setValue(c.getValue())
                        .build())
                .toList();
    }

    private List<DeviceActionAvro> mapActions(List<ru.yandex.practicum.telemetry.collector.dto.hub.DeviceAction> actions) {
        return actions.stream()
                .map(a -> DeviceActionAvro.newBuilder()
                        .setSensorId(a.getSensorId())
                        .setType(ActionTypeAvro.valueOf(a.getType().name()))
                        .setValue(a.getValue())
                        .build())
                .toList();
    }
}
