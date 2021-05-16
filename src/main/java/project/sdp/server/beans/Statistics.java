package project.sdp.server.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Timestamp;

@XmlRootElement
public class Statistics {
    private double avgDelivery;
    private double avgDistance;
    private double averagePollution;
    private double averageBattery;
    private Timestamp timestamp;

    public Statistics(){}

    public Statistics(double averageDelivery, double averageDistance, double averagePollution, double averageBattery, Timestamp timestamp) {
        this.avgDelivery = averageDelivery;
        this.avgDistance = averageDistance;
        this.averagePollution = averagePollution;
        this.averageBattery = averageBattery;
        this.timestamp = timestamp;
    }

    public double getAverageDelivery() {
        return avgDelivery;
    }

    public void setAverageDelivery(int averageDelivery) {
        this.avgDelivery = averageDelivery;
    }

    public double getAverageDistance() {
        return avgDistance;
    }

    public void setAverageDistance(double averageDistance) {
        this.avgDistance = averageDistance;
    }

    public double getAveragePollution() {
        return averagePollution;
    }

    public void setAveragePollution(int averagePollution) {
        this.averagePollution = averagePollution;
    }

    public double getAverageBattery() {
        return averageBattery;
    }

    public void setAverageBattery(int averageBattery) {
        this.averageBattery = averageBattery;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
