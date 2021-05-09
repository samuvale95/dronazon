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

import project.sdp.server.beans.Drone;
import project.sdp.server.beans.SmartCity;
import project.sdp.server.beans.Statistic;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Date;
import java.sql.Timestamp;

@Path("dronazon")
public class SmartCityServices {
    private final SmartCity smartCity = new SmartCity(10,10);

    @POST
    @Path("drone/add")
    @Consumes({"application/json"})
    public Response addDrone(Drone drone){
        smartCity.addDrone(drone);
        return Response.ok().build();
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
    public Response addStatistics(Statistic statistics){
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
    @Path("city/statistics/from/{from}/to/{to}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGlobalStatistics(@PathParam("from") String from,@PathParam("to") String to){
        Timestamp fromTs = new Timestamp(Date.valueOf(from).getTime());
        Timestamp toTs = new Timestamp(Date.valueOf(from).getTime());

        return Response.ok(smartCity.getGlobalStatistics(fromTs, toTs)).build();
    }

    @GET
    @Path("drone/all/statistics/delivery/from/{from}/to/{to}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeliveryStatistics(@PathParam("from") String from,@PathParam("to") String to){
        Timestamp fromTs = new Timestamp(Date.valueOf(from).getTime());
        Timestamp toTs = new Timestamp(Date.valueOf(from).getTime());

        return Response.ok(smartCity.getDeliveryStatistics(fromTs, toTs)).build();
    }

    @GET
    @Path("drone/all/statistics/distance/from/{from}/to/{to}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDistanceStatistics(@PathParam("from") String from,@PathParam("to") String to){
        Timestamp fromTs = new Timestamp(Date.valueOf(from).getTime());
        Timestamp toTs = new Timestamp(Date.valueOf(from).getTime());

        return Response.ok(smartCity.getDistanceStatistics(fromTs, toTs)).build();
    }
}