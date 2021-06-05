package project.sdp.drone;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;
import project.sdp.dronazon.RandomDelivery;
import project.sdp.server.beans.Drone;
import java.util.ArrayList;


public class Master extends Thread{

    private static final String DELIVERY_TOPIC = "dronazon/smarcity/orders";
    private final DroneProcess droneProcess;
    private final Buffer<project.sdp.dronazon.Delivery> deliveryQueue;
    private final Buffer<InfoAndStats> infoAndStatsQueue;
    private final ArrayList<InfoAndStats> globalStats;
    private final MqttClient client;



    public Master(DroneProcess droneProcess) throws MqttException {
        this.droneProcess = droneProcess;
        this.deliveryQueue = new Buffer<>();
        this.infoAndStatsQueue = new Buffer<>();
        this.globalStats = new ArrayList<>();

        this.client = new MqttClient(droneProcess.getBroker(), MqttClient.generateClientId());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        client.connect(options);

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
        client.subscribe(DELIVERY_TOPIC);
    }

    public Buffer<project.sdp.dronazon.Delivery> getDeliveryQueue() { return this.deliveryQueue; }

    public Buffer<InfoAndStats> getInfoAndStatsQueue(){ return this.infoAndStatsQueue; }

    public ArrayList<InfoAndStats> getGlobalStats() {
            return globalStats;
    }

    public void shutdown() throws MqttException, InterruptedException {
        client.disconnect();
        client.close();

        ArrayList<Drone> list;
        synchronized (droneProcess.getDronesList()) {
            list = new ArrayList<>(droneProcess.getDronesList());
        }

        while(list.stream().map(Drone::getCommittedToDelivery).reduce(false, (d1, d2) -> d1||d2 )){
            synchronized (this) {
                System.err.println("WAITING");
                this.wait();
            }
            synchronized (droneProcess.getDronesList()) {
                list = new ArrayList<>(droneProcess.getDronesList());
            }
        }
    }

    @Override
    public void run() {
        droneProcess.setMaster(true);
        new DeliveryHandler(droneProcess).start();
        new InfoAndStatsHandler(droneProcess).start();
        new StatisticsSender(droneProcess).start();
    }

}
