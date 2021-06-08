package project.sdp.drone;

import project.sdp.server.beans.Drone;

import java.awt.*;
import java.util.ArrayList;

public class InfoAndStatsHandler extends Thread{

    private final DroneProcess droneProcess;

    public InfoAndStatsHandler(DroneProcess droneProcess){
        this.droneProcess = droneProcess;
    }

    @Override
    public void run() {
        while(true) {
            Buffer<InfoAndStats> queue;
            synchronized (droneProcess.getMasterProcess().getInfoAndStatsQueue()) {
                queue = droneProcess.getMasterProcess().getInfoAndStatsQueue();
            }

            InfoAndStats infoAndStats = null;
            System.out.println(queue);
            try {
                infoAndStats = queue.next();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            synchronized (droneProcess.getMasterProcess().getGlobalStats()) {
                droneProcess.getMasterProcess().getGlobalStats().add(infoAndStats);
            }

        }
    }
}
