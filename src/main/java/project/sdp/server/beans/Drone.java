package project.sdp.server.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Drone {
    private int id;
    private String ip;
    private int port;

    public Drone(int id, String ip, int port){
        this.id = id;
        this.ip = ip;
        this.port = port;
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

    public void setPort(int port) {
        this.port = port;
    }
}

