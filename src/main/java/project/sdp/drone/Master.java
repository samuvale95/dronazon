package project.sdp.drone;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;
import project.sdp.dronazon.RandomDelivery;

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
            System.out.println(globalStats);
            System.out.println("************************************************");
            System.out.println("\n");
        }, 0, 10*1000, TimeUnit.MILLISECONDS);
    }
}
