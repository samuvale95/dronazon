package project.sdp.drone;

import com.example.grpc.DroneServiceGrpc;
import com.example.grpc.InsertMessage;
import io.grpc.stub.StreamObserver;
import project.sdp.server.beans.Drone;

public class DroneService extends DroneServiceGrpc.DroneServiceImplBase {

    private final DroneProcess droneProcess;

    public DroneService(DroneProcess droneProcess) {
        this.droneProcess = droneProcess;
    }

    @Override
    public void insertIntoRing(com.example.grpc.InsertMessage.InsertRingRequest request, StreamObserver<com.example.grpc.InsertMessage.InsertRingResponse> responseObserver) {
        com.example.grpc.InsertMessage.Drone nextDroneMessage = request.getCallerDrone();
        Drone nextDrone = new Drone(nextDroneMessage.getId(), nextDroneMessage.getIp(), nextDroneMessage.getPort());
        droneProcess.setNextDrone(nextDrone);

        Drone drone = droneProcess.getDrone();
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
        responseObserver.onCompleted();

        if(drone.getId() == droneProcess.getDrone().getId()) return;

        droneProcess.addDronePosition(drone);
        DroneServiceGrpc.DroneServiceBlockingStub stub = droneProcess.getStub(droneProcess.getNextDrone());
        stub.sendPosition(request);
    }
}
