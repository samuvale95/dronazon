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
import project.sdp.server.beans.Drone;
import project.sdp.server.beans.ListDrone;
import project.sdp.server.beans.Pair;

import java.awt.*;
import java.io.IOException;
import java.util.Scanner;

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


    public DroneProcess(int id, int port, String URI_AdmServer){
        this.id = id;
        this.port = port;
        this.URI_AdmServer = URI_AdmServer;
        this.battery = 100;
        this.master = false;
        this.nextDrone = new Drone(id, "localhost", port);
    }

    public Boolean isMaster(){ return this.master; }

    public void setNextDrone(Drone nextDrone) { this.nextDrone = nextDrone; }

    public Drone getMasterDrone(){return this.masterDrone;}

    public Drone getNextDrone() { return this.nextDrone; }

    public Drone getDrone() { return new Drone(id, "localhost", port); }

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
        this.position = pair.getPosition();
    }

    public DroneServiceBlockingStub getBlockingStub(Drone drone) {
        final ManagedChannel managedChannel = ManagedChannelBuilder.forTarget(drone.getIp() +":"+ drone.getPort()).usePlaintext().build();
        return DroneServiceGrpc.newBlockingStub(managedChannel);
    }

    public DroneServiceStub getStub(Drone drone) {
        final ManagedChannel managedChannel = ManagedChannelBuilder.forTarget(drone.getIp() +":"+ drone.getPort()).usePlaintext().build();
        return DroneServiceGrpc.newStub(managedChannel);
    }

    //TODO implementing method to send starting position to all node
    private void insertIntoRing(){
        if(dronesList.getDrones().size() == 0){
            this.masterDrone = new Drone(this.id, "localhost", this.port);
            this.master = true;
            return;
        }

        Drone drone = dronesList.getDrones().get(0);
        System.out.println("Try to communicate with: ");
        System.out.println(drone);
        DroneServiceBlockingStub blockingStub = getBlockingStub(drone);

        InsertMessage.Drone callerDrone = InsertMessage.Drone.newBuilder().setId(id).setIp("localhost").setPort(port).build();
        InsertMessage.InsertRingRequest insertMessage = InsertMessage.InsertRingRequest.newBuilder().setCallerDrone(callerDrone).build();

        InsertMessage.InsertRingResponse insertRingResponse = blockingStub.insertIntoRing(insertMessage);

        InsertMessage.Drone droneNext = insertRingResponse.getNextDrone();
        InsertMessage.Drone droneMaster = insertRingResponse.getMasterDrone();
        nextDrone = new Drone(droneNext.getId(),droneNext.getIp(), droneNext.getPort());
        masterDrone = new Drone(droneMaster.getId(), droneMaster.getIp(), droneMaster.getPort());

        blockingStub = getBlockingStub(nextDrone);
        InsertMessage.Position positionMessage = InsertMessage.Position.newBuilder().setX((int) position.getX()).setY((int) position.getY()).build();
        blockingStub.sendPosition(InsertMessage.PositionRequest.newBuilder().setDrone(callerDrone).setPosition(positionMessage).build());
    }

    public void start() throws IOException, InterruptedException {
        registerToServer();

        //Start gRPC server
        Server server = ServerBuilder.forPort(port).addService(new DroneService(this)).build();
        server.start();

        insertIntoRing();

        while (true){
            System.out.println(nextDrone);
            System.out.println(dronesList);
            Thread.sleep(5000);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Insert a integer id of drone: ");
        int id = scanner.nextInt();
        System.out.println("Insert a communication port for a drone: ");
        int port = scanner.nextInt();
        scanner.close();

        DroneProcess drone = new DroneProcess(id, port, "http://localhost:1337");
        drone.start();
    }


}
