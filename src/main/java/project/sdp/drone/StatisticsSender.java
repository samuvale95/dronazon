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

    private Statistics calculateGlobalStatistics() {
        Double distance = master.getGlobalStats().stream().map(InfoAndStats::getDistanceRoutes).reduce(0.0, Double::sum);
        int deliveryNumber = master.getGlobalStats().stream().map(InfoAndStats::getDeliveryNumber).reduce(0, Integer::sum);
        double pollution = master.getGlobalStats().stream().map(InfoAndStats::getAirPollution).reduce(0.0, Double::sum);
        int battery = master.getGlobalStats().stream().map(InfoAndStats::getBattery).reduce(0, Integer::sum);
        double droneNumber = droneProcess.getDronesList().size();
        return new Statistics(deliveryNumber/droneNumber, distance/droneNumber, pollution/droneNumber, battery/droneNumber, new Timestamp(System.currentTimeMillis()).toString());
    }

    private void sendGlobalStatistics() {
        if(master.getGlobalStats().size() == 0) return;

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
            sendGlobalStatistics();
            try {
                Thread.sleep(10*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
