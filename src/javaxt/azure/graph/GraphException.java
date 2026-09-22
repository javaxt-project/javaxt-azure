package javaxt.azure.graph;
import javaxt.json.JSONObject;

//******************************************************************************
//**  GraphException
//******************************************************************************
/**
 *   Thrown when a Microsoft Graph request fails. Carries the HTTP status code
 *   and, when the response body contains a Graph error object
 *   (<code>{"error":{"code","message","innerError"}}</code>), the Graph error
 *   <code>code</code>, the <code>request-id</code> and the raw error object so
 *   callers can react to specific failures (e.g. a 404 vs a 403).
 *
 ******************************************************************************/

public class GraphException extends Exception {

    private final int status;
    private final String code;
    private final String requestID;
    private final JSONObject error;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a GraphException from a failed {@link Response}, extracting the
   *  Graph error <code>code</code>, <code>message</code> and <code>request-id</code>
   *  when present.
   */
    public GraphException(Connection.Response response){
        this(response, parseError(response));
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Internal constructor that sets the status, Graph error code and request-id
   *  from the response and the parsed error object.
   */
    private GraphException(Connection.Response response, JSONObject error){
        super(buildMessage(response, error));
        this.status = response==null ? 0 : response.getStatus();
        this.error = error;
        if (error!=null){
            this.code = error.get("code").isNull() ? null : error.get("code").toString();
        }
        else{
            this.code = null;
        }
        String rid = null;
        if (error!=null && !error.get("innerError").isNull()){
            JSONObject inner = error.get("innerError").toJSONObject();
            if (inner!=null && !inner.get("request-id").isNull()){
                rid = inner.get("request-id").toString();
            }
        }
        if (rid==null && response!=null){
            rid = response.getHeader("request-id");
            if (rid==null) rid = response.getHeader("client-request-id");
        }
        this.requestID = rid;
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a GraphException with a plain message (e.g. a transport level
   *  failure with no HTTP response).
   */
    public GraphException(String message){
        super(message);
        this.status = 0;
        this.code = null;
        this.requestID = null;
        this.error = null;
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a GraphException with a plain message and an underlying cause
   *  (e.g. an I/O or interruption failure).
   */
    public GraphException(String message, Throwable cause){
        super(message, cause);
        this.status = 0;
        this.code = null;
        this.requestID = null;
        this.error = null;
    }


  //**************************************************************************
  //** getStatus
  //**************************************************************************
  /** Returns the HTTP status code associated with the failure, or 0 if the
   *  failure occurred before a response was received.
   */
    public int getStatus(){
        return status;
    }


  //**************************************************************************
  //** getCode
  //**************************************************************************
  /** Returns the Graph error code (e.g. "ErrorItemNotFound") or null.
   */
    public String getCode(){
        return code;
    }


  //**************************************************************************
  //** getRequestID
  //**************************************************************************
  /** Returns the Graph request-id, useful when opening a support ticket, or null.
   */
    public String getRequestID(){
        return requestID;
    }


  //**************************************************************************
  //** getError
  //**************************************************************************
  /** Returns the raw Graph error object (<code>{"code","message","innerError"}</code>)
   *  or null if the response body did not contain one.
   */
    public JSONObject getError(){
        return error;
    }


  //**************************************************************************
  //** parseError
  //**************************************************************************
  /** Extracts the Graph <code>error</code> object from a response body, or null.
   */
    private static JSONObject parseError(Connection.Response response){
        if (response==null) return null;
        try{
            JSONObject json = response.toJson();
            if (json!=null && !json.get("error").isNull()){
                return json.get("error").toJSONObject();
            }
        }
        catch(Exception e){}
        return null;
    }


  //**************************************************************************
  //** buildMessage
  //**************************************************************************
  /** Builds the exception message from the response status and Graph error.
   */
    private static String buildMessage(Connection.Response response, JSONObject error){
        StringBuilder sb = new StringBuilder("Graph request failed");
        if (response!=null) sb.append(" (HTTP ").append(response.getStatus()).append(")");
        if (error!=null){
            if (!error.get("code").isNull()) sb.append(": ").append(error.get("code"));
            if (!error.get("message").isNull()) sb.append(" - ").append(error.get("message"));
        }
        return sb.toString();
    }
}
