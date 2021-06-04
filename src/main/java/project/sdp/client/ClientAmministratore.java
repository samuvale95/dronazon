package project.sdp.client;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import project.sdp.server.beans.ListDrone;
import project.sdp.server.beans.Statistics;

import java.io.IOException;
import java.util.ArrayList;
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

    public ClientResponse RESTcall(String URL_path){
        Client client = Client.create();
        WebResource webResource = client.resource(BASE_URI + URL_path);
        ClientResponse response = webResource.accept("application/json").get(ClientResponse.class);
        if(response.getStatus() != 200)
            throw new RuntimeException("Something wrong on your request");
        return response;
    }

    public void runClient() throws IOException {
        Scanner scanner = new Scanner(System.in);
        String option;
        ObjectMapper objMap = new ObjectMapper();

        System.out.println(mainMenu);
        while (!(option = scanner.nextLine()).equals("quit")){

            String from, to, n;
            TypeReference<ArrayList<Statistics>> typeRef;

            switch (option){
                case "1":
                    String dronesList = RESTcall("/dronazon/drone/getAll").getEntity(String.class);
                    ListDrone list = objMap.readValue(dronesList, ListDrone.class);
                    System.out.println("All drones inside smart city:");
                    list.getDrones().forEach(System.out::println);
                    break;
                case "2":
                    System.out.println("Insert number of statistics: ");
                    n = scanner.nextLine();

                    System.out.println("Print Statistics: ");
                    String statistics = RESTcall("/dronazon/city/statistics/"+n).getEntity(String.class);
                    typeRef = new org.codehaus.jackson.type.TypeReference<ArrayList<Statistics>>() {};
                    ArrayList<Statistics> statsList = objMap.readValue(statistics, typeRef);
                    statsList.forEach(System.out::println);
                    break;
                case "3":
                    System.out.println("Insert date from (dd-mm-yyyy): ");
                    from = scanner.nextLine();
                    System.out.println("Insert date to (dd-mm-yyyy): ");
                    to = scanner.nextLine();

                    System.out.println("Print delivery Statistics: ");
                    String statsDelivery = RESTcall("/dronazon/city/statistics/delivery/from/"+from+"/to/"+to).getEntity(String.class);
                    typeRef = new org.codehaus.jackson.type.TypeReference<ArrayList<Statistics>>() {};
                    ArrayList<Statistics> DeliveryStatsList = objMap.readValue(statsDelivery, typeRef);
                    DeliveryStatsList.forEach(System.out::println);
                    break;
                case "4":
                    System.out.println("Insert date from (dd-mm-yyyy): ");
                    from = scanner.nextLine();
                    System.out.println("Insert date to (dd-mm-yyyy): ");
                    to = scanner.nextLine();

                    System.out.println("Print distance Statistics: ");
                    String statsDistance = RESTcall("/dronazon/city/statistics/distance/from/"+from+"/to/"+to).getEntity(String.class);
                    typeRef = new org.codehaus.jackson.type.TypeReference<ArrayList<Statistics>>() {};
                    ArrayList<Statistics> DistanceStatsList = objMap.readValue(statsDistance, typeRef);
                    DistanceStatsList.forEach(System.out::println);
                    break;
            }

            System.out.println("\n");
            System.out.println(mainMenu);
        }
    }

    public static void main(String[] args) throws IOException {
        ClientAmministratore clientAmministratore = new ClientAmministratore();
        clientAmministratore.runClient();
    }
}
