package project.sdp.client;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import project.sdp.server.beans.ListDrone;

import java.util.Scanner;

public class ClientAmministratore {
    private static final String BASE_URI = "http://localhost:1337";

    private final String mainMenu = "******** Client Amministratore ********\n" +
                                "\n" +
                                "Press the corresponding number or write <quit> to esc\n" +
                                "\n" +
                                "1. Print all Drones inside the Smart City\n" +
                                "2. Global Statistics\n" +
                                "3. Average number of Delivery\n" +
                                "4. Average Distance traveled\n";

    public void runClient(){
        Scanner scanner = new Scanner(System.in);
        String option;

        System.out.println(mainMenu);
        while (!(option = scanner.nextLine()).equals("quit")){
            Client client = Client.create();
            WebResource webResource = null;
            ClientResponse response;
            String from, to, n;

            switch (option){
                case "1":
                    webResource = client.resource(BASE_URI + "/dronazon/drone/getAll");
                    break;
                case "2":
                    System.out.println("Insert number of statistics: ");
                    n = scanner.nextLine();

                     webResource = client.resource(BASE_URI + "/dronazon/city/statistics/"+n);
                     break;
                case "3":
                    System.out.println("Insert date from (dd-mm-yyyy): ");
                    from = scanner.nextLine();
                    System.out.println("Insert date to (dd-mm-yyyy): ");
                    to = scanner.nextLine();

                    webResource = client.resource(BASE_URI + "/dronazon/city/statistics/delivery/from/"+from+"/to/"+to);
                    break;
                case "4":
                    System.out.println("Insert date from (dd-mm-yyyy): ");
                    from = scanner.nextLine();
                    System.out.println("Insert date to (dd-mm-yyyy): ");
                    to = scanner.nextLine();

                    webResource = client.resource(BASE_URI + "/dronazon/city/statistics/distance/from/"+from+"/to/"+to);
                    break;
            }

            assert webResource != null;
            response = webResource.accept("application/json").get(ClientResponse.class);
            if(response.getStatus() != 200)
                throw new RuntimeException("Something wrong on your request");

            String drones = response.getEntity(String.class);
            System.out.println(drones);
            System.out.println("\n");
            System.out.println(mainMenu);
        }
    }

    public static void main(String[] args) {
        ClientAmministratore clientAmministratore = new ClientAmministratore();
        clientAmministratore.runClient();
    }
}
