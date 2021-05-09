package project.sdp.server.beans;

import javafx.util.Pair;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class SmartCity {
    private final HashMap<Integer , Pair<Drone, int[]>> city;
    private final ArrayList<Statistic> statistics;
    private final int length;
    private final int height;

    public SmartCity(int length, int height){
        this.length = length;
        this.height = height;
        this.city = new HashMap<>();
        this.statistics = new ArrayList<>();
    }

    public synchronized void addDrone(Drone drone){
        Random random = new Random();
        int[] position = new int[]{random.nextInt(this.length), random.nextInt(this.height)};

        System.out.println(Arrays.toString(position));

        if(city.get(drone.getId()) != null)
            throw new IllegalArgumentException("A drone with same id is already present in the city, try to change id");
        city.put(drone.getId(), new Pair<>(drone, position));
        System.out.println(city);
    }

    public synchronized void removeDrone(int id) {
        if(city.get(id) == null)
            throw new IllegalArgumentException("Drone with this ID is not present in the city");

        city.remove(id);
    }

    public synchronized void addStatistic(Statistic statistic){
        this.statistics.add(statistic);
    }

    public synchronized ArrayList<Drone> getAllDrones(){
        ArrayList<Drone> drones = new ArrayList<>();
        city.forEach((key, value) -> drones.add(value.getKey()));
        System.out.println(city);
        System.out.print(drones);
        return drones;
    }

    public ArrayList<Statistic> getGlobalStatistics(Timestamp fromTs, Timestamp toTs) {
        ArrayList<Statistic> gStatistics = new ArrayList<>();
        statistics.forEach((value) -> { if(value.getTimestamp().after(fromTs) && value.getTimestamp().before(toTs)) gStatistics.add(value);});
        return gStatistics;
    }

    public ArrayList<Delivery> getDeliveryStatistics(Timestamp fromTs, Timestamp toTs) {
        ArrayList<Delivery> gStatistics = new ArrayList<>();
        statistics.forEach((value) -> { if(value.getTimestamp().after(fromTs) && value.getTimestamp().before(toTs)) gStatistics.add(value.getAverageDelivery());});
        return gStatistics;
    }

    public ArrayList<Distance> getDistanceStatistics(Timestamp fromTs, Timestamp toTs) {
        ArrayList<Distance> gStatistics = new ArrayList<>();
        statistics.forEach((value) -> { if(value.getTimestamp().after(fromTs) && value.getTimestamp().before(toTs)) gStatistics.add(value.getAverageDistance());});
        return gStatistics;
    }
}
