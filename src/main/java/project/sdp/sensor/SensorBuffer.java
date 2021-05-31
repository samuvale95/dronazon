package project.sdp.sensor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SensorBuffer implements Buffer{

    private List<Measurement> buffer = new ArrayList<>();

    @Override
    public void addMeasurement(Measurement m) {
        buffer.add(m);
        if(buffer.size() == 8)
            this.notify();
    }

    @Override
    public List<Measurement> readAllAndClean() {
        while(buffer.size() < 8) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        List<Measurement> result = buffer;
        buffer = buffer.stream().skip(4).collect(Collectors.toList());
        return result;
    }
}
