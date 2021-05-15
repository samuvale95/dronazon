package project.sdp.drone;

import project.sdp.dronazon.Delivery;

import java.util.ArrayList;

public class DeliveryQueue {
    private ArrayList<Delivery> queue;

    public DeliveryQueue(){
        this.queue = new ArrayList<>();
    }

    public synchronized void add(Delivery delivery) {
        queue.add(0, delivery);
        this.notify();

    }

    public synchronized Delivery next() throws InterruptedException {
        if(queue.size() == 0) this.wait();
        return queue.remove(queue.size()-1);
    }
}