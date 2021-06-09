package project.sdp.drone;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import project.sdp.server.beans.Statistics;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

public class StatisticsSender extends Thread{
    private final DroneProcess droneProcess;
    private final Master master;

    public StatisticsSender(DroneProcess droneProcess){
        this.droneProcess = droneProcess;
        this.master = droneProcess.getMasterProcess();
    }

    private Statistics calculateGlobalStatistics() {
        ArrayList<InfoAndStats> globalStats;
        synchronized (master.getGlobalStats()) {
            globalStats = new ArrayList<>(master.getGlobalStats());
            master.clearGlobalStats();
        }

        HashSet<Integer> set = new HashSet<>();
        globalStats.stream().map(InfoAndStats::getCallerDrone).forEach(set::add);
        double activeDeliveryDrone = set.size();

        Double distance = globalStats.stream().map(InfoAndStats::getDistanceRoutes).reduce(0.0, Double::sum);
        int deliveryNumber = globalStats.size();
        double pollution = globalStats.stream().map(stats -> {
            Optional<Double> sum = stats.getAirPollution().stream().reduce(Double::sum);
            return sum.map(aDouble -> aDouble / (double) stats.getAirPollution().size()).orElse(0.0);
        }).reduce(0.0, Double::sum);
        int battery = globalStats.stream().map(InfoAndStats::getBattery).reduce(0, Integer::sum);

        return new Statistics(deliveryNumber/activeDeliveryDrone, distance/activeDeliveryDrone, pollution/activeDeliveryDrone, battery/activeDeliveryDrone, new Timestamp(System.currentTimeMillis()).toString());
    }

    private void sendGlobalStatistics() {
        synchronized (master.getGlobalStats()) {
            if (master.getGlobalStats().size() == 0) return;
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
            sendGlobalStatistics();
            try {
                Thread.sleep(10*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
