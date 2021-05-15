package project.sdp.drone;

import com.example.grpc.DroneServiceGrpc;
import com.example.grpc.InsertMessage;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import project.sdp.server.beans.Drone;

public class DroneService extends DroneServiceGrpc.DroneServiceImplBase {

    private final DroneProcess droneProcess;

    public DroneService(DroneProcess droneProcess) {
        this.droneProcess = droneProcess;
    }

    @Override
    public void insertIntoRing(com.example.grpc.InsertMessage.InsertRingRequest request, StreamObserver<com.example.grpc.InsertMessage.InsertRingResponse> responseObserver) {
        Drone drone = droneProcess.getNextDrone();

        com.example.grpc.InsertMessage.Drone nextDroneMessage = request.getCallerDrone();
        Drone nextDrone = new Drone(nextDroneMessage.getId(), nextDroneMessage.getIp(), nextDroneMessage.getPort());
        droneProcess.setNextDrone(nextDrone);


        Drone masterDrone = droneProcess.getMasterDrone();
        InsertMessage.Drone droneMessage = InsertMessage.Drone.newBuilder().setId(drone.getId()).setIp(drone.getIp()).setPort(drone.getPort()).build();
        InsertMessage.Drone droneMasterMessage = InsertMessage.Drone.newBuilder().setId(masterDrone.getId()).setIp(masterDrone.getIp()).setPort(masterDrone.getPort()).build();

        responseObserver.onNext(InsertMessage.InsertRingResponse.newBuilder().setNextDrone(droneMessage).setMasterDrone(droneMasterMessage).build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendPosition(InsertMessage.PositionRequest request, StreamObserver<InsertMessage.PositionResponse> responseObserver) {
        InsertMessage.Drone drone = request.getDrone();
        InsertMessage.Position position = request.getPosition();

        if(drone.getId() == droneProcess.getDrone().getId()){
            responseObserver.onNext(InsertMessage.PositionResponse.newBuilder().build());
            responseObserver.onCompleted();
            return;
        }

        droneProcess.addDronePosition(drone, position);
        ManagedChannel channel = droneProcess.getChannel(droneProcess.getNextDrone());
        DroneServiceGrpc.DroneServiceBlockingStub stub = DroneServiceGrpc.newBlockingStub(channel);
        stub.sendPosition(request);
        channel.shutdown();

        responseObserver.onNext(InsertMessage.PositionResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendDelivery(InsertMessage.DeliveryRequest request, StreamObserver<InsertMessage.DeliveryResponse> responseObserver) {
        InsertMessage.Position takePosition = request.getDelivery().getTakePoint();

        if(request.getDelivery().getDroneTarget().getId() == droneProcess.getDrone().getId()){
            responseObserver.onNext(InsertMessage.DeliveryResponse.newBuilder().build());
            responseObserver.onCompleted();

            //TODO implement logic for delivery
            System.out.println("****+ Making a delivery *******");
            return;
        }

        ManagedChannel channel = droneProcess.getChannel(droneProcess.getNextDrone());
        DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
        stub.sendDelivery(request, new StreamObserver<InsertMessage.DeliveryResponse>() {
            @Override
            public void onNext(InsertMessage.DeliveryResponse value) {}

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {
                System.out.println("Delivery sent to Next node");
                channel.shutdown();
            }
        });
        //TODO probably fail point, understand if i terminate a method can cause problem.
    }
}
