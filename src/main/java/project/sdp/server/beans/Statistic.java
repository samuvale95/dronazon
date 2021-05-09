package project.sdp.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Timestamp;

@XmlRootElement
public class Statistic {
    private Delivery avgDelivery;
    private Distance avgDistance;
    private int averagePollution;
    private int averageBattery;
    private Timestamp timestamp;

    public Statistic(){}

    public Statistic(Delivery averageDelivery, Distance averageDistance, int averagePollution, int averageBattery, Timestamp timestamp) {
        this.avgDelivery = averageDelivery;
        this.avgDistance = averageDistance;
        this.averagePollution = averagePollution;
        this.averageBattery = averageBattery;
        this.timestamp = timestamp;
    }

    public Delivery getAverageDelivery() {
        return avgDelivery;
    }

    public void setAverageDelivery(Delivery averageDelivery) {
        this.avgDelivery = averageDelivery;
    }

    public Distance getAverageDistance() {
        return avgDistance;
    }

    public void setAverageDistance(Distance averageDistance) {
        this.avgDistance = averageDistance;
    }

    public int getAveragePollution() {
        return averagePollution;
    }

    public void setAveragePollution(int averagePollution) {
        this.averagePollution = averagePollution;
    }

    public int getAverageBattery() {
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
