package javaxt.azure.graph;
import java.util.*;
import javaxt.json.*;

//******************************************************************************
//**  Node
//******************************************************************************
/**
 *   Base class for all Graph model objects. Each Node is a thin wrapper over
 *   the JSON it was loaded from, so callers can always reach properties the
 *   library has not wrapped yet via {@link #get(String)} / {@link #set(String,Object)}
 *   / {@link #toJson()}.
 *
 *   <p>Node tracks which keys have been {@link #set(String,Object) set} since it
 *   was loaded so that update operations can PATCH only the changed properties
 *   ({@link #getChanges()}).</p>
 *
 *   <p><b>Note on clearing properties:</b> the underlying JSON store removes a
 *   key when it is set to null, so {@link #getChanges()} cannot emit an explicit
 *   JSON <code>null</code>. Clearing a Graph property to null is not supported
 *   through change tracking; set it to an empty value or PATCH directly.</p>
 *
 ******************************************************************************/

public class Node {

    protected Connection conn;
    private JSONObject json;
    private final LinkedHashSet<String> dirty = new LinkedHashSet<>();


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a Node wrapping the given Graph JSON and connection.
   */
    protected Node(JSONObject json, Connection conn){
        this.json = (json==null) ? new JSONObject() : json;
        this.conn = conn;
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a Node bound to the given connection with only its Graph id set
   *  (e.g. to reference an existing object by id without fetching it).
   */
    protected Node(String id, Connection conn){
        json = new JSONObject();
        if (id!=null) json.set("id", id);
        this.conn = conn;
    }


  //**************************************************************************
  //** getID
  //**************************************************************************
  /** Returns the Graph <code>id</code>, or null for a new object not yet saved.
   */
    public String getID(){
        JSONValue v = json.get("id");
        return v.isNull() ? null : v.toString();
    }


  //**************************************************************************
  //** getChangeKey
  //**************************************************************************
  /** Returns the Graph <code>changeKey</code> (an ETag-like optimistic
   *  concurrency token) when present, or null. Passed as <code>If-Match</code>
   *  on PATCH/DELETE.
   */
    public String getChangeKey(){
        JSONValue v = json.get("changeKey");
        return v.isNull() ? null : v.toString();
    }


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Returns the raw value for the given key.
   */
    public JSONValue get(String key){
        return json.get(key);
    }


  //**************************************************************************
  //** has
  //**************************************************************************
  /** Returns true if the given key is present and non-null.
   */
    public boolean has(String key){
        return !json.get(key).isNull();
    }


  //**************************************************************************
  //** getKeys
  //**************************************************************************
  /** Returns the set of property keys in the backing JSON.
   */
    public Set<String> getKeys(){
        return json.keySet();
    }


  //**************************************************************************
  //** set
  //**************************************************************************
  /** Sets a property and records the key as dirty so it is included in the next
   *  {@link #getChanges()} PATCH body.
   */
    public void set(String key, Object val){
        json.set(key, val);
        dirty.add(key);
    }


  //**************************************************************************
  //** markDirty
  //**************************************************************************
  /** Marks a top-level key dirty without changing its value. Used by subclasses
   *  that mutate a nested object in place (e.g. an <code>CalendarEvent</code> editing
   *  its <code>start</code>) so the whole property is re-sent on PATCH.
   */
    protected void markDirty(String key){
        dirty.add(key);
    }


  //**************************************************************************
  //** isDirty
  //**************************************************************************
  /** Returns true if any property has been set since the object was loaded.
   */
    public boolean isDirty(){
        return !dirty.isEmpty();
    }


  //**************************************************************************
  //** getChanges
  //**************************************************************************
  /** Returns a JSON object containing only the properties modified since load,
   *  suitable as a PATCH body. Returns an empty object when nothing changed.
   */
    public JSONObject getChanges(){
        JSONObject changes = new JSONObject();
        for (String key : dirty){
            JSONValue v = json.get(key);
            if (!v.isNull()) changes.set(key, v);
        }
        return changes;
    }


  //**************************************************************************
  //** resetChanges
  //**************************************************************************
  /** Clears the dirty-key set. Called after a successful save.
   */
    protected void resetChanges(){
        dirty.clear();
    }


  //**************************************************************************
  //** replaceJson
  //**************************************************************************
  /** Replaces the backing JSON (e.g. with the server copy returned by a
   *  create/update) and clears change tracking.
   */
    protected void replaceJson(JSONObject json){
        this.json = (json==null) ? new JSONObject() : json;
        dirty.clear();
    }


  //**************************************************************************
  //** getCreatedDateTime
  //**************************************************************************
  /** Returns the Graph <code>createdDateTime</code>, or null.
   */
    public javaxt.utils.Date getCreatedDateTime(){
        return toDate(json.get("createdDateTime"));
    }


  //**************************************************************************
  //** getLastModifiedDateTime
  //**************************************************************************
  /** Returns the Graph <code>lastModifiedDateTime</code>, or null.
   */
    public javaxt.utils.Date getLastModifiedDateTime(){
        return toDate(json.get("lastModifiedDateTime"));
    }


  //**************************************************************************
  //** getLastModifiedDate (compatibility)
  //**************************************************************************
  /** @deprecated Use {@link #getLastModifiedDateTime()}. Retained for the
   *  SharePoint classes.
   */
    @Deprecated
    public javaxt.utils.Date getLastModifiedDate(){
        return getLastModifiedDateTime();
    }


  //**************************************************************************
  //** toDate
  //**************************************************************************
  /** Converts a JSON value to a Date, or null when the value is null.
   */
    private static javaxt.utils.Date toDate(JSONValue v){
        if (v==null || v.isNull()) return null;
        return parseDate(v.toString());
    }


  //**************************************************************************
  //** parseDate
  //**************************************************************************
  /** Parses an ISO date/time string, first truncating any fractional seconds to
   *  3 digits. Graph emits 7-digit fractions (e.g.
   *  <code>2017-04-15T03:00:50.7579581Z</code>) which <code>javaxt.utils.Date</code>
   *  would otherwise read as millions of milliseconds, shifting the time by hours.
   *  Returns null if the string is null or cannot be parsed.
   */
    public static javaxt.utils.Date parseDate(String s){
        if (s==null) return null;
        try{ return new javaxt.utils.Date(s.replaceAll("\\.(\\d{3})\\d+", ".$1")); }
        catch(Exception e){ return null; }
    }


  //**************************************************************************
  //** toJson
  //**************************************************************************
  /** Returns the full backing JSON object.
   */
    public JSONObject toJson(){
        return json;
    }


  //**************************************************************************
  //** toString
  //**************************************************************************
  /** Returns the backing JSON as a pretty-printed string.
   */
    public String toString(){
        return json.toString(4);
    }
}
