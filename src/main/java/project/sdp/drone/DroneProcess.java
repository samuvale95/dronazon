package project.sdp.drone;

import com.example.grpc.DroneServiceGrpc;
import com.example.grpc.DroneServiceGrpc.*;
import com.example.grpc.InsertMessage;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.eclipse.paho.client.mqttv3.*;
import project.sdp.server.beans.Drone;
import project.sdp.server.beans.ListDrone;
import project.sdp.server.beans.Pair;

import java.awt.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
* Ogni dorne e' un processo e non un thread
*
* I droni comunicano con gRPC
*
* Campi della classe:
* - ID
* - porta
* - indirizzo/URI server amministratore
* - battery init 100%
* I due seguenti vengono passati dal server quando il drone si registra al server
* - position
* - DroneList
*
* */
public class DroneProcess {
    private final int id;
    private final int port;
    private final String URI_AdmServer;
    private int battery;
    private Point position;
    private ListDrone dronesList;
    private Boolean master;
    private Drone masterDrone;
    private Drone nextDrone;
    private String broker;
    private Master masterProcess;
    private int deliveryCount = 0;
    private double distance = 0;

    public DroneProcess(int id, int port, String URI_AdmServer){
        this.id = id;
        this.port = port;
        this.URI_AdmServer = URI_AdmServer;
        this.battery = 100;
        this.master = false;
        this.nextDrone = new Drone(id, "localhost", port);
    }

    public void setNextDrone(Drone nextDrone) { this.nextDrone = nextDrone; }

    private void newNextNode(){
        ArrayList<Drone> list = new ArrayList<>(getDronesList());

        int start = 0, end = list.size() - 1;

        int ans = -1;
        while (start <= end) {
            int mid = (start + end) / 2;

            // Move to right side if target is
            // greater.
            if (list.get(mid).getId() <= id) {
                start = mid + 1;
            }

            // Move left side.
            else {
                ans = mid;
                end = mid - 1;
            }
        }
        if(ans==-1)
            System.out.println(list.get(0));
        else
            System.out.println(list.get(ans));

    }

    public Drone getMasterDrone(){return this.masterDrone;}

    public Drone getNextDrone() { return this.nextDrone; }

    public Drone getDrone() { return new Drone(id, "localhost", port); }

    public ArrayList<Drone> getDronesList(){ return this.dronesList.getDrones(); }

    public void addDronePosition(InsertMessage.Drone drone, InsertMessage.Position position) {
        boolean exist = false;
        for (Drone d : dronesList.getDrones()){
            if(d.getId() == drone.getId()) {
                d.setPosition(new Point(position.getX(), position.getY()));
                exist = true;
            }
        }
        if(!exist) {
            Drone d = new Drone(drone.getId(), drone.getIp(), drone.getPort());
            d.setPosition(new Point(position.getX(), position.getY()));
            dronesList.add(d);
            Collections.sort(dronesList.getDrones());
        }
    }

    private void registerToServer(){
        Client client = Client.create();
        Drone newDrone = new Drone(this.id, "localhost", this.port);

        WebResource webResource = client.resource(URI_AdmServer + "/dronazon/drone/add");
        ClientResponse clientResponse = webResource.type("application/json").post(ClientResponse.class, newDrone);

        if(clientResponse.getStatus() != 200)
        throw new RuntimeException("Something in your request is wrong " + clientResponse.getStatus());

        Gson gson = new Gson();
        String json = clientResponse.getEntity(String.class);
        Pair pair = gson.fromJson(json, Pair.class);

        this.dronesList = pair.getListDrone();
        Collections.sort(dronesList.getDrones());
        this.position = pair.getPosition();
    }

    public ManagedChannel getChannel(Drone drone) {
        return ManagedChannelBuilder.forTarget(drone.getIp() +":"+ drone.getPort()).usePlaintext().build();
    }

    public void setBroker(String broker){ this.broker = broker; }

    private void insertIntoRing() throws MqttException {
        if(dronesList.getDrones().size() == 1){
            System.out.println("I'm Drone Master");
            this.masterDrone = new Drone(id, "localhost", port);
            masterProcess = new Master(this);
            masterProcess.start();
            return;
        }


        Drone drone = getFuturePreviousNode();
        System.out.println("Try to communicate with: ");
        System.out.println(drone);
        ManagedChannel channel = getChannel(drone);
        DroneServiceBlockingStub blockingStub = DroneServiceGrpc.newBlockingStub(channel);

        InsertMessage.Drone callerDrone = InsertMessage.Drone.newBuilder().setId(id).setIp("localhost").setPort(port).build();
        InsertMessage.InsertRingRequest insertMessage = InsertMessage.InsertRingRequest.newBuilder().setCallerDrone(callerDrone).build();

        InsertMessage.InsertRingResponse insertRingResponse = blockingStub.insertIntoRing(insertMessage);
        channel.shutdown();

        InsertMessage.Drone droneNext = insertRingResponse.getNextDrone();
        InsertMessage.Drone droneMaster = insertRingResponse.getMasterDrone();
        nextDrone = new Drone(droneNext.getId(),droneNext.getIp(), droneNext.getPort());
        masterDrone = new Drone(droneMaster.getId(), droneMaster.getIp(), droneMaster.getPort());

        channel = getChannel(nextDrone);
        blockingStub = DroneServiceGrpc.newBlockingStub(channel);
        InsertMessage.Position positionMessage = InsertMessage.Position.newBuilder().setX((int) position.getX()).setY((int) position.getY()).build();
        blockingStub.sendPosition(InsertMessage.PositionRequest.newBuilder().setDrone(callerDrone).setPosition(positionMessage).build());
        channel.shutdown();
    }

    private Drone getFuturePreviousNode() {
        ArrayList<Drone> list = new ArrayList<>(getDronesList());

        int start = 0, end = list.size()-1;

        int ans = -1;
        while (start <= end) {
            int mid = (start + end) / 2;

            if (list.get(mid).getId() >= id) {
                end = mid - 1;
            }

            else {
                ans = mid;
                start = mid + 1;
            }
        }

        if(ans==-1)
            return list.get(list.size()-1);
        else
            return list.get(ans);
    }

    public void setMaster(Boolean state){ this.master = state; }

    public Master getMasterProcess() { return this.masterProcess; }

    public void makeDelivery(Delivery delivery) {
        System.out.println("****+ Making a delivery *******");
        try {
            Thread.sleep(5000);
            deliveryCount++;
            battery -= 10;
            position = delivery.getDeliveryPoint();
            distance += position.distance(delivery.getTakePoint()) + delivery.getTakePoint().distance(delivery.getDeliveryPoint());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("******** Delivery End *********");

        System.out.println("\n");
        final ManagedChannel channel = getChannel(nextDrone);
        DroneServiceBlockingStub stub = DroneServiceGrpc.newBlockingStub(channel);

        InsertMessage.InfoAndStatsRequest infoAndStatsMessage =
                InsertMessage.InfoAndStatsRequest
                .newBuilder()
                .setCallerDrone(id)
                .setBattery(battery)
                .setAirPollution(0)
                .setDistanceRoutes(distance)
                .setNewPosition(InsertMessage.Position
                        .newBuilder()
                        .setX((int) position.getX())
                        .setY((int) position.getY())
                        .build())
                .setDeliveryTimeStamp(Timestamp.from(Instant.now()).getTime())
                .setDroneTarget(masterDrone.getId())
                .setDeliveryNumber(deliveryCount)
                .build();

        if(master){
            masterProcess.getInfoAndStatsQueue().add(
                    new InfoAndStats(infoAndStatsMessage.getDeliveryTimeStamp(),
                            new Point(infoAndStatsMessage.getNewPosition().getX(),
                                    infoAndStatsMessage.getNewPosition().getY()
                            ),
                            infoAndStatsMessage.getBattery(),
                            infoAndStatsMessage.getDistanceRoutes(),
                            infoAndStatsMessage.getAirPollution(),
                            infoAndStatsMessage.getCallerDrone(),
                            infoAndStatsMessage.getDeliveryNumber()
                    )
            );
            channel.shutdown();
            return;
        }
        stub.sendInfoAfterDelivery(infoAndStatsMessage);
        channel.shutdown();
    }

    public String getBroker() {
        return this.broker;
    }

    public String getAdministratorServer(){ return this.URI_AdmServer; }

    public void start() throws IOException, MqttException {
        registerToServer();

        //Start gRPC server
        Server server = ServerBuilder.forPort(port).addService(new DroneService(this)).build();
        server.start();

        insertIntoRing();

        System.out.println("Drone started:");
        System.out.println("ID: " + id);
        System.out.println("Position: " + position);

        final ScheduledExecutorService e = Executors.newScheduledThreadPool(1);
        e.scheduleAtFixedRate(() -> {
            try {
                System.out.println("\n");
                System.out.println("Number of delivery: " + deliveryCount);
                System.out.println(dronesList.getDrones().indexOf(new Drone(this.id, "localhost", this.port)));
                newNextNode();
                System.out.println("\n");
            }
            catch(Exception err){
                System.out.println(err);
            }
        }, 0, 10*1000, TimeUnit.MILLISECONDS);

        Scanner sc = new Scanner(System.in);
        System.out.println("Write quit to exit");
        while (!sc.nextLine().equals("quit") && battery > 15){
            System.out.println("Write quit to exit");
        }

        if(master) masterProcess.shutdown();
        System.exit(0);
    }

    public static void main(String[] args) throws IOException, MqttException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Insert a integer id of drone: ");
        int id = scanner.nextInt();
        System.out.println("Insert a communication port for a drone: ");
        int port = scanner.nextInt();

        DroneProcess drone = new DroneProcess(id, port, "http://localhost:1337");
        drone.setBroker("tcp://localhost:1883");
        drone.start();
    }
}
