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

    public static final int MAX_INLINE_BYTES = 3 * 1024 * 1024;  //3 MB inline limit

    private final JSONObject json;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Attachment(JSONObject json){
        this.json = (json==null) ? new JSONObject() : json;
    }


  //**************************************************************************
  //** file
  //**************************************************************************
  /** Builds a file attachment payload from raw bytes (base64-encoded inline). */
    public static Attachment file(String name, String contentType, byte[] bytes){
        JSONObject j = new JSONObject();
        j.set("@odata.type", "#microsoft.graph.fileAttachment");
        j.set("name", name);
        if (contentType!=null) j.set("contentType", contentType);
        if (bytes!=null) j.set("contentBytes", Base64.getEncoder().encodeToString(bytes));
        return new Attachment(j);
    }


  //**************************************************************************
  //** accessors
  //**************************************************************************
    public String getID(){ return str("id"); }
    public String getODataType(){ return str("@odata.type"); }
    public String getName(){ return str("name"); }
    public String getContentType(){ return str("contentType"); }
    public String getContentID(){ return str("contentId"); }

    public Integer getSize(){
        return json.get("size").isNull() ? null : json.get("size").toInteger();
    }

    public boolean isInline(){
        return !json.get("isInline").isNull() && json.get("isInline").toBoolean();
    }

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
        try{ return Base64.getDecoder().decode(json.get("contentBytes").toString()); }
        catch(Exception e){ return null; }
    }


  //**************************************************************************
  //** toJson
  //**************************************************************************
    public JSONObject toJson(){ return json; }

    private String str(String key){
        return json.get(key).isNull() ? null : json.get(key).toString();
    }
}
