package ru.yandex.practicum.telemetry.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.telemetry.collector.handler.hub.HubEventHandlerDispatcher;
import ru.yandex.practicum.telemetry.collector.handler.sensor.SensorEventHandlerDispatcher;

@GrpcService
@RequiredArgsConstructor
public class EventController
        extends CollectorControllerGrpc.CollectorControllerImplBase {

    private final SensorEventHandlerDispatcher sensorDispatcher;
    private final HubEventHandlerDispatcher hubDispatcher;

    @Override
    public void collectSensorEvent(
            SensorEventProto request,
            StreamObserver<Empty> responseObserver
    ) {
        try {
            sensorDispatcher.handle(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.fromThrowable(e).asRuntimeException());
        }
    }

    @Override
    public void collectHubEvent(
            HubEventProto request,
            StreamObserver<Empty> responseObserver
    ) {
        try {
            hubDispatcher.handle(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.fromThrowable(e).asRuntimeException());
        }
    }
}