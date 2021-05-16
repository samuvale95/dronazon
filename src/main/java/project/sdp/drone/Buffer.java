package project.sdp.drone;

import project.sdp.dronazon.Delivery;

import java.util.ArrayList;

public class Buffer<E> {
    private ArrayList<E> queue;

    public Buffer(){
        this.queue = new ArrayList<E>();
    }

    public synchronized void add(E obj) {
        queue.add(0, obj);
        this.notify();

    }

    public synchronized E next() throws InterruptedException {
        if(queue.size() == 0) this.wait();
        return queue.remove(queue.size()-1);
    }
}
