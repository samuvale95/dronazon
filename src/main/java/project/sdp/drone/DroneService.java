package project.sdp.drone;

import com.example.grpc.DroneServiceGrpc;
import com.example.grpc.InsertMessage;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.eclipse.paho.client.mqttv3.MqttException;
import project.sdp.dronazon.RandomDelivery;
import project.sdp.server.beans.Drone;
import java.awt.*;
import java.util.ArrayList;
import java.util.logging.Logger;

public class DroneService extends DroneServiceGrpc.DroneServiceImplBase {

    private final DroneProcess droneProcess;
    private final static Logger LOGGER = Logger.getLogger(DroneService.class.getName());

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

    private void sendPositionToNextDrone(InsertMessage.PositionRequest request) {
        Context.current().fork().run(() -> {
            ManagedChannel channel = droneProcess.getChannel(droneProcess.getNextDrone());
            DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
            stub.sendPosition(request, new StreamObserver<InsertMessage.PositionResponse>() {
                @Override
                public void onNext(InsertMessage.PositionResponse value) {}

                @Override
                public void onError(Throwable t) {
                    System.err.println("Send position");
                    droneProcess.onFailNode(t, channel);
                    sendPositionToNextDrone(request);
                }

                @Override
                public void onCompleted() {
                    channel.shutdown();
                    droneProcess.setBusy(false);
                }
            });
        });
    }

    @Override
    public void sendPosition(InsertMessage.PositionRequest request, StreamObserver<InsertMessage.PositionResponse> responseObserver) {
        droneProcess.setBusy(true);
        InsertMessage.Drone drone = request.getDrone();
        InsertMessage.Position position = request.getPosition();

        if(drone.getId() == droneProcess.getDrone().getId()){
            responseObserver.onNext(InsertMessage.PositionResponse.newBuilder().build());
            responseObserver.onCompleted();
            droneProcess.setBusy(false);
            return;
        }

        LOGGER.info("SET POSITION: " + drone);
        LOGGER.info(droneProcess.getDronesList().toString());
        droneProcess.addDronePosition(drone, position);
        LOGGER.info(droneProcess.getDronesList().toString());

        sendPositionToNextDrone(request);
        responseObserver.onNext(InsertMessage.PositionResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    private void sendDeliveryToNextDrone(InsertMessage.DeliveryRequest request) {
        Context.current().fork().run(() -> {
            ManagedChannel channel = droneProcess.getChannel(droneProcess.getNextDrone());
            DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
            stub.sendDelivery(request, new StreamObserver<InsertMessage.DeliveryResponse>() {
                @Override
                public void onNext(InsertMessage.DeliveryResponse value) {}

                @Override
                public void onError(Throwable t) {
                    System.err.println("Send delivery");
                    droneProcess.onFailNode(t, channel);
                    LOGGER.info("Recovered Node, sending delivery to next Drone");
                    sendDeliveryToNextDrone(request);
                }

                @Override
                public void onCompleted() {
                    channel.shutdown();
                    droneProcess.setBusy(false);
                }
            });
        });
    }

    @Override
    public void sendDelivery(InsertMessage.DeliveryRequest request, StreamObserver<InsertMessage.DeliveryResponse> responseObserver) {
        droneProcess.setBusy(true);
        if(request.getDelivery().getDroneTarget().getId() == droneProcess.getDrone().getId()){
            responseObserver.onNext(InsertMessage.DeliveryResponse.newBuilder().build());
            responseObserver.onCompleted();

            InsertMessage.Delivery newDelivery = request.getDelivery();
            Point deliveryPosition = new Point(newDelivery.getDeliveryPoint().getX(), newDelivery.getDeliveryPoint().getY());
            Point takePosition = new Point(newDelivery.getTakePoint().getX(), newDelivery.getTakePoint().getY());
            try {
                droneProcess.makeDelivery(new Delivery(request.getDelivery().getId(), takePosition, deliveryPosition));
            } catch (InterruptedException | MqttException e) {
                e.printStackTrace();
            }
            droneProcess.setBusy(false);
            return;
        }else if(droneProcess.getDrone().getId() == droneProcess.getMasterDrone().getId() && droneProcess.getDrone().getId() != request.getDelivery().getDroneTarget().getId()){
            LOGGER.info("Reinsert delivery to queue, drone: " + request.getDelivery().getDroneTarget() + "no more inside a Ring");
            droneProcess.getMasterProcess().getDeliveryQueue().add(new RandomDelivery(request.getDelivery().getId(),
                    new Point(request.getDelivery().getTakePoint().getX(),
                            request.getDelivery().getTakePoint().getY()),
                    new Point(request.getDelivery().getDeliveryPoint().getX(),
                            request.getDelivery().getDeliveryPoint().getY()))
            );
            InsertMessage.Drone drone = request.getDelivery().getDroneTarget();
            synchronized (droneProcess.getDronesList()) {
                droneProcess.getDronesList().remove(new Drone(drone.getId(), drone.getIp(), drone.getPort()));
            }
            droneProcess.getMasterProcess().decrementDelivery();
            return;
        }

        sendDeliveryToNextDrone(request);
        responseObserver.onNext(InsertMessage.DeliveryResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    public void sendInfoAfterDeliveryToNextDrone(InsertMessage.InfoAndStatsRequest request){
        Context.current().fork().run(() -> {
            ManagedChannel channel = droneProcess.getChannel(droneProcess.getNextDrone());
            DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
            stub.sendInfoAfterDelivery(request, new StreamObserver<InsertMessage.InfoAndStatsResponse>() {
                @Override
                public void onNext(InsertMessage.InfoAndStatsResponse value) {}

                @Override
                public void onError(Throwable t) {
                    LOGGER.info("Error on send Info and Stats");
                    droneProcess.onFailNode(t, channel);
                    sendInfoAfterDeliveryToNextDrone(request);
                }

                @Override
                public void onCompleted() {
                    LOGGER.info("STATS sent to next drone");
                    channel.shutdown();
                    droneProcess.setBusy(false);
                }
            });
        });
    }


    @Override
    public void sendInfoAfterDelivery(InsertMessage.InfoAndStatsRequest request, StreamObserver<InsertMessage.InfoAndStatsResponse> responseObserver) {
        droneProcess.setBusy(true);
        LOGGER.info("Message STATS arrived  from previous drone " + request.getDroneTarget());
        if(droneProcess.getDrone().getId() == request.getDroneTarget()){
            InfoAndStats infoAndStats = new InfoAndStats(request.getDeliveryTimeStamp(),
                    new Point(request.getNewPosition().getX(),
                            request.getNewPosition().getY()
                    ),
                    request.getBattery(),
                    request.getDistanceRoutes(),
                    request.getAirPollutionList(),
                    request.getCallerDrone(),
                    request.getDeliveryNumber()
            );

            droneProcess.getMasterProcess().getInfoAndStatsQueue().add( infoAndStats );

            int callerDroneId = infoAndStats.getCallerDrone();
            Point newPosition = infoAndStats.getNewPosition();
            int battery = infoAndStats.getBattery();

            ArrayList<Drone> listDrone;
            synchronized (droneProcess.getDronesList()) {
                listDrone = new ArrayList<>(droneProcess.getDronesList());

                for (Drone drone: listDrone) {
                    if (drone.getId() == callerDroneId) {
                        drone.setPosition(newPosition);
                        drone.setBattery(battery);
                    }
                }
            }

            synchronized (droneProcess.getMasterProcess().getDeliveryHandler()){
                droneProcess.getMasterProcess().getDeliveryHandler().notify();
            }

            synchronized (droneProcess.getMasterProcess()) {
                LOGGER.info("NOTIFY received Statistic from " + request.getDroneTarget());
                droneProcess.getMasterProcess().notify();
            }

            droneProcess.getMasterProcess().incrementStatistic();
            responseObserver.onNext(InsertMessage.InfoAndStatsResponse.newBuilder().build());
            responseObserver.onCompleted();
            droneProcess.setBusy(false);
            return;
        }

        sendInfoAfterDeliveryToNextDrone(request);
        responseObserver.onNext(InsertMessage.InfoAndStatsResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    private void sendElectionToNextDrone(InsertMessage.ElectionRequest message) {
        Context.current().fork().run(()-> {
            LOGGER.info("Sending election to: " + droneProcess.getNextDrone());
            ManagedChannel channel = droneProcess.getChannel(droneProcess.getNextDrone());

            DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
            stub.election(message, new StreamObserver<InsertMessage.ElectionResponse>() {
                @Override
                public void onNext(InsertMessage.ElectionResponse value) { }

                @Override
                public void onError(Throwable t) { }

                @Override
                public void onCompleted() {
                    LOGGER.info("COMPLETE SEND ELECTION");
                    channel.shutdown();
                    if(message.getType().equals("ELECTED"))
                        droneProcess.setBusy(false);
                }
            });
        });
    }

    @Override
    public void election(InsertMessage.ElectionRequest request, StreamObserver<InsertMessage.ElectionResponse> responseObserver) {
        droneProcess.setBusy(true);
        InsertMessage.ElectionRequest message;

        if(request.getType().equals("ELECTION")) {
            LOGGER.info("ELECTION IN ACTION");
            if (request.getId() == droneProcess.getDrone().getId()) {
                LOGGER.info("ELECTED " + droneProcess.getDrone().getId() + " drone" );
                message = InsertMessage.ElectionRequest
                        .newBuilder()
                        .setId(droneProcess.getDrone().getId())
                        .setBattery(droneProcess.getDrone().getBattery())
                        .setType("ELECTED")
                        .build();
            }else if (request.getBattery() > droneProcess.getDrone().getBattery() ||
                    (request.getBattery() == droneProcess.getDrone().getBattery() && request.getId() > droneProcess.getDrone().getId())) {
                LOGGER.info("FORWARD");
                message = request;
            }else{
                LOGGER.info("FORWARD with new packet");
                message = InsertMessage.ElectionRequest
                        .newBuilder()
                        .setId(droneProcess.getDrone().getId())
                        .setBattery(droneProcess.getDrone().getBattery())
                        .setType("ELECTION")
                        .build();
            }
        }else{
            LOGGER.info("ELECTED RECEIVED ON DRONE " + droneProcess.getDrone().getId());
            if(request.getId() == droneProcess.getDrone().getId()){
                LOGGER.info("SET NEW MASTER on Master");
                droneProcess.becomeMaster();
                droneProcess.setBusy(false);
                return;
            }


            message = request;
            droneProcess.setMasterNode(droneProcess.getDrone(message.getId()));

            Drone drone = droneProcess.getDrone();
            InsertMessage.PositionRequest position = InsertMessage.PositionRequest
                    .newBuilder()
                    .setDrone(InsertMessage.Drone
                            .newBuilder()
                            .setIp(drone.getIp())
                            .setPort(drone.getPort())
                            .setId(drone.getId())
                            .build()
                    )
                    .setPosition(InsertMessage.Position
                            .newBuilder()
                            .setX((int) drone.getPosition().getX())
                            .setY((int) drone.getPosition().getY())
                            .build()
                    )
                    .build();
            sendPositionToNextDrone(position);
        }

        sendElectionToNextDrone(message);
        responseObserver.onNext(InsertMessage.ElectionResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void isAlive(InsertMessage.AliveMessage request, StreamObserver<InsertMessage.AliveMessage> responseObserver) {
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }
}
