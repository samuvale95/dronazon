package project.sdp.dronazon;

import java.awt.*;
import java.util.Random;
import java.util.UUID;

public class RandomDelivery implements Delivery{
    private final int id;
    private final Point takePoint;
    private final Point deliveryPoint;

    public RandomDelivery(int length, int height){
        this.id = setID();
        this.deliveryPoint = getRandomPoint(length, height);
        this.takePoint = getRandomPoint(length, height);
    }

    public RandomDelivery(int id, Point takePoint, Point deliveryPoint){
        this.id = id;
        this.deliveryPoint = takePoint;
        this.takePoint = deliveryPoint;
    }

    private static Point getRandomPoint(int length, int height){
        Random random = new Random();
        return new Point(random.nextInt(length), random.nextInt(height));
    }

    private static int setID(){
        UUID idOne = java.util.UUID.randomUUID();
        String str=""+idOne;
        int uid=str.hashCode();
        String filterStr=""+uid;
        str=filterStr.replaceAll("-", "");
        return Integer.parseInt(str);
    }

    @Override
    public int getID() {
        return this.id;
    }

    @Override
    public Point getTakePoint() {
        return this.takePoint;
    }

    @Override
    public Point getDeliveryPoint() {
        return this.deliveryPoint;
    }

    @Override
    public String toString() {
        return "RandomDelivery{" +
                "id=" + id +
                ", takePoint=" + takePoint +
                ", deliveryPoint=" + deliveryPoint +
                '}';
    }
}
