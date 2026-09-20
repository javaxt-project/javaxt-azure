package javaxt.azure.graph;
import java.util.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import static java.nio.charset.StandardCharsets.UTF_8;

import javaxt.json.*;

//******************************************************************************
//**  Connection
//******************************************************************************
/**
 *   Represents an authenticated connection to Microsoft Graph. Handles bearer
 *   token acquisition (via {@link Credentials}), request execution, transient
 *   error retry with <code>Retry-After</code> handling, and paging/delta
 *   traversal of Graph collections.
 *
 *   <p>A single Connection is thread safe and is intended to be shared by a
 *   multi-threaded sync service; model objects it returns are not thread safe.</p>
 *
 *   <p>Example (app-only, client credentials):</p>
 *   <pre>
 *   Connection conn = new Connection(tenantID, clientID, clientSecret);
 *   User user = User.findByEmail("therapist@contoso.com", conn);
 *   </pre>
 *
 ******************************************************************************/

public class Connection {

    private final String graphURL = "https://graph.microsoft.com/v1.0";

    private final Credentials credentials;

  //Prefer header values appended to every request (comma joined into one header)
    private final List<String> preferValues = new ArrayList<>();

  //Retry policy
    private int maxRetries = 5;
    private long defaultRetryMillis = 2000;
    private long maxBackoffMillis = 60000;

    private HttpClient client;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates an app-only connection using the client-credentials grant, with
   *  <code>Prefer: IdType="ImmutableId"</code> enabled by default (the EMR
   *  persists ids, so immutable ids are required).
   */
    public Connection(String tenantID, String clientID, String clientSecret) throws GraphException {
        this(tenantID, clientID, clientSecret, true);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates an app-only connection using the client-credentials grant.
   *  @param immutableIds when true, sets <code>Prefer: IdType="ImmutableId"</code>
   *  on every request. Ids obtained with this preference are NOT comparable to
   *  ids obtained without it.
   */
    public Connection(String tenantID, String clientID, String clientSecret, boolean immutableIds) throws GraphException {
        this(new Credentials(tenantID, clientID, clientSecret), immutableIds);
        credentials.getAuthorization(); //fail fast on bad credentials
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a connection from the given {@link Credentials} (app-only or
   *  pre-authenticated), with immutable ids enabled.
   */
    public Connection(Credentials credentials){
        this(credentials, true);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Connection(Credentials credentials, boolean immutableIds){
        if (credentials==null) throw new IllegalArgumentException("credentials is required");
        this.credentials = credentials;
        if (immutableIds) preferValues.add("IdType=\"ImmutableId\"");
    }


  //**************************************************************************
  //** setPreferHeader
  //**************************************************************************
  /** Adds a value to the <code>Prefer</code> header sent on every request.
   *  Multiple values are comma-joined into a single header.
   */
    public void setPreferHeader(String pref){
        if (pref!=null && !preferValues.contains(pref)) preferValues.add(pref);
    }

    public List<String> getPreferValues(){
        return Collections.unmodifiableList(preferValues);
    }


  //**************************************************************************
  //** setMaxRetries
  //**************************************************************************
  /** Sets the maximum number of retries on transient errors (429/503/504). */
    public void setMaxRetries(int maxRetries){
        this.maxRetries = Math.max(0, maxRetries);
    }


  //**************************************************************************
  //** getGraphURL
  //**************************************************************************
  /** Returns the Graph base URL, e.g. "https://graph.microsoft.com/v1.0". */
    public String getGraphURL(){
        return graphURL;
    }


  //**************************************************************************
  //** get / post / patch / delete / getBytes
  //**************************************************************************
  /** Issues a GET and returns the parsed JSON body. */
    public JSONObject get(String url) throws GraphException {
        return execute("GET", url, null, null).toJson();
    }

  /** Issues a POST with a JSON body; returns the parsed response (empty for 202/204). */
    public JSONObject post(String url, JSONObject payload) throws GraphException {
        return execute("POST", url, payload, null).toJson();
    }

  /** Issues a PATCH with a JSON body; returns the parsed response (empty for 204). */
    public JSONObject patch(String url, JSONObject payload) throws GraphException {
        return execute("PATCH", url, payload, null).toJson();
    }

  /** Issues a DELETE; succeeds on any 2xx, throws a {@link GraphException} (e.g. 404) otherwise. */
    public void delete(String url) throws GraphException {
        execute("DELETE", url, null, null);
    }

  /** Issues a GET and returns the raw response body (e.g. attachment/$value content). */
    public byte[] getBytes(String url) throws GraphException {
        return execute("GET", url, null, null).getBody();
    }


  //**************************************************************************
  //** execute
  //**************************************************************************
  /** Executes an arbitrary request with optional extra headers. Any 2xx status
   *  is success; 429/503/504 are retried (honouring <code>Retry-After</code>)
   *  with exponential backoff; a 401 refreshes the token and retries once. A
   *  {@link GraphException} carrying the status, Graph error code and request-id
   *  is thrown on non-transient failures or once retries are exhausted.
   */
    public Response execute(String method, String url, JSONObject payload, Map<String, String> headers) throws GraphException {
        String fullURL = resolve(url);
        byte[] body = (payload==null) ? null : payload.toString().getBytes(UTF_8);

      //POST is not idempotent: a 503/504 may arrive after the server already
      //processed the request, so retrying could double-create. Only retry POST on
      //429 (throttled requests are rejected unprocessed). GET/PATCH/DELETE are
      //idempotent and safe to retry on 503/504 as well.
        boolean idempotent = !"POST".equalsIgnoreCase(method);

        int attempt = 0;
        boolean authRetryAvailable = true;

        while (true){
            Map<String, String> h = buildHeaders(body!=null, headers);
            Response response = send(method, fullURL, h, body);
            int status = response.getStatus();

            if (status>=200 && status<300){
                return response;
            }
            else if ((status==429 || (idempotent && (status==503 || status==504))) && attempt<maxRetries){
                sleep(retryWaitMillis(response, attempt));
                attempt++;
            }
            else if (status==401 && authRetryAvailable){
                credentials.invalidate();
                authRetryAvailable = false;
            }
            else{
                throw new GraphException(response);
            }
        }
    }


  //**************************************************************************
  //** send
  //**************************************************************************
  /** Performs a single HTTP request and returns the {@link Response}. This is
   *  the seam unit tests override to replay canned responses offline; production
   *  code uses <code>java.net.http</code>.
   */
    protected Response send(String method, String url, Map<String, String> headers, byte[] body) throws GraphException {
        if (client==null){
            client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        }

        HttpRequest.BodyPublisher publisher = (body==null)
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofByteArray(body);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(120))
            .method(method, publisher);

        if (headers!=null){
            for (Map.Entry<String, String> entry : headers.entrySet()){
                if (entry.getValue()!=null) builder.header(entry.getKey(), entry.getValue());
            }
        }

        try{
            HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            return new Response(response.statusCode(), response.headers().map(), response.body());
        }
        catch(java.io.IOException e){
            throw new GraphException("Transport error calling " + method + " " + url + ": " + e.getMessage(), e);
        }
        catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new GraphException("Interrupted during HTTP request", e);
        }
    }


  //**************************************************************************
  //** sleep
  //**************************************************************************
  /** Backoff delay between retries. Overridable so unit tests incur no real
   *  delay.
   */
    protected void sleep(long millis) throws GraphException {
        try{
            Thread.sleep(millis);
        }
        catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new GraphException("Interrupted during retry backoff", e);
        }
    }


  //**************************************************************************
  //** buildHeaders
  //**************************************************************************
    private Map<String, String> buildHeaders(boolean hasBody, Map<String, String> extraHeaders) throws GraphException {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", credentials.getAuthorization());
        headers.put("Accept", "application/json");
        if (hasBody) headers.put("Content-Type", "application/json; charset=utf-8");

        List<String> prefer = new ArrayList<>(preferValues);
        if (extraHeaders!=null){
            for (Map.Entry<String, String> entry : extraHeaders.entrySet()){
                if ("Prefer".equalsIgnoreCase(entry.getKey())){
                    if (entry.getValue()!=null) prefer.add(entry.getValue());
                }
                else{
                    headers.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (!prefer.isEmpty()) headers.put("Prefer", String.join(", ", prefer));

        return headers;
    }


  //**************************************************************************
  //** retryWaitMillis
  //**************************************************************************
    private long retryWaitMillis(Response response, int attempt){
        String retryAfter = response.getHeader("Retry-After");
        if (retryAfter!=null){
            try{
                long seconds = Long.parseLong(retryAfter.trim());
                return Math.min(maxBackoffMillis, seconds*1000);
            }
            catch(NumberFormatException e){}
        }
        long backoff = defaultRetryMillis * (1L << Math.min(attempt, 20));
        return Math.min(maxBackoffMillis, backoff);
    }


  //**************************************************************************
  //** getPage
  //**************************************************************************
  /** Lazily iterates the items of a Graph collection, transparently following
   *  <code>@odata.nextLink</code> until it is absent. An empty <code>value</code>
   *  array does not stall iteration.
   */
    public Iterable<JSONObject> getPage(String url) throws GraphException {
        return getPage(url, null);
    }

  /** Same as {@link #getPage(String)} but sends the given extra headers (e.g. a
   *  per-request <code>Prefer</code>) on every page request.
   */
    public Iterable<JSONObject> getPage(final String url, final Map<String, String> headers){
        return new Iterable<JSONObject>(){
            public Iterator<JSONObject> iterator(){
                return new PageIterator(url, headers);
            }
        };
    }


  //**************************************************************************
  //** getList
  //**************************************************************************
  /** Collects an entire paged collection into a list. */
    public List<JSONObject> getList(String url, Map<String, String> headers) throws GraphException {
        ArrayList<JSONObject> list = new ArrayList<>();
        try{
            for (JSONObject item : getPage(url, headers)) list.add(item);
        }
        catch(RuntimeException e){
            if (e.getCause() instanceof GraphException) throw (GraphException) e.getCause();
            throw e;
        }
        return list;
    }


  //**************************************************************************
  //** getDelta
  //**************************************************************************
  /** Follows a Graph delta query, accumulating changed items across the
   *  <code>@odata.nextLink</code> chain and returning them with the final
   *  <code>@odata.deltaLink</code>. Removed items keep their <code>@removed</code>
   *  marker.
   */
    public Delta getDelta(String url) throws GraphException {
        return getDelta(url, null);
    }

    public Delta getDelta(String url, Map<String, String> headers) throws GraphException {
        ArrayList<JSONObject> items = new ArrayList<>();
        String deltaLink = null;
        String next = url;
        while (next!=null){
            JSONObject page = execute("GET", next, null, headers).toJson();
            JSONArray value = page.get("value").toJSONArray();
            if (value!=null){
                for (JSONValue v : value) items.add(v.toJSONObject());
            }
            if (!page.get("@odata.nextLink").isNull()){
                next = page.get("@odata.nextLink").toString();
            }
            else{
                next = null;
                if (!page.get("@odata.deltaLink").isNull()){
                    deltaLink = page.get("@odata.deltaLink").toString();
                }
            }
        }
        return new Delta(items, deltaLink);
    }


  //**************************************************************************
  //** resolve
  //**************************************************************************
    private String resolve(String url){
        if (url==null) return graphURL;
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        if (!url.startsWith("/")) url = "/" + url;
        return graphURL + url;
    }


  //**************************************************************************
  //** getResponse (compatibility)
  //**************************************************************************
  /** @deprecated Retained so the existing SharePoint classes keep compiling.
   *  Use {@link #get(String)}.
   */
    @Deprecated
    public JSONObject getResponse(String url) throws GraphException {
        return get(url);
    }




  //**************************************************************************
  //** PageIterator Class
  //**************************************************************************
    private class PageIterator implements Iterator<JSONObject> {
        private final Map<String, String> headers;
        private Iterator<JSONValue> current;
        private String nextLink;
        private JSONObject nextItem;

        PageIterator(String url, Map<String, String> headers){
            this.headers = headers;
            this.nextLink = url;
        }

        public boolean hasNext(){
            if (nextItem!=null) return true;
            try{ nextItem = advance(); }
            catch(GraphException e){ throw new RuntimeException(e); }
            return nextItem!=null;
        }

        public JSONObject next(){
            if (!hasNext()) throw new NoSuchElementException();
            JSONObject item = nextItem;
            nextItem = null;
            return item;
        }

        private JSONObject advance() throws GraphException {
            while (true){
                if (current!=null && current.hasNext()){
                    return current.next().toJSONObject();
                }
                if (nextLink==null) return null;
                JSONObject page = execute("GET", nextLink, null, headers).toJson();
                JSONArray value = page.get("value").toJSONArray();
                current = (value==null) ? null : value.iterator();
                nextLink = page.get("@odata.nextLink").isNull() ? null : page.get("@odata.nextLink").toString();
            }
        }
    }


  //**************************************************************************
  //** Response Class
  //**************************************************************************
  /** Immutable HTTP response: status, headers and raw body, with helpers to
   *  read a header (case insensitively) and parse the body as JSON.
   */
    public static class Response {

        private final int status;
        private final Map<String, List<String>> headers;
        private final byte[] body;

        public Response(int status, Map<String, List<String>> headers, byte[] body){
            this.status = status;
            this.headers = (headers==null) ? Collections.emptyMap() : headers;
            this.body = body;
        }

        public int getStatus(){ return status; }
        public Map<String, List<String>> getHeaders(){ return headers; }
        public byte[] getBody(){ return body; }
        public boolean isEmpty(){ return body==null || body.length==0; }

      /** Returns the first value of the given header (case insensitive), or null. */
        public String getHeader(String name){
            if (name==null) return null;
            for (Map.Entry<String, List<String>> entry : headers.entrySet()){
                if (name.equalsIgnoreCase(entry.getKey())){
                    List<String> values = entry.getValue();
                    if (values!=null && !values.isEmpty()) return values.get(0);
                }
            }
            return null;
        }

      /** Parses the body as JSON; returns an empty object for an empty body. */
        public JSONObject toJson(){
            if (isEmpty()) return new JSONObject();
            return new JSONObject(new String(body, UTF_8));
        }

        public String toString(){
            return "HTTP " + status + (isEmpty() ? "" : "\n" + new String(body, UTF_8));
        }
    }


  //**************************************************************************
  //** Delta Class
  //**************************************************************************
  /** Raw result of a delta query: every changed item collected across the
   *  nextLink chain plus the final deltaLink. Deleted items carry an
   *  <code>@removed</code> marker (see {@link #isRemoved}).
   */
    public static class Delta {

        private final List<JSONObject> items;
        private final String deltaLink;

        public Delta(List<JSONObject> items, String deltaLink){
            this.items = (items==null) ? new ArrayList<>() : items;
            this.deltaLink = deltaLink;
        }

        public List<JSONObject> getItems(){ return items; }
        public String getDeltaLink(){ return deltaLink; }

        public static boolean isRemoved(JSONObject item){
            return item!=null && !item.get("@removed").isNull();
        }
    }


  //**************************************************************************
  //** Query Class
  //**************************************************************************
  /** Assembles a URL path and query string with all parameter values properly
   *  percent-encoded. Keys are emitted verbatim (so OData options keep their
   *  <code>$</code>); values are fully encoded, spaces as <code>%20</code>.
   */
    public static class Query {

        private final String path;
        private final LinkedHashMap<String, String> params = new LinkedHashMap<>();

        public Query(String path){
            this.path = (path==null) ? "" : path;
        }

        public Query set(String key, String value){
            if (key==null) return this;
            if (value==null) params.remove(key);
            else params.put(key, value);
            return this;
        }

        public Query set(String key, int value){
            return set(key, Integer.toString(value));
        }

        public boolean has(String key){
            return params.containsKey(key);
        }

        public String toString(){
            if (params.isEmpty()) return path;
            StringBuilder sb = new StringBuilder(path).append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()){
                if (!first) sb.append("&");
                first = false;
                sb.append(entry.getKey()).append("=").append(encode(entry.getValue()));
            }
            return sb.toString();
        }

      /** Percent-encodes a query value; spaces become %20 (not "+"). */
        public static String encode(String value){
            if (value==null) return "";
            return URLEncoder.encode(value, UTF_8).replace("+", "%20");
        }
    }
}
