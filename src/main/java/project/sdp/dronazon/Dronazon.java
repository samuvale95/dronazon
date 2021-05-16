package project.sdp.dronazon;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;

public class Dronazon extends Thread{
    private final int length;
    private final int height;
    private MqttClient mqttClient;
    private final String topic;
    private final String broker;
    private final int qos;

    public Dronazon(int length, int height, String topic, String broker, int qos) {
        this.length = length;
        this.height = height;
        this.topic = topic;
        this.broker = broker;
        this.qos = qos;
    }

    @Override
    public void run() {
        MqttClient client = null;
        try {
             client = new MqttClient(broker, MqttClient.generateClientId());
             MqttConnectOptions options = new MqttConnectOptions();
             options.setCleanSession(true);
             client.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        assert client != null;
        client.setCallback(new MqttCallback() {
            //TODO need to implement this method with something with sense
            @Override
            public void connectionLost(Throwable cause) {}

            @Override
            public void messageArrived(String topic, MqttMessage message) {}

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                if(token.isComplete()) {
                    try {
                        System.out.println("New delivery generated: " + token.getMessage());
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            }
        });


        while(true){
            Gson gson = new Gson();
            try {
                MqttMessage message = new MqttMessage();
                String json = gson.toJson(new RandomDelivery(length, height));
                System.out.println(json);
                message.setPayload(json.getBytes());
                message.setQos(qos);
                client.publish(topic, message);

                Thread.sleep(10*1000);
            } catch (InterruptedException | MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new Dronazon(10, 10, "dronazon/smarcity/orders", "tcp://localhost:1883", 2).start();
    }
}
