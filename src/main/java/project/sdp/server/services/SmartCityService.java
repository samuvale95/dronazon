package project.sdp.server.services;

/*
* ########### Interfaccia per i droni ############
* Struttura interna della smart city Matrice 10x10
*
* Inserimento di un nuovo drone --> POST drone/add {id, ip, port}
* Inserimento possibile solo se non ci sono altri droni con lo stesso id
*
*Rimozione di un drone --> DELETE drone/remove {id drone}
* Rimuovere il drone dalla struttura interna
*
* Inserimento delle statistiche --> POST city/statistics
* Inserire le statistiche in un'opportuna struttura dati
*
* ############ Interfaccia Client ###############
* Elenco dei droni --> GET drone/getAll
*
* Ultime n statistiche globali --> GET city/statistics/from/{date}/to/{date}
*
* Media delle consegne tra --> GET drone/all/statistics/delivery/from/{date}/to/{date}
*
* Media dei chilometri percorsi dai droni --> get drone/all/statistics/distance/from/{date}/to/{date}
* */

import com.google.gson.Gson;
import project.sdp.server.beans.Drone;
import project.sdp.server.beans.SmartCity;
import project.sdp.server.beans.Statistics;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Date;
import java.sql.Timestamp;

@Path("dronazon")
public class SmartCityService {
    private final SmartCity smartCity = SmartCity.getInstance();

    @POST
    @Path("drone/add")
    @Consumes({"application/json"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response addDrone(Drone drone){
        Gson gson = new Gson();
        return Response.ok(gson.toJson(smartCity.addDrone(drone))).build();
    }

    @DELETE
    @Path("drone/remove/{id}")
    public Response removeDrone(@PathParam("id") int id){
        smartCity.removeDrone(id);
        return Response.ok().build();
    }

    @POST
    @Path("city/statistics")
    @Consumes({"application/json"})
    public Response addStatistics(Statistics statistics){
        smartCity.addStatistic(statistics);
        return Response.ok().build();
    }

    @GET
    @Path("drone/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllDrones(){
        return Response.ok(smartCity.getAllDrones()).build();
    }

    @GET
    @Path("city/statistics/{n}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGlobalStatistics(@PathParam("n") int n){
        return Response.ok(smartCity.getGlobalStatistics(n)).build();
    }

    @GET
    @Path("drone/all/statistics/delivery/from/{from}/to/{to}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeliveryStatistics(@PathParam("from") String from,@PathParam("to") String to){
        return Response.ok(smartCity.getDeliveryStatistics(from, to)).build();
    }

    @GET
    @Path("drone/all/statistics/distance/from/{from}/to/{to}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDistanceStatistics(@PathParam("from") String from,@PathParam("to") String to){
        return Response.ok(smartCity.getDistanceStatistics(from, to)).build();
    }
}