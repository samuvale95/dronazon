package project.sdp.server.beans;

import org.codehaus.jackson.annotate.JsonIgnore;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.awt.*;

@XmlRootElement
public class Drone{
    private int id;
    private String ip;
    private int port;
    @JsonIgnore
    private Point position;
    private Boolean committedToDelivery;
    private int battery;

    public Drone(int id, String ip, int port){
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.committedToDelivery = false;
        this.battery = 100;
    }

    public Drone(){}

    public int getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Boolean getCommittedToDelivery() {
        return committedToDelivery;
    }

    public void setCommittedToDelivery(Boolean committedToDelivery) {
        this.committedToDelivery = committedToDelivery;
    }

    public void setBattery(int battery) {
        this.battery = battery;
    }

    @XmlElement
    public int getBattery() {
        return battery;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "Drone{" +
                "id=" + id +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", position=" + position +
                ", committedToDelivery=" + committedToDelivery +
                '}';
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point position) {
        this.position = position;
    }
}

