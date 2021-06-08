package project.sdp.server.beans;

import org.codehaus.jackson.annotate.JsonIgnore;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.awt.*;
import java.util.Objects;

@XmlRootElement
public class Drone implements Comparable<Drone> {
    private int id;
    private String ip;
    private int port;
    @JsonIgnore
    private Point position;
    private boolean hasValidPosition;
    private int battery;

    public Drone(int id, String ip, int port){
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.hasValidPosition = false;
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

    public boolean hasValidPosition() {
        return this.hasValidPosition;
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
                ", ip='" + ip +
                ", port=" + port +
                ", position=" + position +
                ", hasValidPosition=" + hasValidPosition +
                '}';
    }

    public Point getPosition() {
        return position;
    }

    public void invalidatePosition() {
        this.hasValidPosition = false;
    }

    public void setPosition(Point position) {
        this.position = position;
        this.hasValidPosition = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Drone drone = (Drone) o;
        return id == drone.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public int compareTo(Drone o) {
        return id-o.getId();
    }
}

