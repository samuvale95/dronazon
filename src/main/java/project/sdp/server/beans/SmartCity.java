package project.sdp.server.beans;

import java.awt.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class SmartCity {
    private static SmartCity instance;
    private final HashMap<Integer , Drone> city;
    private final ArrayList<Statistic> statistics;
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

    public synchronized void addStatistic(Statistic statistic){
        this.statistics.add(statistic);
    }

    public synchronized ListDrone getAllDrones(){
        ListDrone drones = new ListDrone();
        city.forEach((key, value) -> drones.add(value));
        return drones;
    }

    public ArrayList<Statistic> getGlobalStatistics(Timestamp fromTs, Timestamp toTs) {
        ArrayList<Statistic> gStatistics = new ArrayList<>();
        statistics.forEach((value) -> { if(value.getTimestamp().after(fromTs) && value.getTimestamp().before(toTs)) gStatistics.add(value);});
        return gStatistics;
    }

    public ArrayList<DeliveryStats> getDeliveryStatistics(Timestamp fromTs, Timestamp toTs) {
        ArrayList<DeliveryStats> gStatistics = new ArrayList<>();
        statistics.forEach((value) -> { if(value.getTimestamp().after(fromTs) && value.getTimestamp().before(toTs)) gStatistics.add(value.getAverageDelivery());});
        return gStatistics;
    }

    public ArrayList<DistanceStats> getDistanceStatistics(Timestamp fromTs, Timestamp toTs) {
        ArrayList<DistanceStats> gStatistics = new ArrayList<>();
        statistics.forEach((value) -> { if(value.getTimestamp().after(fromTs) && value.getTimestamp().before(toTs)) gStatistics.add(value.getAverageDistance());});
        return gStatistics;
    }
}
