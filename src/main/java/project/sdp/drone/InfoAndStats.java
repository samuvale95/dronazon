package project.sdp.drone;

import java.awt.*;
import java.util.List;

public class InfoAndStats {
    private final int deliveryNumber;
    private final double deliveryTimeStamp;
    private final Point newPosition;
    private final int battery;
    private final double distanceRoutes;
    private final java.util.List<Double> airPollution;
    private final int callerDrone;

    public InfoAndStats(double deliveryTimeStamp, Point newPosition, int battery, double distanceRoutes, List<Double> airPollution, int callerDrone, int deliveryNumber) {
        this.deliveryTimeStamp = deliveryTimeStamp;
        this.newPosition = newPosition;
        this.battery = battery;
        this.distanceRoutes = distanceRoutes;
        this.airPollution = airPollution;
        this.callerDrone = callerDrone;
        this.deliveryNumber = deliveryNumber;
    }

    public double getDeliveryTimeStamp() {
        return deliveryTimeStamp;
    }

    public Point getNewPosition() {
        return newPosition;
    }

    public int getBattery() {
        return battery;
    }

    public double getDistanceRoutes() {
        return distanceRoutes;
    }

    public List<Double> getAirPollution() {
        return airPollution;
    }

    public Integer getDeliveryNumber() { return this.deliveryNumber; }

    public int getCallerDrone() {
        return callerDrone;
    }

    @Override
    public String toString() {
        return "InfoAndStats{" +
                "deliveryTimeStamp=" + deliveryTimeStamp +
                ", newPosition=" + newPosition +
                ", battery=" + battery +
                ", distanceRoutes=" + distanceRoutes +
                ", airPollution=" + airPollution +
                ", callerDrone=" + callerDrone +
                '}';
    }
}
