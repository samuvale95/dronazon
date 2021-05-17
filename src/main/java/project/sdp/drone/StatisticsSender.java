package project.sdp.drone;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import project.sdp.server.beans.Statistics;

import java.sql.Timestamp;

public class StatisticsSender extends Thread{
    private final DroneProcess droneProcess;
    private final Master master;

    public StatisticsSender(DroneProcess droneProcess){
        this.droneProcess = droneProcess;
        this.master = droneProcess.getMasterProcess();
    }

    //TODO fix global statistic calculator
    private Statistics calculateGlobalStatistics() {
        Double distance = master.getGlobalStats().stream().reduce(0.0, (acc, stats)-> acc+stats.getDistanceRoutes(), Double::sum);
        int deliveryNumber = master.getGlobalStats().stream().reduce(0, (acc, stats)-> acc+stats.getDeliveryNumber(), Integer::sum);
        double pollution = master.getGlobalStats().stream().reduce(0.0, (acc, stats)-> acc+stats.getAirPollution(), Double::sum);
        int battery = master.getGlobalStats().stream().reduce(0, (acc, stats)-> acc+stats.getBattery(), Integer::sum);
        double droneNumber = droneProcess.getDronesList().size();
        return new Statistics(deliveryNumber/droneNumber, distance/droneNumber, pollution/droneNumber, battery/droneNumber, new Timestamp(System.currentTimeMillis()).toString());
    }

    private void sendGlobalStatistics() {
        System.out.println("\n");
        System.out.println("sending global stats to server");
        System.out.println("\n");

        if(master.getGlobalStats().size() == 0) {
            System.out.println("Global stats are zero!!");
            return;
        }

        Client client = Client.create();

        Statistics statistics = calculateGlobalStatistics();

        System.out.println(statistics);

        WebResource webResource = client.resource(droneProcess.getAdministratorServer() + "/dronazon/city/statistics");
        ClientResponse clientResponse = webResource.type("application/json").post(ClientResponse.class, statistics);

        if(clientResponse.getStatus() != 200)
            throw new RuntimeException("Something in your request is wrong " + clientResponse.getStatus());

        System.out.println(clientResponse);
    }

    @Override
    public void run() {
        while(true){
            System.out.println("\n");
            System.out.println("************ SEND STATS TO SERVER **************");
            sendGlobalStatistics();
            System.out.println("************************************************");
            System.out.println("\n");
            try {
                Thread.sleep(10*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}