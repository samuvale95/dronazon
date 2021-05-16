package project.sdp.drone;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.eclipse.paho.client.mqttv3.*;
import project.sdp.dronazon.RandomDelivery;
import project.sdp.server.beans.Statistics;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Master extends Thread{

    private static final String DELIVERY_TOPIC = "dronazon/smarcity/orders";
    private final DroneProcess droneProcess;
    private final Buffer<project.sdp.dronazon.Delivery> deliveryQueue;
    private final Buffer<InfoAndStats> infoAndStatsQueue;
    private final ArrayList<InfoAndStats> globalStats;



    public Master(DroneProcess droneProcess){
        this.droneProcess = droneProcess;
        this.deliveryQueue = new Buffer<>();
        this.infoAndStatsQueue = new Buffer<>();
        this.globalStats = new ArrayList<>();
    }

    public Buffer<project.sdp.dronazon.Delivery> getDeliveryQueue() { return this.deliveryQueue; }
    public Buffer<InfoAndStats> getInfoAndStatsQueue(){ return this.infoAndStatsQueue; }
    public ArrayList<InfoAndStats> getGlobalStats() {
        synchronized (globalStats) {
            return globalStats;
        }
    }

    @Override
    public void run() {
        droneProcess.setMaster(true);

        DeliveryHandler deliveryHandler = new DeliveryHandler(droneProcess);
        deliveryHandler.start();

        MqttClient client = null;
        try {
            client = new MqttClient(droneProcess.getBroker(), MqttClient.generateClientId());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        assert client != null;
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {}

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                Gson gson = new Gson();
                RandomDelivery delivery = gson.fromJson(new String(message.getPayload()), RandomDelivery.class);
                System.out.println("******* New delivery arrived ********");
                System.out.println(delivery);
                System.out.println("*************************************");
                deliveryQueue.add(delivery);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {}
        });

        try {
            client.subscribe(DELIVERY_TOPIC);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        InfoAndStatsHandler infoAndStatsHandler = new InfoAndStatsHandler(droneProcess);
        infoAndStatsHandler.start();

        final ScheduledExecutorService e = Executors.newScheduledThreadPool(1);

        e.scheduleAtFixedRate(() -> {
            System.out.println("\n");
            System.out.println("************ SEND STATS TO SERVER **************");
            sendGlobalStatistics();
            System.out.println("************************************************");
            System.out.println("\n");
        }, 0, 10*1000, TimeUnit.MILLISECONDS);
    }

    private void sendGlobalStatistics() {
        Client client = Client.create();

        Statistics statistic = calculateGlobalStatistics();

        WebResource webResource = client.resource(droneProcess.getAdministratorServer() + "city/statistics");
        ClientResponse clientResponse = webResource.type("application/json").post(ClientResponse.class, statistic);

        if(clientResponse.getStatus() != 200)
            throw new RuntimeException("Something in your request is wrong " + clientResponse.getStatus());

        System.out.println(clientResponse);
    }

    private Statistics calculateGlobalStatistics() {
        Double distance = globalStats.stream().reduce(0.0, (acc, stats)-> acc+stats.getDistanceRoutes(), Double::sum);
        int deliveryNumber = globalStats.stream().reduce(0, (acc, stats)-> acc+stats.getDeliveryNumber(), Integer::sum);
        double pollution = globalStats.stream().reduce(0.0, (acc, stats)-> acc+stats.getAirPollution(), Double::sum);
        int battery = globalStats.stream().reduce(0, (acc, stats)-> acc+stats.getBattery(), Integer::sum);
        double droneNumber = droneProcess.getDronesList().size();
        return new Statistics(deliveryNumber/droneNumber, distance/droneNumber, pollution/droneNumber, battery/droneNumber, Timestamp.from(Instant.now()));
    }

}
