package project.sdp.dronazon;

import java.util.Random;
import java.util.UUID;

public class RandomDelivery implements Delivery{
    private final int id;
    private final int[] takePoint;
    private final int[] deliveryPoint;

    public RandomDelivery(int length, int height){
        this.id = setID();
        this.deliveryPoint = getRandomPoint(length, height);
        this.takePoint = getRandomPoint(length, height);
    }

    private static int[] getRandomPoint(int length, int height){
        Random random = new Random();
        return new int[]{random.nextInt(length), random.nextInt(height)};
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
    public int[] getTakePoint() {
        return this.takePoint;
    }

    @Override
    public int[] getDeliveryPoint() {
        return this.deliveryPoint;
    }
}
