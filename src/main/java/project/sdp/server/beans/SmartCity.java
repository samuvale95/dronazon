package project.sdp.server.beans;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class SmartCity {
    private static SmartCity instance;
    private final HashMap<Integer , Drone> city;
    private final ArrayList<Statistics> statistics;
    private static int length = 10;
    private static int height = 10;

    private SmartCity(){
        this.city = new HashMap<>();
        this.statistics = new ArrayList<>();
    }

    public static void setLength(int l){
        length = l;
    }

    public static void setHeight(int h){
        height = h;
    }

    public synchronized static SmartCity getInstance(){
        if(instance == null) instance = new SmartCity();

        return instance;
    }

    public synchronized project.sdp.server.beans.Pair addDrone(Drone drone){
        Random random = new Random();
        Point position = new Point(random.nextInt(length), random.nextInt(height));

        if(city.get(drone.getId()) != null)
            throw new IllegalArgumentException("A drone with same id is already present in the city, try to change id");

        drone.setPosition(position);
        city.put(drone.getId(), drone);

        return new project.sdp.server.beans.Pair(getAllDrones(), position);
    }

    public synchronized void removeDrone(int id) {
        if(city.get(id) == null)
            throw new IllegalArgumentException("Drone with this ID is not present in the city");

        city.remove(id);
    }

    public synchronized void addStatistic(Statistics statistic){
        this.statistics.add(statistic);
    }

    public synchronized ListDrone getAllDrones(){
        ListDrone drones = new ListDrone();
        drones.setDrones(new ArrayList<>());
        city.forEach((key, value) -> drones.add(value));
        return drones;
    }

    public ArrayList<Statistics> getGlobalStatistics(int n) {
        return new ArrayList<>(statistics.subList(0,n));
    }

    public ArrayList<Statistics> getDeliveryStatistics(String from, String to) {
        ArrayList<Statistics> gStatistics = new ArrayList<>();
        statistics.forEach((value) -> { if(value.getTimestamp().compareTo(from) >= 0 && value.getTimestamp().compareTo(to) <= 0) gStatistics.add(value);});
        return gStatistics;
    }

    public ArrayList<Statistics> getDistanceStatistics(String from, String to) {
        ArrayList<Statistics> gStatistics = new ArrayList<>();
        statistics.forEach((value) -> { if(value.getTimestamp().compareTo(from)>=0 && value.getTimestamp().compareTo(to)<=0) gStatistics.add(value);});
        return gStatistics;
    }

    public boolean contain(Drone drone) {
        return city.get(drone.getId()) != null;
    }
}
