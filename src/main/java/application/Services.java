package application;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by cestevez on 8/12/16.
 */
@Path("cen")
public class Services {

    private final String FLICKR_API_KEY = "b725e51ea8e39a3ea2fb991c35614e65";
    private final String GMAPS_API_KEY = "AIzaSyAFdMfTTGNAXKnXtoVRqMRuyvK1WslN9ao";



    @GET
    @Path("imageGeoData")
    @Produces(MediaType.TEXT_PLAIN)
    public Response imageGeoData(@QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit){

        Response response;


        try {

            JsonObject jsonResponse = loadFlickrImageGeoData("30676368174");

            String latitude = jsonResponse.getAsJsonObject("photo").getAsJsonObject("location").get("latitude").getAsString();
            String longitude = jsonResponse.getAsJsonObject("photo").getAsJsonObject("location").get("longitude").getAsString();

            List<String> locations = googleMapsLocationsByLatLong(latitude, longitude);

            response = createResponseJson(locations, offset, limit);


        } catch (Exception e) {
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
            e.printStackTrace();
        }



        return response;
    }





    private List<String> googleMapsLocationsByLatLong(String latitude, String longitude) throws URISyntaxException, IOException {
        JsonObject jsonResponse;

        URI uri = new URIBuilder("https://maps.googleapis.com/maps/api/geocode/json")
                .setParameter("latlng", latitude +","+longitude)
                .setParameter("key", GMAPS_API_KEY)
                .build();

        HttpGet mapsRequest = new HttpGet(uri);

        HttpClient client = HttpClientBuilder.create().build();

        System.out.println("Calling Google Maps Services at " +uri.toString());
        HttpResponse mapsResponse = client.execute(mapsRequest);
        System.out.println("Google Maps Services returned with status " +mapsResponse.getStatusLine().getStatusCode());


        JsonParser parser = new JsonParser();
        jsonResponse = parser.parse(EntityUtils.toString(mapsResponse.getEntity())).getAsJsonObject();

        Gson gson = new Gson();

        JsonObject[] resultsGMaps =  gson.fromJson(jsonResponse.get("results"), JsonObject[].class);

        List<String> locations = new ArrayList<>();

        (Arrays.asList(resultsGMaps)).forEach((k)->
                locations.add(k.get("formatted_address").getAsString()));

        return locations;
    }



    private JsonObject loadFlickrImageGeoData(String imageId) throws URISyntaxException, IOException {
        HttpClient client = HttpClientBuilder.create().build();


        URI uri = new URIBuilder("https://api.flickr.com/services/rest/")
                .setParameter("method","flickr.photos.geo.getLocation")
                .setParameter("api_key", FLICKR_API_KEY)
                .setParameter("photo_id", imageId)
                .setParameter("format", "json")
                .setParameter("nojsoncallback", "1")
                .build();

        HttpGet getRequest = new HttpGet(uri);

        getRequest.addHeader("accept", "application/json");

        System.out.println("Calling Flickr Image Services at " +uri.toString());
        HttpResponse response = client.execute(getRequest);
        System.out.println("Flickr Image Services returned with status " +response.getStatusLine().getStatusCode());

        JsonParser parser = new JsonParser();

        return parser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();
    }





    private Response createResponseJson(List<String> locations, int offset, int limit){


        JsonArray locationsElement = new JsonArray();

        int newOffset = offset;
        while (newOffset < offset + limit){
            locationsElement.add(locations.get(offset));
            newOffset++;
        }



        String linkBaseURI = "http://localhost:2222/cen/imageGeoData?offset={offset}&limit={limit}";

        Link firstLink = Link.fromUri(linkBaseURI)
                .rel("first").type("text/plain")
                .build(0, limit);

        Link lastLink = Link.fromUri(linkBaseURI)
                .rel("last").type("text/plain")
                .build(locations.size()-limit, limit);

        Link previousLink = Link.fromUri(linkBaseURI)
                .rel("prev").type("text/plain")
                .build(offset - limit, limit);

        if (newOffset+limit > locations.size()){
            limit = newOffset+limit - locations.size();
        }

        Link nextLink = Link.fromUri(linkBaseURI)
                .rel("next").type("text/plain")
                .build(newOffset, limit);



        return Response.ok(locations, MediaType.APPLICATION_JSON_TYPE)
                .header("X-Total-Count", locations.size())
                .entity(locationsElement.toString())
                .links(firstLink, lastLink, nextLink, previousLink)
                .build();
    }
}
