package project.sdp.drone;

import com.example.grpc.DroneServiceGrpc;
import com.example.grpc.DroneServiceGrpc.*;
import com.example.grpc.InsertMessage;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
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
import java.util.logging.Logger;

public class DroneProcess {
    private final int id;
    private final int port;
    private final String URI_AdmServer;
    private volatile int battery;
    private Point position;
    private final Object positionSync = new Object();
    private final ListDrone dronesList;
    private boolean master;
    private Drone masterDrone;
    private Drone nextDrone;
    private final Object nextDroneSync = new Object();
    private final String broker;
    private final Master masterProcess;
    private int deliveryCount = 0;
    private double distance = 0;
    private final ArrayList<Double> pm10means;
    private final Object propertySync = new Object();
    private final static Logger LOGGER = Logger.getLogger(DroneProcess.class.getName());


    public DroneProcess(int id, int port, String URI_AdmServer, String broker) {
        this.id = id;
        this.port = port;
        this.URI_AdmServer = URI_AdmServer;
        this.battery = 100;
        this.master = false;
        this.nextDrone = new Drone(id, "localhost", port);
        this.pm10means = new ArrayList<>();
        this.dronesList = new ListDrone();
        this.broker = broker;
        this.masterProcess = new Master(this);
    }

    public void setNextDrone(Drone nextDrone) {
        synchronized (nextDroneSync) {
            this.nextDrone = nextDrone;
        }
    }

    public void newNextNode(){
        ArrayList<Drone> list = getDronesListCopy();

        int index = list.indexOf(getDrone());
        System.err.println("list: " + list);
        System.err.println("index:" + index);
        synchronized (nextDroneSync) {
            this.nextDrone = list.get((index + 1) % list.size());
        }
    }

    private ArrayList<Drone> getDronesListCopy() {
        ArrayList<Drone> list;
        synchronized (dronesList) {
            list = new ArrayList<>(getDronesList());
        }
        return list;
    }

    public Drone getMasterDrone(){return this.masterDrone;}

    public Drone getNextDrone() {
        synchronized (nextDroneSync){
            return this.nextDrone;
        }
    }

    public Drone getDrone() {
        Drone t = new Drone(id, "localhost", port);
        synchronized (positionSync){
            t.setPosition(position);
        }
        return t;
    }

    public Drone getDrone(int id) {
        Drone res=null;
        for( Drone drone : new ArrayList<>(getDronesList()))
            if(drone.getId()==id) res = drone;

        assert res != null;
        return res;
    }


    public ArrayList<Drone> getDronesList(){
        synchronized (dronesList) {
            return this.dronesList.getDrones();
        }
    }

    public void addDronePosition(InsertMessage.Drone drone, InsertMessage.Position position) {
        synchronized (dronesList) {
            boolean exist = false;
            for (Drone d : dronesList.getDrones()) {
                if (d.getId() == drone.getId()) {
                    d.setPosition(new Point(position.getX(), position.getY()));
                    exist = true;
                }
            }
            if (!exist) {
                System.err.println("New Drone enter into Ring");
                Drone d = new Drone(drone.getId(), drone.getIp(), drone.getPort());
                d.setPosition(new Point(position.getX(), position.getY()));
                dronesList.add(d);
                Collections.sort(dronesList.getDrones());
                synchronized (masterProcess.getDeliveryHandler()){
                    masterProcess.getDeliveryHandler().notify();
                }
            }
        }
    }

    private synchronized void registerToServer(){
        Client client = Client.create();
        Drone newDrone = new Drone(this.id, "localhost", this.port);

        WebResource webResource = client.resource(URI_AdmServer + "/dronazon/drone/add");
        ClientResponse clientResponse = null;
        try {
            clientResponse = webResource.type("application/json").post(ClientResponse.class, newDrone);
        }catch (ClientHandlerException e){
            System.out.println("Server not answer!!");
            System.exit(0);
        }

        assert clientResponse != null;
        if(clientResponse.getStatus() == 403){
            System.out.println("Duplicate ID your request, couldn't be accepted");
            System.out.println("Insert another ID");
            System.exit(0);
        }

        Gson gson = new Gson();
        String json = clientResponse.getEntity(String.class);
        Pair pair = gson.fromJson(json, Pair.class);

        this.dronesList.setDrones(pair.getListDrone().getDrones());
        Collections.sort(dronesList.getDrones());
        this.position = pair.getPosition();
    }

    public ManagedChannel getChannel(Drone drone) {
        return ManagedChannelBuilder.forTarget(drone.getIp() +":"+ drone.getPort()).usePlaintext().build();
    }

    private synchronized void insertIntoRing() throws InterruptedException {
        if(dronesList.getDrones().size() == 1){
            System.out.println("I'm Drone Master");
            this.masterDrone = new Drone(id, "localhost", port);
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
        channel.shutdown().awaitTermination(1, TimeUnit.MINUTES);

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
        channel1.awaitTermination(1, TimeUnit.MINUTES);
    }

    private Drone getFuturePreviousNode() {
        ArrayList<Drone> list = getDronesListCopy();
        int index = list.indexOf(getDrone());
        System.err.println("list: " + list);
        System.err.println("index:" + index);
        synchronized (nextDroneSync) {
            return list.get((index - 1) % list.size());
        }
    }

    public void setMaster(Boolean state){
        this.master = state;
    }

    public Master getMasterProcess() {
        return this.masterProcess;
    }

    public void makeDelivery(Delivery delivery) throws InterruptedException, MqttException {
        System.out.println("****+ Making a delivery *******");
        try {
            Thread.sleep(5000);
            synchronized (propertySync) {
                deliveryCount++;
                battery -= 10;
                position = delivery.getDeliveryPoint();
                distance += position.distance(delivery.getTakePoint()) + delivery.getTakePoint().distance(delivery.getDeliveryPoint());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("******** Delivery End *********");

        System.out.println("\n");
        InsertMessage.InfoAndStatsRequest infoAndStatsMessage;
        synchronized (this) {
            infoAndStatsMessage = InsertMessage.InfoAndStatsRequest
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
            if (master) {
                masterProcess.getInfoAndStatsQueue().add(
                        new InfoAndStats(infoAndStatsMessage.getDeliveryTimeStamp(),
                                new Point(infoAndStatsMessage.getNewPosition().getX(),
                                        infoAndStatsMessage.getNewPosition().getY()
                                ),
                                infoAndStatsMessage.getBattery(),
                                infoAndStatsMessage.getDistanceRoutes(),
                                infoAndStatsMessage.getAirPollutionList(),
                                infoAndStatsMessage.getCallerDrone(),
                                infoAndStatsMessage.getDeliveryNumber()
                        )
                );
                return;
            }
        }

            final ManagedChannel channel = getChannel(nextDrone);
            DroneServiceStub stub = DroneServiceGrpc.newStub(channel);
            stub.sendInfoAfterDelivery(infoAndStatsMessage, new StreamObserver<InsertMessage.InfoAndStatsResponse>() {
                @Override
                public void onNext(InsertMessage.InfoAndStatsResponse value) {
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

            channel.awaitTermination(1, TimeUnit.MINUTES);
            if(battery < 15)
                this.close();
    }

    private void recoverFromNodeFailure(Drone drone) throws MqttException {
        synchronized (dronesList) {
            getDronesList().remove(drone);
            if (getDronesList().size() == 1) {
                if(!master) {
                    setMasterNode(getDrone());
                    setMaster(true);
                    masterProcess.start();
                }
                setNextDrone(getDrone());
                return;
            }
        }
        newNextNode();
    }

    public String getBroker() {
        return this.broker;
    }

    public String getAdministratorServer(){ return this.URI_AdmServer; }

    public void setMasterNode(Drone drone) { this.masterDrone = drone; }

    public void onFailNode(Throwable t) {
        System.err.println("ERROR, next done failed " + getNextDrone());
        try {
            Drone failNode;
            synchronized (nextDroneSync) {
                failNode = getNextDrone();
            }
            if (t instanceof StatusRuntimeException && ((StatusRuntimeException) t).getStatus().getCode() == Status.UNAVAILABLE.getCode()) {
                System.err.println("Setting new next node");
                recoverFromNodeFailure(failNode);
            }

            if (failNode.getId() == getMasterDrone().getId()) {
                System.err.println("Starting new election");
                startNewElection();
            }

        } catch (InterruptedException | MqttException e) {
            e.printStackTrace();
        }
    }

    private void startNewElection() throws InterruptedException, MqttException {
        InsertMessage.ElectionRequest message;
        synchronized (propertySync) {
            message = InsertMessage.ElectionRequest
                    .newBuilder()
                    .setId(id)
                    .setBattery(battery)
                    .setType("ELECTION")
                    .build();
        }
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

        channel.awaitTermination(1, TimeUnit.MINUTES);
    }

    private void startPM10Sensor() {
        SensorBuffer sensorBuffer = new SensorBuffer(buffer -> pm10means.add(((SensorBuffer) buffer).readAllAndClean().stream().map(Measurement::getValue).reduce(0.0, Double::sum)/8.0));
        new PM10Simulator(sensorBuffer).start();
    }

    public boolean isMaster() {
        return master;
    }

    private void close() throws MqttException, InterruptedException {
        if(master) masterProcess.shutdown();

        Client client = Client.create();
        WebResource webResource = client.resource(URI_AdmServer + "/dronazon/drone/remove/"+id);
        ClientResponse clientResponse = webResource.type("application/json").delete(ClientResponse.class);
        if(clientResponse.getStatus() == 200)
            System.out.println("Drone Removed");
        System.exit(0);
    }

    public void start() throws InterruptedException, MqttException {
        registerToServer();

        //Start gRPC server
        try {
            Server server = ServerBuilder.forPort(port).addService(new DroneService(this)).build();
            server.start();
        }catch (IOException e){
            if(e.getMessage().equals("Failed to bind")){
                System.err.println("Error on bind PORT, retry with another one");
                close();
            }
        }


        insertIntoRing();

        startPM10Sensor();

        System.out.println("Drone started:");
        System.out.println("ID: " + id);
        System.out.println("Position: " + position);

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            System.out.println("\n");
            System.out.println("PRINTING DRONE INFO");
            System.out.println("Drone id: " + id);
            System.out.println("Number of delivery: " + deliveryCount);
            System.out.println("Next drone: " + nextDrone);
            System.out.println("Master drone: " + masterDrone);
            System.out.println(getDronesList());
            System.out.println("\n");
        }, 0, 10*1000, TimeUnit.MILLISECONDS);


        new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            System.out.println("Write quit to exit");
            while (!sc.nextLine().equals("quit")){
                System.out.println("Write quit to exit");
            }
            try {
                DroneProcess.this.close();
            } catch (MqttException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            synchronized (nextDroneSync) {
                if (getNextDrone().getId() == getDrone().getId()) return;
            }

            System.err.println(getNextDrone());
            ManagedChannel channel = getChannel(getNextDrone());
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
    }

    public static void main(String[] args) throws IOException, MqttException, InterruptedException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Insert a integer id of drone: ");
        int id=-1;
        boolean isInt = false;
        while(!isInt){
           try{
               id=Integer.parseInt(scanner.nextLine());
               isInt = true;
           }catch (NumberFormatException nfe){
               System.err.println("ERR: Inserire un intero");
           }
        }


        System.out.println("Insert a communication port for a drone: ");
        int port=-1;
        isInt = false;
        while(!isInt){
            try{
                port=Integer.parseInt(scanner.nextLine());
                isInt = true;
            }catch (NumberFormatException nfe){
                System.err.println("ERR: Inserire un intero");
            }
        }

        DroneProcess drone = new DroneProcess(id, port, "http://localhost:1337", "tcp://localhost:1883");
        drone.start();
    }
}
