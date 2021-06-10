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

    public synchronized ArrayList<E> getAll() throws InterruptedException {
        while(queue.size() == 0){this.wait();};
        ArrayList<E> result = new ArrayList<>(queue);
        queue.clear();
        return result;
    }

    public synchronized int size(){
        return queue.size();
    }

    @Override
    public String toString() {
        return "Buffer{" +
                "queue=" + queue +
                '}';
    }
}
