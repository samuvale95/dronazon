package project.sdp.server.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Statistics {

    private double averageDelivery;
    private  double averageDistance;
    private  double averagePollution;
    private  double averageBattery;
    private  String timestamp;

    public Statistics(){}

    public Statistics(double averageDelivery, double averageDistance, double averagePollution, double averageBattery, String timestamp){
        this.averageDelivery = averageDelivery;
        this.averageDistance = averageDistance;
        this.averagePollution = averagePollution;
        this.averageBattery = averageBattery;
        this.timestamp = timestamp;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setAverageDelivery(double averageDelivery) {
        this.averageDelivery = averageDelivery;
    }

    public void setAverageDistance(double averageDistance) {
        this.averageDistance = averageDistance;
    }

    public void setAveragePollution(double averagePollution) {
        this.averagePollution = averagePollution;
    }

    public void setAverageBattery(double averageBattery) {
        this.averageBattery = averageBattery;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public double getAverageDelivery() {
        return averageDelivery;
    }

    public double getAverageDistance() {
        return averageDistance;
    }

    public double getAveragePollution() {
        return averagePollution;
    }

    public double getAverageBattery() {
        return averageBattery;
    }

    @Override
    public String toString() {
        return "Statistics{" +
                "avgDelivery=" + averageDelivery +
                ", avgDistance=" + averageDistance +
                ", averagePollution=" + averagePollution +
                ", averageBattery=" + averageBattery +
                ", timestamp=" + timestamp +
                '}';
    }
}
