package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.entity.Action;
import ru.yandex.practicum.telemetry.analyzer.entity.Condition;
import ru.yandex.practicum.telemetry.analyzer.entity.Scenario;
import ru.yandex.practicum.telemetry.analyzer.entity.Sensor;
import ru.yandex.practicum.telemetry.analyzer.repository.ActionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.SensorRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HubEventService {

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @Transactional
    public void handle(HubEventAvro event) {
        Object payload = event.getPayload();
        String hubId = event.getHubId();

        if (payload instanceof DeviceAddedEventAvro added) {
            handleDeviceAdded(hubId, added);
        } else if (payload instanceof DeviceRemovedEventAvro removed) {
            handleDeviceRemoved(hubId, removed);
        } else if (payload instanceof ScenarioAddedEventAvro scenarioAdded) {
            handleScenarioAdded(hubId, scenarioAdded);
        } else if (payload instanceof ScenarioRemovedEventAvro scenarioRemoved) {
            scenarioRepository.findByHubIdAndName(hubId, scenarioRemoved.getName())
                    .ifPresent(scenarioRepository::delete);
        } else {
            throw new IllegalArgumentException("Unsupported hub event payload: " + payload.getClass());
        }
    }

    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro event) {
        sensorRepository.findById(event.getId()).ifPresentOrElse(existing -> {
            if (!existing.getHubId().equals(hubId)) {
                throw new IllegalStateException("Sensor " + event.getId() + " belongs to another hub");
            }
        }, () -> sensorRepository.save(new Sensor(event.getId(), hubId)));
    }

    private void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro event) {
        sensorRepository.findByIdAndHubId(event.getId(), hubId).ifPresent(sensor -> {
            List<Condition> conditionsToDelete = new ArrayList<>();
            List<Action> actionsToDelete = new ArrayList<>();

            for (Scenario scenario : scenarioRepository.findByHubId(hubId)) {
                Condition removedCondition = scenario.getConditions().remove(event.getId());
                Action removedAction = scenario.getActions().remove(event.getId());
                if (removedCondition != null) {
                    conditionsToDelete.add(removedCondition);
                }
                if (removedAction != null) {
                    actionsToDelete.add(removedAction);
                }
                if (removedCondition != null || removedAction != null) {
                    scenarioRepository.save(scenario);
                }
            }
            scenarioRepository.flush();
            conditionRepository.deleteAll(conditionsToDelete);
            actionRepository.deleteAll(actionsToDelete);
            sensorRepository.delete(sensor);
        });
    }

    private void handleScenarioAdded(String hubId, ScenarioAddedEventAvro event) {
        Set<String> sensorIds = new HashSet<>();
        event.getConditions().forEach(c -> sensorIds.add(c.getSensorId()));
        event.getActions().forEach(a -> sensorIds.add(a.getSensorId()));

        for (String sensorId : sensorIds) {
            if (sensorRepository.findByIdAndHubId(sensorId, hubId).isEmpty()) {
                throw new IllegalArgumentException(
                        "Sensor " + sensorId + " is not registered in hub " + hubId
                );
            }
        }

        Scenario scenario = scenarioRepository.findByHubIdAndName(hubId, event.getName())
                .orElseGet(() -> new Scenario(hubId, event.getName()));

        scenario.getConditions().clear();
        scenario.getActions().clear();

        for (ScenarioConditionAvro conditionAvro : event.getConditions()) {
            scenario.getConditions().put(
                    conditionAvro.getSensorId(),
                    new Condition(
                            conditionAvro.getType(),
                            conditionAvro.getOperation(),
                            toInteger(conditionAvro.getValue())
                    )
            );
        }

        for (DeviceActionAvro actionAvro : event.getActions()) {
            scenario.getActions().put(
                    actionAvro.getSensorId(),
                    new Action(actionAvro.getType(), actionAvro.getValue())
            );
        }

        scenarioRepository.save(scenario);
    }

    private Integer toInteger(Object value) {
        if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("Unsupported condition value: " + value);
    }
}
