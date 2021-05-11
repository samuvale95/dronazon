package project.sdp.drone;

import com.example.grpc.DroneServiceGrpc;
import com.example.grpc.InsertMessageOuterClass.*;
import io.grpc.stub.StreamObserver;
import project.sdp.server.beans.Drone;

public class DroneService extends DroneServiceGrpc.DroneServiceImplBase {

    private final DroneProcess droneProcess;

    public DroneService(DroneProcess droneProcess){
        this.droneProcess = droneProcess;
    }

    @Override
    public void insertIntoRing(InsertMessage request, StreamObserver<InsertMessage> responseObserver) {
        InsertMessage.drone nextDroneMessage = request.getCallerDrone();
        Drone nextDrone = new Drone(nextDroneMessage.getId(), nextDroneMessage.getIp(), nextDroneMessage.getPort());
        droneProcess.setNextDrone(nextDrone);

        System.out.println("My new next node is: ");
        System.out.println(nextDrone);

        Drone drone = droneProcess.getDrone();
        InsertMessage.drone droneMessage = InsertMessage.drone.newBuilder().setId(drone.getId()).setIp(drone.getIp()).setPort(drone.getPort()).build();
        responseObserver.onNext(InsertMessage.newBuilder().setCallerDrone(droneMessage).build());
        responseObserver.onCompleted();
    }
}
