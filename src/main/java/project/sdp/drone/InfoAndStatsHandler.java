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
            Buffer<InfoAndStats> queue = droneProcess.getInfoAndStatsQueue();
            InfoAndStats infoAndStats = null;
            try {
                infoAndStats = queue.next();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            assert infoAndStats != null;
            int callerDroneId = infoAndStats.getCallerDrone();
            Point newPosition = infoAndStats.getNewPosition();
            int battery = infoAndStats.getBattery();

            ArrayList<Drone> listDrone = droneProcess.getDronesList();
            listDrone.forEach(drone -> {
                synchronized (listDrone) {
                    if (drone.getId() == callerDroneId) {
                        drone.setCommittedToDelivery(false);
                        drone.setPosition(newPosition);
                        drone.setBattery(battery);
                    }
                }
            });

            droneProcess.getGlobalStats().add(infoAndStats);
        }
    }
}
