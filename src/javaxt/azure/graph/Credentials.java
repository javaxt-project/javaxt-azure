package javaxt.azure.graph;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import static java.nio.charset.StandardCharsets.UTF_8;

import javaxt.json.JSONObject;

//******************************************************************************
//**  Credentials
//******************************************************************************
/**
 *   Supplies the <code>Authorization</code> value for Graph requests. Two forms
 *   are supported:
 *   <ul>
 *     <li><b>App-only</b> (client credentials): constructed with tenant/client
 *         id and secret; acquires and caches an OAuth token, refreshing it when
 *         fewer than 60 seconds remain.</li>
 *     <li><b>Pre-authenticated</b>: constructed with an access token obtained
 *         elsewhere (e.g. a delegated/user flow, or a unit test). No network is
 *         performed.</li>
 *   </ul>
 *   Token acquisition is synchronized so a single instance can be shared by a
 *   multi-threaded sync service.
 *
 ******************************************************************************/

public class Credentials {

    private final String tenantID;
    private final String clientID;
    private final String clientSecret;
    private final String loginURL;

    private String tokenType = "Bearer";
    private String accessToken;
    private javaxt.utils.Date expirationDate;

    private HttpClient client;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** App-only credentials using the OAuth 2.0 client-credentials grant.
   */
    public Credentials(String tenantID, String clientID, String clientSecret){
        this.tenantID = tenantID;
        this.clientID = clientID;
        this.clientSecret = clientSecret;
        this.loginURL = "https://login.microsoftonline.com/" + tenantID + "/oauth2/v2.0/token";
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Pre-authenticated credentials wrapping an access token acquired elsewhere.
   *  Never fetches or refreshes.
   */
    public Credentials(String accessToken){
        this.tenantID = null;
        this.clientID = null;
        this.clientSecret = null;
        this.loginURL = null;
        this.accessToken = accessToken;
    }


  //**************************************************************************
  //** getAuthorization
  //**************************************************************************
  /** Returns the value for the HTTP <code>Authorization</code> header, e.g.
   *  "Bearer eyJ...". Refreshes the token first if it is app-only and expired.
   */
    public synchronized String getAuthorization() throws GraphException {
        if (clientSecret!=null){
            if (accessToken==null || expirationDate==null ||
                expirationDate.compareTo(new javaxt.utils.Date(), "seconds") < 60){
                connect();
            }
        }
        return tokenType + " " + accessToken;
    }


  //**************************************************************************
  //** invalidate
  //**************************************************************************
  /** Discards a cached app-only token so the next request re-acquires one.
   *  No-op for pre-authenticated credentials.
   */
    public synchronized void invalidate(){
        if (clientSecret!=null){
            accessToken = null;
            expirationDate = null;
        }
    }


  //**************************************************************************
  //** getExpirationDate
  //**************************************************************************
  /** Returns the expiration date of the cached app-only token, or null if no
   *  token has been acquired (or these are pre-authenticated credentials).
   */
    public synchronized javaxt.utils.Date getExpirationDate(){
        return expirationDate;
    }


  //**************************************************************************
  //** connect
  //**************************************************************************
  /** Acquires a new app-only access token via the client-credentials grant and
   *  caches it along with its expiration date.
   */
    private void connect() throws GraphException {
        if (client==null) client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

        String payload =
            "client_id=" + enc(clientID) +
            "&scope=" + enc("https://graph.microsoft.com/.default") +
            "&client_secret=" + enc(clientSecret) +
            "&grant_type=client_credentials";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(loginURL))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload, UTF_8))
            .build();

        HttpResponse<String> response;
        try{
            response = client.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
        }
        catch(java.io.IOException e){
            throw new GraphException("Unable to acquire access token: " + e.getMessage(), e);
        }
        catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new GraphException("Interrupted while acquiring access token", e);
        }

        JSONObject json;
        try{ json = new JSONObject(response.body()); }
        catch(Exception e){ json = new JSONObject(); }

        if (response.statusCode()>=200 && response.statusCode()<300){
            if (!json.get("token_type").isNull()) tokenType = json.get("token_type").toString();
            accessToken = json.get("access_token").toString();
            Integer expiresIn = json.get("expires_in").toInteger();
            if (expiresIn==null) expiresIn = 3600;
            expirationDate = new javaxt.utils.Date().add(expiresIn, "seconds");
        }
        else{
            String code = json.get("error").isNull() ? null : json.get("error").toString();
            String desc = json.get("error_description").isNull() ? response.body() : json.get("error_description").toString();
            throw new GraphException("Token request failed (HTTP " + response.statusCode() + ")" +
                (code!=null ? " " + code : "") + ": " + desc);
        }
    }


  //**************************************************************************
  //** enc
  //**************************************************************************
  /** URL-encodes a form value (treating null as an empty string).
   */
    private static String enc(String s){
        return URLEncoder.encode(s==null ? "" : s, UTF_8);
    }
}
