package javaxt.azure.graph;
import java.util.*;
import javaxt.json.*;

//******************************************************************************
//**  Item
//******************************************************************************
/**
 *   Base class for Graph <code>outlookItem</code>-derived resources
 *   (<code>event</code>, <code>contact</code>, <code>message</code>). Adds the
 *   properties and operations those resources share on top of {@link Node}:
 *   <code>categories</code> and open extensions.
 *
 *   <p>Open-extension and other per-item operations need the item's resource path
 *   (e.g. <code>/users/{u}/events/{id}</code>), which the owning
 *   {@link Calendar}/{@link ContactFolder} stamps on the item when it is loaded
 *   or created.</p>
 *
 ******************************************************************************/

public abstract class Item extends Node {

  //Resource path for per-item operations (open extensions, etc.), set by the
  //owning folder/calendar. Form: /users/{u}/events/{id} or /users/{u}/contacts/{id}
    protected String resourcePath;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates an Item from Graph JSON bound to the given connection.
   */
    protected Item(JSONObject json, Connection conn){
        super(json, conn);
    }


  //**************************************************************************
  //** setResourcePath
  //**************************************************************************
  /** Sets the resource path used for per-item operations.
   */
    void setResourcePath(String resourcePath){ this.resourcePath = resourcePath; }


  //**************************************************************************
  //** getResourcePath
  //**************************************************************************
  /** Returns the resource path used for per-item operations.
   */
    String getResourcePath(){ return resourcePath; }


  //**************************************************************************
  //** getCategories
  //**************************************************************************
  /** Returns the item's category names, or an empty list.
   */
    public List<String> getCategories(){
        ArrayList<String> list = new ArrayList<>();
        if (!get("categories").isNull()){
            for (JSONValue v : get("categories").toJSONArray()) list.add(v.toString());
        }
        return list;
    }


  //**************************************************************************
  //** setCategories
  //**************************************************************************
  /** Sets the item's category names (nulls are skipped).
   */
    public void setCategories(List<String> categories){
        JSONArray arr = new JSONArray();
        if (categories!=null) for (String c : categories) if (c!=null) arr.add(c);
        set("categories", arr);
    }


  //**************************************************************************
  //** getBody
  //**************************************************************************
  /** Returns the body content (the text/html string), or null. Applies to
   *  events and emails; contacts have no body.
   */
    public String getBody(){
        if (get("body").isNull()) return null;
        JSONValue content = get("body").toJSONObject().get("content");
        return content.isNull() ? null : content.toString();
    }


  //**************************************************************************
  //** getBodyType
  //**************************************************************************
  /** Returns the body content type ("text" or "html"), or null.
   */
    public String getBodyType(){
        if (get("body").isNull()) return null;
        JSONValue type = get("body").toJSONObject().get("contentType");
        return type.isNull() ? null : type.toString();
    }


  //**************************************************************************
  //** setBody
  //**************************************************************************
  /** Sets the body content and format (Graph <code>itemBody</code> shape).
   *  @param format "text" or "html"; defaults to "text" when null.
   */
    public void setBody(String body, String format){
        if (body==null){ set("body", null); return; }
        JSONObject json = new JSONObject();
        json.set("contentType", (format==null || format.trim().isEmpty()) ? "text" : format.toLowerCase());
        json.set("content", body);
        set("body", json);
    }


  //**************************************************************************
  //** setTextBody
  //**************************************************************************
  /** Sets the body as plain text.
   */
    public void setTextBody(String body){ setBody(body, "text"); }


  //**************************************************************************
  //** setHtmlBody
  //**************************************************************************
  /** Sets the body as HTML.
   */
    public void setHtmlBody(String body){ setBody(body, "html"); }


  //**************************************************************************
  //** hasAttachments
  //**************************************************************************
  /** True when this item has attachments. Applies to events and emails;
   *  contacts have no attachments.
   */
    public boolean hasAttachments(){
        return !get("hasAttachments").isNull() && get("hasAttachments").toBoolean();
    }


  //**************************************************************************
  //** getAttachments
  //**************************************************************************
  /** Lists this item's attachments (a separate request).
   */
    public List<Attachment> getAttachments() throws GraphException {
        requirePath();
        ArrayList<Attachment> list = new ArrayList<>();
        for (JSONObject json : conn.getList(resourcePath + "/attachments", null)){
            list.add(new Attachment(json));
        }
        return list;
    }


  //**************************************************************************
  //** getAttachmentContent
  //**************************************************************************
  /** Downloads the raw content of a file attachment.
   */
    public byte[] getAttachmentContent(String attachmentId) throws GraphException {
        requirePath();
        return conn.getBytes(resourcePath + "/attachments/" + attachmentId + "/$value");
    }


  //**************************************************************************
  //** addFileAttachment
  //**************************************************************************
  /** Adds an inline file attachment (&le; 3 MB). Throws for larger payloads,
   *  which require an upload session (out of scope).
   */
    public Attachment addFileAttachment(String name, String contentType, byte[] bytes) throws GraphException {
        requirePath();
        if (bytes!=null && bytes.length > Attachment.MAX_INLINE_BYTES){
            throw new GraphException("Attachment '" + name + "' is " + bytes.length +
                " bytes; inline attachments are limited to " + Attachment.MAX_INLINE_BYTES +
                " bytes (use an upload session for larger files)");
        }
        JSONObject server = conn.post(resourcePath + "/attachments", Attachment.file(name, contentType, bytes).toJson());
        return new Attachment(server);
    }


  //**************************************************************************
  //** getExtension
  //**************************************************************************
  /** Returns the named open extension, either from data already $expand-ed onto
   *  this item or by fetching it from Graph.
   */
    public JSONObject getExtension(String name) throws GraphException {
        if (has("extensions")){
          //Extensions were $expand-ed, so the answer is definitive: scan the
          //array and return null WITHOUT a network call when absent (which is the
          //common case for Outlook-created items). Match on the bare
          //extensionName or either id form.
            String qualified = "Microsoft.OutlookServices.OpenTypeExtension." + name;
            for (JSONValue v : get("extensions").toJSONArray()){
                JSONObject ext = v.toJSONObject();
                String id = ext.get("id").isNull() ? null : ext.get("id").toString();
                String en = ext.get("extensionName").isNull() ? null : ext.get("extensionName").toString();
                if (name.equals(id) || name.equals(en) || qualified.equals(id)) return ext;
            }
            return null;
        }
        if (conn!=null && resourcePath!=null){
            try{ return conn.get(resourcePath + "/extensions/" + Connection.Query.encode(name)); }
            catch(GraphException e){ if (e.getStatus()==404) return null; throw e; }
        }
        return null;
    }


  //**************************************************************************
  //** expandExtensions
  //**************************************************************************
  /** Builds a <code>$expand=extensions($filter=id eq '&lt;name&gt;')</code> clause
   *  for the given open-extension names. The bare name is Graph's documented and
   *  tenant-verified filter form; {@link #getExtension} tolerates either the bare
   *  name or the qualified id when scanning the expanded result. Returns null when
   *  no names are given.
   */
    static String expandExtensions(List<String> names){
        if (names==null || names.isEmpty()) return null;
        StringBuilder filter = new StringBuilder();
        for (int i=0; i<names.size(); i++){
            if (i>0) filter.append(" or ");
            filter.append("id eq '").append(names.get(i)).append("'");
        }
        return "extensions($filter=" + filter + ")";
    }


  //**************************************************************************
  //** setExtension
  //**************************************************************************
  /** Creates or updates the named open extension with the given data.
   *  <p>Writing an extension bumps the item's server-side <code>changeKey</code>,
   *  so the stale local changeKey is cleared afterwards; this lets a subsequent
   *  {@code update...()} succeed (it will not send a now-invalid
   *  <code>If-Match</code>) in the common create&nbsp;&rarr; stamp&nbsp;&rarr;
   *  update flow.</p>
   */
    public JSONObject setExtension(String name, JSONObject data) throws GraphException {
        requirePath();
        JSONObject payload = (data==null) ? new JSONObject() : new JSONObject(data.toString());
        payload.set("@odata.type", "microsoft.graph.openTypeExtension");
        payload.set("extensionName", name);
        JSONObject result;
        try{
            result = conn.post(resourcePath + "/extensions", payload);
        }
        catch(GraphException e){
            if (e.getStatus()==409 || e.getStatus()==400){
                result = conn.patch(resourcePath + "/extensions/" + Connection.Query.encode(name), payload);
            }
            else throw e;
        }
        clearChangeKey();
        return result;
    }


  //**************************************************************************
  //** removeExtension
  //**************************************************************************
  /** Removes the named open extension.
   */
    public void removeExtension(String name) throws GraphException {
        requirePath();
        conn.delete(resourcePath + "/extensions/" + Connection.Query.encode(name));
        clearChangeKey();
    }


  //**************************************************************************
  //** clearChangeKey
  //**************************************************************************
  /** Drops the locally cached changeKey (without marking the item dirty) after
   *  an operation that changed it server-side.
   */
    private void clearChangeKey(){
        if (!toJson().get("changeKey").isNull()) toJson().remove("changeKey");
    }


  //**************************************************************************
  //** writableChanges
  //**************************************************************************
  /** Returns {@link #getChanges()} with server-managed, read-only properties
   *  removed, so a PATCH body never carries keys Graph rejects. This makes the
   *  natural "bind an id then set a few fields" update pattern safe even when the
   *  caller set <code>id</code> (or other read-only keys) explicitly.
   */
    protected JSONObject writableChanges(){
        JSONObject changes = getChanges();
        JSONObject out = new JSONObject();
        for (String key : changes.keySet()){
            if (key.startsWith("@")) continue;          //@odata.* and similar
            if (READ_ONLY.contains(key)) continue;
            out.set(key, changes.get(key));
        }
        return out;
    }

    private static final Set<String> READ_ONLY = new HashSet<>(Arrays.asList(
        "id", "changeKey", "iCalUId", "type", "seriesMasterId", "organizer",
        "createdDateTime", "lastModifiedDateTime", "webLink"));


  //**************************************************************************
  //** str
  //**************************************************************************
  /** Returns a string property, or null.
   */
    protected String str(String key){
        return get(key).isNull() ? null : get(key).toString();
    }


  //**************************************************************************
  //** requirePath
  //**************************************************************************
  /** Throws if this item is not bound to a mailbox (no connection/resource path).
   */
    protected void requirePath() throws GraphException {
        if (conn==null || resourcePath==null){
            throw new GraphException("This item is not bound to a mailbox; save it first");
        }
    }
}
