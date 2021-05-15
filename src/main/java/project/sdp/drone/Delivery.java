package project.sdp.drone;

import java.awt.*;

public class Delivery {
    private int id;
    private Point takePoint;
    private Point deliveryPoint;

    public Delivery(int id, Point takePoint, Point deliveryPoint){
        this.id = id;
        this.takePoint = takePoint;
        this.deliveryPoint = deliveryPoint;
    }

    public int getId() {
        return id;
    }

    public Point getDeliveryPoint() {
        return deliveryPoint;
    }

    public Point getTakePoint() {
        return takePoint;
    }
}
