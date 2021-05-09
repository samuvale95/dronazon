package project.sdp.server.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.awt.*;

@XmlRootElement
public class Pair {
    private ListDrone listDrone;
    private Point position;

    public Pair(){}

    public Pair(ListDrone listDrone, Point position){
        this.listDrone = listDrone;
        this.position = position;
    }

    public ListDrone getListDrone() {
        return listDrone;
    }

    public void setListDrone(ListDrone listDrone) {
        this.listDrone = listDrone;
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point position) {
        this.position = position;
    }
}
