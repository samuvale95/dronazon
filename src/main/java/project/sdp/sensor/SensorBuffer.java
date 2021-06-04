package project.sdp.sensor;

import project.sdp.drone.CallBack;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SensorBuffer implements Buffer{

    private List<Measurement> buffer = new ArrayList<>();
    private final CallBack callBack;

    public SensorBuffer(CallBack callBack){
        this.callBack = callBack;
    }

    @Override
    public void addMeasurement(Measurement m) {
        buffer.add(m);
        if(buffer.size() == 8)
            this.callBack.callBack(this);
    }

    @Override
    public List<Measurement> readAllAndClean() {
        List<Measurement> result = buffer;
        buffer = buffer.stream().skip(4).collect(Collectors.toList());
        return result;
    }
}
