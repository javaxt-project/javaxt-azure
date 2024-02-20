package javaxt.azure.graph;

import java.util.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static java.nio.charset.StandardCharsets.*;

import javaxt.json.*;
import static javaxt.utils.Console.console;

public class Connection {

  //Azure end-points
    private String loginURL = "https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token";
    private String graphURL = "https://graph.microsoft.com/v1.0";


  //Static properties
    private String tenantID;
    private String clientID;
    private String clientSecret;


  //Transient properties
    private String tokenType;
    private String accessToken;
    private javaxt.utils.Date expirationDate;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Connection(String tenantID, String clientID, String clientSecret) throws Exception {
        this.tenantID = tenantID;
        this.clientID = clientID;
        this.clientSecret = clientSecret;
        connect();
    }


  //**************************************************************************
  //** connect
  //**************************************************************************
    private void connect() throws Exception {
        console.log("connecting...");

        String loginURL = this.loginURL.replace("{tenant}", tenantID);
        javaxt.http.Request request = new javaxt.http.Request(loginURL);
        request.setNumRedirects(0);

        String payload = "client_id=" +clientID +
        "&scope=https%3A%2F%2Fgraph.microsoft.com%2F.default" +
        "&client_secret=" + clientSecret +
        "&grant_type=client_credentials";

        //request.setHeader(); application/x-www-form-urlencoded
        request.write(payload);

        javaxt.http.Response response = request.getResponse();
        JSONObject json = response.getJSONObject();


        if (response.getStatus()==200){
            tokenType = json.get("token_type").toString();
            accessToken = json.get("access_token").toString();
            Integer expiresIn = json.get("expires_in").toInteger();
            expirationDate = new javaxt.utils.Date().add(expiresIn, "seconds");

            console.log("Connected! token expires " + expirationDate);
        }
        else{
            System.out.println(response.toString());
            System.out.println(json.toString(4));
            throw new Exception();
        }
    }


  //**************************************************************************
  //** getExpiration
  //**************************************************************************
    public javaxt.utils.Date getExpirationDate(){
        return expirationDate;
    }


  //**************************************************************************
  //** getResponse
  //**************************************************************************
    public JSONObject getResponse(String url) throws Exception {
        return getResponse(url, null);
    }


  //**************************************************************************
  //** getResponse
  //**************************************************************************
    public JSONObject getResponse(String url, JSONObject payload) throws Exception {
        return getResponse(url, payload, null);
    }


  //**************************************************************************
  //** getResponse
  //**************************************************************************
    public JSONObject getResponse(String url, JSONObject payload, String method) throws Exception {


      //Get request builder
        HttpRequest.Builder request = getRequest(url);


      //Set payload and method as needed
        if (payload!=null){
            if (method==null) method = "POST";
            else method = method.toUpperCase();

            request.header("Content-Type", "application/json;charset=UTF-8");
            request.method(method, HttpRequest.BodyPublishers.ofByteArray(
                payload.toString().getBytes(UTF_8))
            );
        }


      //Get response
        HttpResponse<String> response = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()
        .send(request.build(), HttpResponse.BodyHandlers.ofString(UTF_8));



      //Parse and return response
        JSONObject json = new JSONObject(response.body());
        int status = response.statusCode();
        if (status==200){
            return json;
        }
        else if (status==429){
            Thread.sleep(1500);
            return getResponse(url);
        }
        else{

            //console.log(response.headers().map());
            //System.out.println(response.toString());
            System.out.println(json.toString(4));
            throw new Exception();
        }
    }


  //**************************************************************************
  //** getRequest
  //**************************************************************************
    private synchronized HttpRequest.Builder getRequest(String url) throws Exception {

      //Update url as needed
        if (!url.startsWith(graphURL) && !url.startsWith("http")){
            if (!url.startsWith("/")) url = "/" + url;
            url = graphURL + url;
        }


      //Refresh tokens as needed
        var timeRemaining = expirationDate.compareTo(new javaxt.utils.Date(), "seconds");
        //console.log("timeRemaining: " + timeRemaining + " seconds");
        if (timeRemaining<60) connect();


      //Execute http request and return response
        return HttpRequest.newBuilder()
        .uri(java.net.URI.create(url))
        .header("Authorization", tokenType + " " + accessToken);
    }

}