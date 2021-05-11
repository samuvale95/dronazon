package project.sdp.drone;

import com.example.grpc.DroneServiceGrpc;
import com.example.grpc.DroneServiceGrpc.*;
import com.example.grpc.InsertMessageOuterClass.*;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
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
    private Boolean isMaster;
    private Drone nextDrone;


    public DroneProcess(int id, int port, String URI_AdmServer){
        this.id = id;
        this.port = port;
        this.URI_AdmServer = URI_AdmServer;
        this.battery = 100;
        this.isMaster = false;
        this.nextDrone = new Drone(id, "localhost", port);
    }

    public void setNextDrone(Drone nextDrone) {
        this.nextDrone = nextDrone;
    }

    public Drone getDrone() {
        return new Drone(id, "localhost", port);
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

    private void insertIntoRing(){
        if(dronesList.getDrones().size() == 0){
        System.out.println("Im alone in a City");
        return;
        }

        Drone drone = dronesList.getDrones().get(0);
        System.out.println("Try to communicate with: ");
        System.out.println(drone);
        final ManagedChannel managedChannel = ManagedChannelBuilder.forTarget(drone.getIp() +":"+drone.getPort()).usePlaintext().build();
        DroneServiceStub droneServiceStub = DroneServiceGrpc.newStub(managedChannel);

        InsertMessage.drone callerDrone = InsertMessage.drone.newBuilder().setId(id).setIp("localhost").setPort(port).build();
        InsertMessage insertMessage = InsertMessage.newBuilder().setCallerDrone(callerDrone).build();

        droneServiceStub.insertIntoRing(insertMessage, new StreamObserver<InsertMessage>() {
            @Override
            public void onNext(InsertMessage value) {
                InsertMessage.drone drone = value.getCallerDrone();
                nextDrone = new Drone(drone.getId(),drone.getIp(), drone.getPort());
                System.out.println("My next drone is: " + nextDrone);
            }

            @Override
            public void onError(Throwable t) {

                for (StackTraceElement stackTraceElement : t.getStackTrace()) {
                    System.err.println(stackTraceElement);
                }
                System.err.println(t.toString());
            }

            @Override
            public void onCompleted() {
                System.out.println("COMPLETED CALL");
            }
        });
    }

    public void start() throws IOException, InterruptedException {
        registerToServer();

        Server server = ServerBuilder.forPort(port).addService(new DroneService(this)).build();
        server.start();

        insertIntoRing();

        while (true){}
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
