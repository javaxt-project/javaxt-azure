package javaxt.azure.graph;
import java.util.Base64;
import javaxt.json.JSONObject;

//******************************************************************************
//**  Attachment
//******************************************************************************
/**
 *   Thin wrapper over a Graph attachment (<code>fileAttachment</code>,
 *   <code>itemAttachment</code> or <code>referenceAttachment</code>). For file
 *   attachments retrieved with their <code>contentBytes</code>, the decoded
 *   content is available via {@link #getContentBytes()}; larger items are fetched
 *   separately through {@link Email#getAttachmentContent(String)}.
 *
 ******************************************************************************/

public class Attachment {

  /** Maximum size (3 MB) for content that can be sent inline as contentBytes.
   */
    public static final int MAX_INLINE_BYTES = 3 * 1024 * 1024;

    private final JSONObject json;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates an Attachment wrapping the given Graph attachment JSON object.
   */
    public Attachment(JSONObject json){
        this.json = (json==null) ? new JSONObject() : json;
    }


  //**************************************************************************
  //** file
  //**************************************************************************
  /** Builds a file attachment payload from raw bytes (base64-encoded inline).
   */
    public static Attachment file(String name, String contentType, byte[] bytes){
        JSONObject j = new JSONObject();
        j.set("@odata.type", "#microsoft.graph.fileAttachment");
        j.set("name", name);
        if (contentType!=null) j.set("contentType", contentType);
        if (bytes!=null) j.set("contentBytes", Base64.getEncoder().encodeToString(bytes));
        return new Attachment(j);
    }


  //**************************************************************************
  //** getID
  //**************************************************************************
  /** Returns the attachment id, or null.
   */
    public String getID(){ return str("id"); }


  //**************************************************************************
  //** getODataType
  //**************************************************************************
  /** Returns the Graph @odata.type of the attachment, or null.
   */
    public String getODataType(){ return str("@odata.type"); }


  //**************************************************************************
  //** getName
  //**************************************************************************
  /** Returns the attachment name (typically the file name), or null.
   */
    public String getName(){ return str("name"); }


  //**************************************************************************
  //** getContentType
  //**************************************************************************
  /** Returns the content (MIME) type, or null.
   */
    public String getContentType(){ return str("contentType"); }


  //**************************************************************************
  //** getContentID
  //**************************************************************************
  /** Returns the content id used to reference inline attachments, or null.
   */
    public String getContentID(){ return str("contentId"); }


  //**************************************************************************
  //** getSize
  //**************************************************************************
  /** Returns the size in bytes, or null if not set.
   */
    public Integer getSize(){
        return json.get("size").isNull() ? null : json.get("size").toInteger();
    }


  //**************************************************************************
  //** isInline
  //**************************************************************************
  /** Returns true if the attachment is marked as inline.
   */
    public boolean isInline(){
        return !json.get("isInline").isNull() && json.get("isInline").toBoolean();
    }


  //**************************************************************************
  //** isFileAttachment
  //**************************************************************************
  /** Returns true if this is a file attachment (as opposed to an item or
   *  reference attachment).
   */
    public boolean isFileAttachment(){
        String t = getODataType();
        return t!=null && t.toLowerCase().contains("fileattachment");
    }


  //**************************************************************************
  //** getContentBytes
  //**************************************************************************
  /** Returns the decoded content of a file attachment when its base64
   *  <code>contentBytes</code> is present, otherwise null.
   */
    public byte[] getContentBytes(){
        if (json.get("contentBytes").isNull()) return null;
        try{
            return Base64.getDecoder().decode(json.get("contentBytes").toString());
        }
        catch(Exception e){
            return null;
        }
    }


  //**************************************************************************
  //** toJson
  //**************************************************************************
  /** Returns the underlying Graph attachment JSON object.
   */
    public JSONObject toJson(){
        return json;
    }


  //**************************************************************************
  //** str
  //**************************************************************************
  /** Returns the string value for a key, or null.
   */
    private String str(String key){
        return json.get(key).isNull() ? null : json.get(key).toString();
    }
}
