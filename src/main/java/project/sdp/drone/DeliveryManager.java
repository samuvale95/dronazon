package project.sdp.drone;

import com.example.grpc.DroneServiceGrpc;
import com.example.grpc.InsertMessage;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import javafx.util.Pair;
import org.eclipse.paho.client.mqttv3.MqttException;
import project.sdp.dronazon.Delivery;
import project.sdp.server.beans.Drone;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DeliveryManager extends Thread{

    private final DroneProcess droneProcess;
    private final static Logger LOGGER = Logger.getLogger(DeliveryManager.class.getName());


    public DeliveryManager(DroneProcess droneProcess){
        this.droneProcess = droneProcess;
    }

    private Drone getDeliveryDrone(Delivery delivery) throws MqttException {
        ArrayList<Drone> listDrone;
        synchronized (droneProcess.getDronesList()) {
            listDrone =  new ArrayList<>(droneProcess.getDronesList());
        }
        ArrayList<Drone> possibleDrones = new ArrayList<>();

        for (Drone d: listDrone) {
            if (d.hasValidPosition())
                possibleDrones.add(d);
        }

        Drone masterDrone = droneProcess.getMasterDrone();
        if(possibleDrones.contains(masterDrone) && masterDrone.getBattery()<=20 && droneProcess.getMasterProcess().getDeliveryQueue().size()>1) {
            droneProcess.getMasterProcess().disconnectMQTT();
            possibleDrones.remove(masterDrone);
        }

        if (possibleDrones.size() == 0) {
            return null;
        }

        Optional<Pair<Drone, Double>> res = possibleDrones.stream().map(drone -> new Pair<>(drone, drone.getPosition().distance(delivery.getTakePoint()))).reduce((d1, d2) -> {
            if (d1.getValue() < d2.getValue()) return d1;
            else if (d1.getValue() > d2.getValue()) return d2;
            else if (d1.getKey().getId() > d2.getKey().getId()) return d1;
            else return d2;
        });

        Drone result = res.orElse(null).getKey();
        result.invalidatePosition();

        LOGGER.info("Next Delivery Drone: " + result);
        LOGGER.info("List Drone: ");
        LOGGER.info(listDrone.toString());
        LOGGER.info("Possible Drone: ");
        for (Drone drone : possibleDrones) {
            LOGGER.info(new Pair<>(drone, drone.getPosition().distance(delivery.getTakePoint())).toString());
        }
        return result;
    }

    @Override
    public void run() {
        while (true){
            Buffer<Delivery> queue = droneProcess.getMasterProcess().getDeliveryQueue();
            Delivery delivery = null;
            try { delivery = queue.next(); }
            catch (InterruptedException e) { e.printStackTrace(); }

            assert delivery != null;

            Drone deliveryDrone = null;
            try {
                deliveryDrone = getDeliveryDrone(delivery);
            } catch (MqttException e) {
                e.printStackTrace();
            }

            //All drone are busy
            if(deliveryDrone == null){
                droneProcess.getMasterProcess().getDeliveryQueue().add(delivery);
                synchronized (droneProcess.getMasterProcess().getDeliveryHandler()){
                    try {
                        LOGGER.info("Waiting on new delivery drone!");
                        droneProcess.getMasterProcess().getDeliveryHandler().wait();
                        continue;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

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
                public void onError(Throwable t) {
                    droneProcess.onFailNode(t, channel);
                }

                @Override
                public void onCompleted() {
                    LOGGER.info("Delivery Sent to " + droneProcess.getNextDrone());
                    droneProcess.getMasterProcess().incrementDelivery();
                    channel.shutdown();
                }
            });
        }
    }
}
