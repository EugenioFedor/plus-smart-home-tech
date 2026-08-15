package ru.yandex.practicum.telemetry.analyzer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;

@Entity
@Table(name = "conditions")
@Getter
@Setter
@NoArgsConstructor
public class Condition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConditionTypeAvro type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConditionOperationAvro operation;

    @Column(nullable = false)
    private Integer value;

    public Condition(ConditionTypeAvro type, ConditionOperationAvro operation, Integer value) {
        this.type = type;
        this.operation = operation;
        this.value = value;
    }
}
