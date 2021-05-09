package project.sdp.drone;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import javafx.util.Pair;
import project.sdp.server.beans.ListDrone;

import javax.swing.text.Position;
import java.awt.*;
import java.io.IOException;
import java.util.Scanner;

/*
* Ogni dorne e' un processo e non un thread
*
* I droni comunicano con gRPC
*
* Campi della classe:
* - ID
* - porta
* - indirizzo/URI server amministratore
* - battery init 100%
* I due seguenti vengono passati dal server quando il drone si registra al server
* - position
* - DroneList
*
* */
public class Drone {
  private final int id;
  private final int port;
  private final String URI_AdmServer;
  private int battery;
  private Point position;
  private ListDrone dronesList;
  private Boolean isMaster;

  public Drone(int id, int port, String URI_AdmServer){
      this.id = id;
      this.port = port;
      this.URI_AdmServer = URI_AdmServer;
      this.battery = 100;
      this.isMaster = false;
  }

  private void registerToServer(){
      Client client = Client.create();
      WebResource webResource = client.resource(URI_AdmServer + "/dronazon/drone/add");
      ClientResponse clientResponse = webResource.post(ClientResponse.class);

      if(clientResponse.getStatus() != 200)
          throw new RuntimeException("Something in your request is wrong " + clientResponse.getStatus());

      Gson gson = new Gson();
      Pair<ListDrone, Point> pair = gson.fromJson(clientResponse.getEntity(String.class), Pair.class);
      this.position = pair.getValue();
      this.dronesList = pair.getKey();
  }

  public void start(){
      registerToServer();
      System.out.println("Drone Info:");
      System.out.println(dronesList);
      System.out.println(position);

      try { System.in.read(); } catch (IOException e) { e.printStackTrace(); }
  }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Insert a integer id of drone: ");
        int id = scanner.nextInt();
        System.out.println("Insert a communication port for a drone: ");
        int port = scanner.nextInt();

        Drone drone = new Drone(id, port, "http://localhost:1337");
        drone.start();
    }
}
