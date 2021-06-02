package project.sdp.drone;

import com.example.grpc.DroneServiceGrpc;
import com.example.grpc.DroneServiceGrpc.*;
import com.example.grpc.InsertMessage;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import org.eclipse.paho.client.mqttv3.*;
import project.sdp.sensor.Measurement;
import project.sdp.sensor.PM10Simulator;
import project.sdp.sensor.SensorBuffer;
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
    private volatile int battery;
    private Point position;
    private ListDrone dronesList;
    private boolean master;
    private Drone masterDrone;
    private Drone nextDrone;
    private String broker;
    private Master masterProcess;
    private int deliveryCount = 0;
    private double distance = 0;
    private volatile Quit quit;
    private final ArrayList<Double> pm10means;


    public DroneProcess(int id, int port, String URI_AdmServer){
        this.id = id;
        this.port = port;
        this.URI_AdmServer = URI_AdmServer;
        this.battery = 100;
        this.master = false;
        this.nextDrone = new Drone(id, "localhost", port);
        this.quit = new Quit();
        this.pm10means = new ArrayList<>();
    }

    public void setNextDrone(Drone nextDrone) { this.nextDrone = nextDrone; }

    public void newNextNode(){
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
        if(ans==-1) {
            this.nextDrone = list.get(0);
        }
        else {
            this.nextDrone = list.get(ans);
        }

    }

    public Drone getMasterDrone(){return this.masterDrone;}

    public Drone getNextDrone() { return this.nextDrone; }

    public Drone getDrone() {
        Drone t = new Drone(id, "localhost", port);
        t.setPosition(position);
        return t;
    }

    public Drone getDrone(int id) {
        Drone res=null;
        for( Drone drone : new ArrayList<>(getDronesList()))
            if(drone.getId()==id) res = drone;

        assert res != null;
        return res;
    }


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

    private void insertIntoRing() throws MqttException, InterruptedException {
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
        channel.shutdown().awaitTermination(3, TimeUnit.SECONDS);

        InsertMessage.Drone droneNext = insertRingResponse.getNextDrone();
        InsertMessage.Drone droneMaster = insertRingResponse.getMasterDrone();
        nextDrone = new Drone(droneNext.getId(),droneNext.getIp(), droneNext.getPort());
        masterDrone = new Drone(droneMaster.getId(), droneMaster.getIp(), droneMaster.getPort());

        ManagedChannel channel1 = getChannel(nextDrone);
        DroneServiceStub stub = DroneServiceGrpc.newStub(channel1);
        InsertMessage.Position positionMessage = InsertMessage.Position.newBuilder().setX((int) position.getX()).setY((int) position.getY()).build();
        stub.sendPosition(InsertMessage.PositionRequest.newBuilder().setDrone(callerDrone).setPosition(positionMessage).build(), new StreamObserver<InsertMessage.PositionResponse>() {
            @Override
            public void onNext(InsertMessage.PositionResponse value) {

            }

            @Override
            public void onError(Throwable t) {
                onFailNode(t);
            }

            @Override
            public void onCompleted() {
                channel1.shutdown();
            }
        });
        channel1.awaitTermination(3, TimeUnit.SECONDS);
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

    public void makeDelivery(Delivery delivery) throws InterruptedException {
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

        InsertMessage.InfoAndStatsRequest infoAndStatsMessage =
                InsertMessage.InfoAndStatsRequest
                .newBuilder()
                .setCallerDrone(id)
                .setBattery(battery)
                .addAllAirPollution(pm10means)
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
                            infoAndStatsMessage.getAirPollutionList().stream().reduce(0.0, Double::sum)/(double) infoAndStatsMessage.getAirPollutionCount(),
                            infoAndStatsMessage.getCallerDrone(),
                            infoAndStatsMessage.getDeliveryNumber()
                    )
            );
            return;
        }

        final ManagedChannel channel = getChannel(nextDrone);
        DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
        stub.sendInfoAfterDelivery(infoAndStatsMessage, new StreamObserver<InsertMessage.InfoAndStatsResponse>() {
            @Override
            public void onNext(InsertMessage.InfoAndStatsResponse value) {}

            @Override
            public void onError(Throwable t) {
                onFailNode(t);
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }
        });

        channel.awaitTermination(3, TimeUnit.SECONDS);
    }

    private void recoverFromNodeFailure(Drone drone) {
        getDronesList().remove(drone);
        newNextNode();
    }

    public String getBroker() {
        return this.broker;
    }

    public String getAdministratorServer(){ return this.URI_AdmServer; }

    public void setMasterProcess(Master master) {
        this.masterProcess = master;
    }

    public void setMasterNode(Drone drone) {
        this.masterDrone = drone;
    }

    public void onFailNode(Throwable t) {
        Drone failNode = getNextDrone();
        if(t instanceof StatusRuntimeException && ((StatusRuntimeException) t).getStatus().getCode() == Status.UNAVAILABLE.getCode()) {
            System.err.println("ERROR on send infoAfterDelivery");
            recoverFromNodeFailure(failNode);
            System.err.println("new next Drone: " + getNextDrone());
        }

        if(failNode.getId() == getMasterDrone().getId())
            try {
                startNewElection();
            } catch (InterruptedException | MqttException e) {
                e.printStackTrace();
            }
    }

    private void startNewElection() throws InterruptedException, MqttException {
        InsertMessage.ElectionRequest message = InsertMessage.ElectionRequest
                .newBuilder()
                .setId(id)
                .setBattery(battery)
                .setType("ELECTION")
                .build();
        final ManagedChannel channel = getChannel(nextDrone);
        DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
        stub.election(message, new StreamObserver<InsertMessage.ElectionResponse>() {
            @Override
            public void onNext(InsertMessage.ElectionResponse value) {

            }

            @Override
            public void onError(Throwable t) {
                onFailNode(t);
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }
        });

        channel.awaitTermination(3, TimeUnit.SECONDS);
    }

    private void startPM10Sensor() {
        SensorBuffer sensorBuffer = new SensorBuffer(buffer -> pm10means.add(buffer.readAllAndClean().stream().map(Measurement::getValue).reduce(0.0, Double::sum)/8.0));
        new PM10Simulator(sensorBuffer).start();
    }

    public void start() throws IOException, MqttException, InterruptedException {
        registerToServer();

        //Start gRPC server
        Server server = ServerBuilder.forPort(port).addService(new DroneService(this)).build();
        server.start();

        insertIntoRing();

        startPM10Sensor();

        System.out.println("Drone started:");
        System.out.println("ID: " + id);
        System.out.println("Position: " + position);

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            try {
                System.out.println("\n");
                System.out.println("Drone id: " + id);
                System.out.println("Number of delivery: " + deliveryCount);
                System.out.println("Next drone: " + nextDrone);
                System.out.println("\n");
            }
            catch(Exception err){
                System.out.println(err);
            }
        }, 0, 10*1000, TimeUnit.MILLISECONDS);


        new Thread(new Runnable() {
            final Quit quit = DroneProcess.this.quit;

            @Override
            public void run() {
                Scanner sc = new Scanner(System.in);
                System.out.println("Write quit to exit");
                while (!sc.nextLine().equals("quit")){
                    System.out.println("Write quit to exit");
                }
                quit.setQuit(true);
            }
        }).start();

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            System.err.println(getNextDrone());
            ManagedChannel channel = getChannel(getMasterDrone());
            DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
            stub.isAlive(InsertMessage.AliveMessage.newBuilder().build(), new StreamObserver<InsertMessage.AliveMessage>() {
                @Override
                public void onNext(InsertMessage.AliveMessage value) {}

                @Override
                public void onError(Throwable t) {
                    System.err.println("Fail on PING");
                    onFailNode(t);
                }

                @Override
                public void onCompleted() { channel.shutdown(); }
            });
            try {
                channel.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 0, 7*1000, TimeUnit.MILLISECONDS);

        while (!this.quit.isQuit() && battery > 15);

        if(master) masterProcess.shutdown();

        Client client = Client.create();
        WebResource webResource = client.resource(URI_AdmServer + "/dronazon/drone/remove/"+id);
        ClientResponse clientResponse = webResource.type("application/json").delete(ClientResponse.class);
        if(clientResponse.getStatus() == 200)
            System.out.println("Drone Removed");
        System.exit(0);
    }

    public static void main(String[] args) throws IOException, MqttException, InterruptedException {
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
