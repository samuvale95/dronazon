package project.sdp.server.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement
public class ListDrone {
    private final ArrayList<Drone> drones = new ArrayList<>();

    public ListDrone() {}


    public ArrayList<Drone> getDrones() {
        return drones;
    }

    public ArrayList<Drone> setDrones() {
        return drones;
    }

    public void add(Drone drone) {
        drones.add(drone);
    }

    @Override
    public String toString() {
        System.out.println(drones);
        return "ListDrone{" +
                "drones=" + drones +
                '}';
    }
}
