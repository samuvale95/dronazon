package project.sdp.drone;

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
        while(queue.size() == 0){this.wait();};
        return queue.remove(queue.size()-1);
    }

    @Override
    public String toString() {
        return "Buffer{" +
                "queue=" + queue +
                '}';
    }
}
