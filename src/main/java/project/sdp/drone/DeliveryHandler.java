package project.sdp.drone;

import com.example.grpc.DroneServiceGrpc;
import com.example.grpc.InsertMessage;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import project.sdp.dronazon.Delivery;
import project.sdp.server.beans.Drone;

public class DeliveryHandler extends Thread{

    private DroneProcess droneProcess;

    public DeliveryHandler(DroneProcess droneProcess){
        this.droneProcess = droneProcess;
    }

    @Override
    public void run() {
        while (true){
            DeliveryQueue queue = droneProcess.getDeliveryQueue();
            Delivery delivery = null;
            try { delivery = queue.next(); }
            catch (InterruptedException e) { e.printStackTrace(); }

            assert delivery != null;

            Drone nextDrone = droneProcess.getNextDrone();
            assert nextDrone != null;

            InsertMessage.Drone droneTarget = InsertMessage.Drone
                    .newBuilder()
                    .setId(nextDrone.getId())
                    .setIp(nextDrone.getIp())
                    .setPort(nextDrone.getPort())
                    .build();

            InsertMessage.Position deliveryPoint = InsertMessage.Position
                    .newBuilder()
                    .setX((int) delivery.getDeliveryPoint().getX())
                    .setY((int) delivery.getDeliveryPoint().getY())
                    .build();

            InsertMessage.Position takePoint = InsertMessage.Position
                    .newBuilder()
                    .setX((int) delivery.getTakePoint().getX())
                    .setY((int) delivery.getTakePoint().getY())
                    .build();

            InsertMessage.DeliveryRequest deliveryRequest = InsertMessage.DeliveryRequest
                    .newBuilder()
                    .setDelivery(
                            InsertMessage.Delivery
                                    .newBuilder()
                                    .setDroneTarget(droneTarget)
                                    .setTakePoint(takePoint)
                                    .setDeliveryPoint(deliveryPoint)
                                    .setId(delivery.getID())
                                    .build())
                    .build();

            ManagedChannel channel = droneProcess.getChannel(droneProcess.getNextDrone());
            DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
            stub.sendDelivery(deliveryRequest, new StreamObserver<InsertMessage.DeliveryResponse>() {
                @Override
                public void onNext(InsertMessage.DeliveryResponse value) {}

                @Override
                public void onError(Throwable t) {}

                @Override
                public void onCompleted() {
                    System.out.println("Delivery Sent!");
                    channel.shutdown();
                }
            });
        }
    }
}
