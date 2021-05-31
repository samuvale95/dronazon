package project.sdp.drone;

import com.example.grpc.DroneServiceGrpc;
import com.example.grpc.InsertMessage;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.eclipse.paho.client.mqttv3.MqttException;
import project.sdp.server.beans.Drone;

import java.awt.*;
import java.util.concurrent.TimeUnit;

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
        Context.current().fork().run(() -> {
            ManagedChannel channel = droneProcess.getChannel(droneProcess.getNextDrone());
            DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
            stub.sendPosition(request, new StreamObserver<InsertMessage.PositionResponse>() {
                @Override
                public void onNext(InsertMessage.PositionResponse value) {

                }

                @Override
                public void onError(Throwable t) {
                    droneProcess.onFailNode(t);
                }

                @Override
                public void onCompleted() {
                    channel.shutdown();
                }
            });
            try {
                channel.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        responseObserver.onNext(InsertMessage.PositionResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendDelivery(InsertMessage.DeliveryRequest request, StreamObserver<InsertMessage.DeliveryResponse> responseObserver) {
        if(request.getDelivery().getDroneTarget().getId() == droneProcess.getDrone().getId()){
            responseObserver.onNext(InsertMessage.DeliveryResponse.newBuilder().build());
            responseObserver.onCompleted();

            InsertMessage.Delivery newDelivery = request.getDelivery();
            Point deliveryPosition = new Point(newDelivery.getDeliveryPoint().getX(), newDelivery.getDeliveryPoint().getY());
            Point takePosition = new Point(newDelivery.getTakePoint().getX(), newDelivery.getTakePoint().getY());
            try {
                droneProcess.makeDelivery(new Delivery(request.getDelivery().getId(), takePosition, deliveryPosition));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }

        Context.current().fork().run(() -> {
            ManagedChannel channel = droneProcess.getChannel(droneProcess.getNextDrone());
            DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
            stub.sendDelivery(request, new StreamObserver<InsertMessage.DeliveryResponse>() {
                @Override
                public void onNext(InsertMessage.DeliveryResponse value) {}

                @Override
                public void onError(Throwable t) {
                    droneProcess.onFailNode(t);
                }

                @Override
                public void onCompleted() {
                    channel.shutdown();
                }
            });
            try {
                channel.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        responseObserver.onNext(InsertMessage.DeliveryResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendInfoAfterDelivery(InsertMessage.InfoAndStatsRequest request, StreamObserver<InsertMessage.InfoAndStatsResponse> responseObserver) {
        System.out.println("Message STATS arrived  from previous drone");
        if(droneProcess.getDrone().getId() == request.getDroneTarget()){
            System.out.println("PRINT MESSAGE");
            System.out.println(request);
            droneProcess.getMasterProcess().getInfoAndStatsQueue().add(
                    new InfoAndStats(request.getDeliveryTimeStamp(),
                            new Point(request.getNewPosition().getX(),
                                    request.getNewPosition().getY()
                            ),
                            request.getBattery(),
                            request.getDistanceRoutes(),
                            request.getAirPollution(),
                            request.getCallerDrone(),
                            request.getDeliveryNumber()
                    )
            );
            responseObserver.onNext(InsertMessage.InfoAndStatsResponse.newBuilder().build());
            responseObserver.onCompleted();
            return;
        }
        //TODO update position of drones in all nodes
        Context.current().fork().run(() -> {
            ManagedChannel channel = droneProcess.getChannel(droneProcess.getNextDrone());
            DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
            stub.sendInfoAfterDelivery(request, new StreamObserver<InsertMessage.InfoAndStatsResponse>() {
                @Override
                public void onNext(InsertMessage.InfoAndStatsResponse value) {}

                @Override
                public void onError(Throwable t) {
                    droneProcess.onFailNode(t);
                }

                @Override
                public void onCompleted() {
                    System.out.println("STATS sent to next drone");
                    channel.shutdown();
                }
            });
            try {
                channel.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        responseObserver.onNext(InsertMessage.InfoAndStatsResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void election(InsertMessage.ElectionRequest request, StreamObserver<InsertMessage.ElectionResponse> responseObserver) {
        InsertMessage.ElectionRequest message;

        if(request.getType().equals("ELECTION")) {
            System.err.println("ELECTION IN ACTION");
            if (request.getBattery() == droneProcess.getDrone().getBattery() && request.getId() == droneProcess.getDrone().getId()) {
                message = InsertMessage.ElectionRequest
                        .newBuilder()
                        .setId(droneProcess.getDrone().getId())
                        .setBattery(droneProcess.getDrone().getBattery())
                        .setType("ELECTED")
                        .build();
                droneProcess.setParticipantToElection(false);
            }else if (request.getBattery() > droneProcess.getDrone().getBattery() ||
                    (request.getBattery() == droneProcess.getDrone().getBattery() && request.getId() > droneProcess.getDrone().getId())) {
                System.err.println("FORWARD");
                message = request;
                droneProcess.setParticipantToElection(true);
            }else{
                System.err.println("FORWARD with new packet");
                message = InsertMessage.ElectionRequest
                        .newBuilder()
                        .setId(droneProcess.getDrone().getId())
                        .setBattery(droneProcess.getDrone().getBattery())
                        .setType("ELECTION")
                        .build();
                droneProcess.setParticipantToElection(true);
            }
        }else{
            if(request.getBattery() == droneProcess.getDrone().getBattery() && request.getId() == droneProcess.getDrone().getId()){
                System.err.println("SET NEW MASTER");
                droneProcess.setMaster(true);
                droneProcess.setMasterNode(new Drone(droneProcess.getDrone().getId(), droneProcess.getDrone().getIp(), droneProcess.getDrone().getPort()));
                try {
                    droneProcess.setMasterProcess(new Master(droneProcess));
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                droneProcess.getMasterProcess().start();
                return;
            }

            message = request;
            droneProcess.setParticipantToElection(false);
        }

        Context.current().fork().run(()-> {
            Drone master = droneProcess.getDrone(message.getId());
            droneProcess.setMasterNode(master);

            ManagedChannel channel = droneProcess.getChannel(droneProcess.getNextDrone());

            DroneServiceGrpc.DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
            stub.election(message, new StreamObserver<InsertMessage.ElectionResponse>() {
                @Override
                public void onNext(InsertMessage.ElectionResponse value) { }

                @Override
                public void onError(Throwable t) {
                    droneProcess.onFailNode(t);
                }

                @Override
                public void onCompleted() { channel.shutdown(); }
            });

            Drone drone = droneProcess.getDrone();
            InsertMessage.PositionRequest position = InsertMessage.PositionRequest
                    .newBuilder()
                    .setPosition(InsertMessage.Position
                            .newBuilder()
                            .setX((int) drone.getPosition().getX())
                            .setY((int) drone.getPosition().getY())
                            .build()
                    )
                    .setDrone(InsertMessage.Drone
                            .newBuilder()
                            .setId(drone.getId())
                            .setPort(drone.getPort())
                            .setIp(drone.getIp())
                    )
                    .build();

            stub.sendPosition(position, new StreamObserver<InsertMessage.PositionResponse>() {
                @Override
                public void onNext(InsertMessage.PositionResponse value) { }

                @Override
                public void onError(Throwable t) { droneProcess.onFailNode(t); }

                @Override
                public void onCompleted() { }
            });

            try {
                channel.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        responseObserver.onNext(InsertMessage.ElectionResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void isAlive(InsertMessage.AliveMessage request, StreamObserver<InsertMessage.AliveMessage> responseObserver) {
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }
}
