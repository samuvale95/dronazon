package project.sdp.drone;

import com.example.grpc.DroneServiceGrpc;
import com.example.grpc.InsertMessage;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import project.sdp.dronazon.Delivery;
import project.sdp.server.beans.Drone;
import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class DeliveryHandler extends Thread{

    private final DroneProcess droneProcess;

    public DeliveryHandler(DroneProcess droneProcess){
        this.droneProcess = droneProcess;
    }

    private Drone getDeliveryDrone(Point takeDelivery) {
        ArrayList<Drone> listDrone = droneProcess.getDronesList();
        ArrayList<Drone> possibleDrones = new ArrayList<>();

        listDrone.forEach(d -> {
            if(!d.getCommittedToDelivery())
                possibleDrones.add(d);
        });

        Optional<Drone> res = possibleDrones.stream().min((drone1, drone2) ->{
            double distanceDrone1 = drone1.getPosition().distance(takeDelivery)/drone1.getBattery();
            double distanceDrone2 = drone2.getPosition().distance(takeDelivery)/drone2.getBattery();

            if(distanceDrone1 < distanceDrone2) return -1;
            else if(distanceDrone1 > distanceDrone2) return 1;
            else return Integer.compare(drone2.getId(), drone1.getId());
        });

        Drone result = res.orElse(null);
        synchronized (Objects.requireNonNull(result)) {
            result.setCommittedToDelivery(true);
        }
        System.out.println("********* LOG **********");
        System.out.println(droneProcess.getDronesList());
        System.out.println("******* END LOG **********");
        return result;
    }

    @Override
    public void run() {
        while (true){
            DeliveryQueue queue = droneProcess.getDeliveryQueue();
            Delivery delivery = null;
            try { delivery = queue.next(); }
            catch (InterruptedException e) { e.printStackTrace(); }

            assert delivery != null;

            Drone deliveryDrone = getDeliveryDrone(delivery.getTakePoint());
            assert deliveryDrone != null;

            InsertMessage.Drone droneTarget = InsertMessage.Drone
                    .newBuilder()
                    .setId(deliveryDrone.getId())
                    .setIp(deliveryDrone.getIp())
                    .setPort(deliveryDrone.getPort())
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
