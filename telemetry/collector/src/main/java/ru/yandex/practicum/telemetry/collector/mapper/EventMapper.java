package ru.yandex.practicum.telemetry.collector.mapper;

import com.google.protobuf.Timestamp;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioConditionProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.*;
import java.time.Instant;
import java.util.List;

public final class EventMapper {

    private EventMapper() {
    }

    public static SensorEventAvro toAvro(SensorEventProto event) {
        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(toInstant(event.getTimestamp()))
                .setPayload(toSensorPayload(event))
                .build();
    }

    public static HubEventAvro toAvro(HubEventProto event) {
        return HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(toEpochMillis(event.getTimestamp()))
                .setPayload(toHubPayload(event))
                .build();
    }

    private static Object toSensorPayload(SensorEventProto event) {
        return switch (event.getPayloadCase()) {

            case CLIMATE_SENSOR -> {
                var e = event.getClimateSensor();

                yield ClimateSensorAvro.newBuilder()
                        .setTemperatureC(e.getTemperatureC())
                        .setHumidity(e.getHumidity())
                        .setCo2Level(e.getCo2Level())
                        .build();
            }

            case LIGHT_SENSOR -> {
                var e = event.getLightSensor();

                yield LightSensorAvro.newBuilder()
                        .setLinkQuality(e.getLinkQuality())
                        .setLuminosity(e.getLuminosity())
                        .build();
            }

            case MOTION_SENSOR -> {
                var e = event.getMotionSensor();

                yield MotionSensorAvro.newBuilder()
                        .setLinkQuality(e.getLinkQuality())
                        .setMotion(e.getMotion())
                        .setVoltage(e.getVoltage())
                        .build();
            }

            case SWITCH_SENSOR -> {
                var e = event.getSwitchSensor();

                yield SwitchSensorAvro.newBuilder()
                        .setState(e.getState())
                        .build();
            }

            case TEMPERATURE_SENSOR -> {
                var e = event.getTemperatureSensor();

                yield TemperatureSensorAvro.newBuilder()
                        .setTemperatureC(e.getTemperatureC())
                        .setTemperatureF(e.getTemperatureF())
                        .build();
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported sensor payload: " + event.getPayloadCase()
            );
        };
    }

    private static Object toHubPayload(HubEventProto event) {
        return switch (event.getPayloadCase()) {

            case DEVICE_ADDED -> {
                var e = event.getDeviceAdded();

                yield DeviceAddedEventAvro.newBuilder()
                        .setId(e.getId())
                        .setDeviceType(
                                DeviceTypeAvro.valueOf(e.getType().name())
                        )
                        .build();
            }

            case DEVICE_REMOVED -> {
                var e = event.getDeviceRemoved();

                yield DeviceRemovedEventAvro.newBuilder()
                        .setId(e.getId())
                        .build();
            }

            case SCENARIO_ADDED -> {
                var e = event.getScenarioAdded();

                yield ScenarioAddedEventAvro.newBuilder()
                        .setName(e.getName())
                        .setConditions(mapConditions(e.getConditionList()))
                        .setActions(mapActions(e.getActionList()))
                        .build();
            }

            case SCENARIO_REMOVED -> {
                var e = event.getScenarioRemoved();

                yield ScenarioRemovedEventAvro.newBuilder()
                        .setName(e.getName())
                        .build();
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported hub payload: " + event.getPayloadCase()
            );
        };
    }

    private static List<ScenarioConditionAvro> mapConditions(
            List<ScenarioConditionProto> conditions
    ) {
        return conditions.stream()
                .map(condition -> ScenarioConditionAvro.newBuilder()
                        .setSensorId(condition.getSensorId())
                        .setType(
                                ConditionTypeAvro.valueOf(
                                        condition.getType().name()
                                )
                        )
                        .setOperation(
                                ConditionOperationAvro.valueOf(
                                        condition.getOperation().name()
                                )
                        )
                        .setValue(
                                switch (condition.getValueCase()) {
                                    case BOOL_VALUE -> condition.getBoolValue();
                                    case INT_VALUE -> condition.getIntValue();
                                    default -> throw new IllegalArgumentException(
                                            "Unsupported condition value: "
                                                    + condition.getValueCase()
                                    );
                                }
                        )
                        .build())
                .toList();
    }

    private static List<DeviceActionAvro> mapActions(
            List<DeviceActionProto> actions
    ) {
        return actions.stream()
                .map(action -> DeviceActionAvro.newBuilder()
                        .setSensorId(action.getSensorId())
                        .setType(
                                ActionTypeAvro.valueOf(
                                        action.getType().name()
                                )
                        )
                        .setValue(
                                action.hasValue()
                                        ? action.getValue()
                                        : null
                        )
                        .build())
                .toList();
    }

    private static Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(
                timestamp.getSeconds(),
                timestamp.getNanos()
        );
    }

    private static long toEpochMillis(Timestamp timestamp) {
        return timestamp.getSeconds() * 1000
                + timestamp.getNanos() / 1_000_000;
    }
}