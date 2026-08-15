package ru.yandex.practicum.telemetry.analyzer.service;

import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc.HubRouterControllerBlockingStub;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.entity.Action;
import ru.yandex.practicum.telemetry.analyzer.entity.Condition;
import ru.yandex.practicum.telemetry.analyzer.entity.Scenario;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;

import java.time.Instant;
import java.util.Map;
import java.util.OptionalInt;

@Service
public class SnapshotAnalysisService {

    private final ScenarioRepository scenarioRepository;
    private final HubRouterControllerBlockingStub hubRouterClient;

    public SnapshotAnalysisService(
            ScenarioRepository scenarioRepository,
            @GrpcClient("hub-router") HubRouterControllerBlockingStub hubRouterClient) {
        this.scenarioRepository = scenarioRepository;
        this.hubRouterClient = hubRouterClient;
    }

    public void analyze(SensorsSnapshotAvro snapshot) {
        for (Scenario scenario : scenarioRepository.findByHubId(snapshot.getHubId())) {
            if (matches(snapshot, scenario)) {
                scenario.getActions().forEach((sensorId, action) ->
                        sendAction(snapshot, scenario, sensorId, action));
            }
        }
    }

    private boolean matches(SensorsSnapshotAvro snapshot, Scenario scenario) {
        if (scenario.getConditions().isEmpty()) {
            return true;
        }

        for (Map.Entry<String, Condition> entry : scenario.getConditions().entrySet()) {
            SensorStateAvro state = snapshot.getSensorsState().get(entry.getKey());
            if (state == null || !matchesCondition(state, entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesCondition(SensorStateAvro state, Condition condition) {
        OptionalInt actualValue = extractValue(state.getData(), condition.getType());
        if (actualValue.isEmpty()) {
            return false;
        }

        int actual = actualValue.getAsInt();
        int expected = condition.getValue();

        return switch (condition.getOperation()) {
            case EQUALS -> actual == expected;
            case GREATER_THAN -> actual > expected;
            case LOWER_THAN -> actual < expected;
        };
    }

    private OptionalInt extractValue(Object data, ConditionTypeAvro type) {
        return switch (type) {
            case MOTION -> data instanceof MotionSensorAvro motion
                    ? OptionalInt.of(motion.getMotion() ? 1 : 0)
                    : OptionalInt.empty();
            case LUMINOSITY -> data instanceof LightSensorAvro light
                    ? OptionalInt.of(light.getLuminosity())
                    : OptionalInt.empty();
            case SWITCH -> data instanceof SwitchSensorAvro switchSensor
                    ? OptionalInt.of(switchSensor.getState() ? 1 : 0)
                    : OptionalInt.empty();
            case TEMPERATURE -> {
                if (data instanceof TemperatureSensorAvro temperature) {
                    yield OptionalInt.of(temperature.getTemperatureC());
                }
                if (data instanceof ClimateSensorAvro climate) {
                    yield OptionalInt.of(climate.getTemperatureC());
                }
                yield OptionalInt.empty();
            }
            case CO2LEVEL -> data instanceof ClimateSensorAvro climate
                    ? OptionalInt.of(climate.getCo2Level())
                    : OptionalInt.empty();
            case HUMIDITY -> data instanceof ClimateSensorAvro climate
                    ? OptionalInt.of(climate.getHumidity())
                    : OptionalInt.empty();
        };
    }

    private void sendAction(
            SensorsSnapshotAvro snapshot,
            Scenario scenario,
            String sensorId,
            Action action) {

        DeviceActionProto.Builder actionBuilder = DeviceActionProto.newBuilder()
                .setSensorId(sensorId)
                .setType(ActionTypeProto.valueOf(action.getType().name()));

        if (action.getValue() != null) {
            actionBuilder.setValue(action.getValue());
        }

        Instant instant = snapshot.getTimestamp();
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();

        DeviceActionRequest request = DeviceActionRequest.newBuilder()
                .setHubId(snapshot.getHubId())
                .setScenarioName(scenario.getName())
                .setAction(actionBuilder.build())
                .setTimestamp(timestamp)
                .build();

        hubRouterClient.handleDeviceAction(request);
    }
}
