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
            assert infoAndStats != null;
            int callerDroneId = infoAndStats.getCallerDrone();
            Point newPosition = infoAndStats.getNewPosition();
            int battery = infoAndStats.getBattery();

            ArrayList<Drone> listDrone;
            synchronized (droneProcess.getDronesList()) {
                listDrone = new ArrayList<>(droneProcess.getDronesList());


                System.out.println(infoAndStats);
                for (Drone drone: listDrone) {
                    if (drone.getId() == callerDroneId) {
                        drone.setCommittedToDelivery(false);
                        drone.setPosition(newPosition);
                        drone.setBattery(battery);
                        synchronized (droneProcess.getMasterProcess()) {
                            if (droneProcess.getMasterProcess().getState() == State.TERMINATED) {
                                System.err.println("NOTIFY");
                                droneProcess.getMasterProcess().notify();
                            }
                        }
                    }
                }
            }

            synchronized (droneProcess.getMasterProcess().getGlobalStats()) {
                droneProcess.getMasterProcess().getGlobalStats().add(infoAndStats);
            }

        }
    }
}
