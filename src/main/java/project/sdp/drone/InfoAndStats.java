package project.sdp.drone;

import java.awt.*;

public class InfoAndStats {
    private double deliveryTimeStamp;
    private Point newPosition;
    private int battery;
    private double distanceRoutes;
    private double airPollution;
    private int callerDrone;

    public InfoAndStats(double deliveryTimeStamp, Point newPosition, int battery, double distanceRoutes, double airPollution, int callerDrone) {
        this.deliveryTimeStamp = deliveryTimeStamp;
        this.newPosition = newPosition;
        this.battery = battery;
        this.distanceRoutes = distanceRoutes;
        this.airPollution = airPollution;
        this.callerDrone = callerDrone;
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

    public double getAirPollution() {
        return airPollution;
    }

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
