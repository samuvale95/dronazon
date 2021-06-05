package project.sdp.server.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement
public class ListDrone {
    private ArrayList<Drone> drones;

    public ListDrone() {}


    public ArrayList<Drone> getDrones() {
        return drones;
    }

    public void setDrones(ArrayList<Drone> droneList) {
        this.drones = droneList;
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
