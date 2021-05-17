package project.sdp.server.beans;

import org.codehaus.jackson.annotate.JsonIgnore;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Timestamp;

@XmlRootElement
public class Statistics {
    private double avgDelivery;
    private double avgDistance;
    private double averagePollution;
    private double averageBattery;
    private String timestamp;

    public Statistics(){}

    public Statistics(double averageDelivery, double averageDistance, double averagePollution, double averageBattery, String timestamp) {
        this.avgDelivery = averageDelivery;
        this.avgDistance = averageDistance;
        this.averagePollution = averagePollution;
        this.averageBattery = averageBattery;
        this.timestamp = timestamp;
    }

    public double getAvgDelivery() {
        return avgDelivery;
    }

    public double getAvgDistance() {
        return avgDistance;
    }

    public double getAveragePollution() {
        return averagePollution;
    }

    public double getAverageBattery() {
        return averageBattery;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Statistics{" +
                "avgDelivery=" + avgDelivery +
                ", avgDistance=" + avgDistance +
                ", averagePollution=" + averagePollution +
                ", averageBattery=" + averageBattery +
                ", timestamp=" + timestamp +
                '}';
    }
}
