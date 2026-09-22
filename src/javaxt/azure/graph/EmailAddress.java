package javaxt.azure.graph;
import java.util.regex.Pattern;
import javaxt.json.JSONObject;

//******************************************************************************
//**  EmailAddress
//******************************************************************************
/**
 *   Value type mirroring the Graph <code>emailAddress</code> shape
 *   (<code>{ name, address }</code>). The address is trimmed, lowercased and
 *   validated against a sane regular expression on construction. Used for
 *   organizer, attendees, message recipients and contact emails.
 *
 ******************************************************************************/

public class EmailAddress {

    private final String name;
    private final String address;

    private static final Pattern EMAIL = Pattern.compile(
        "^[A-Za-z0-9._%+'\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates an EmailAddress with the given address and no display name.
   */
    public EmailAddress(String address){
        this(null, address);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** @throws IllegalArgumentException if the address is null or malformed.
   */
    public EmailAddress(String name, String address){
        if (address==null) throw new IllegalArgumentException("Email address is required");
        String a = address.trim().toLowerCase();
        if (!EMAIL.matcher(a).matches()){
            throw new IllegalArgumentException("Invalid email address: " + address);
        }
        this.address = a;
        this.name = (name==null || name.trim().isEmpty()) ? null : name.trim();
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates an EmailAddress from a Graph emailAddress JSON object.
   */
    public EmailAddress(JSONObject json){
        this(json==null || json.get("name").isNull() ? null : json.get("name").toString(),
             json==null ? null : json.get("address").toString());
    }


  //**************************************************************************
  //** getName
  //**************************************************************************
  /** Returns the display name, or null if none was set.
   */
    public String getName(){ return name; }


  //**************************************************************************
  //** getAddress
  //**************************************************************************
  /** Returns the normalized (trimmed, lowercased) email address.
   */
    public String getAddress(){ return address; }


  //**************************************************************************
  //** toJson
  //**************************************************************************
  /** Returns this email address as a Graph emailAddress JSON object.
   */
    public JSONObject toJson(){
        JSONObject json = new JSONObject();
        if (name!=null) json.set("name", name);
        json.set("address", address);
        return json;
    }


  //**************************************************************************
  //** isValid
  //**************************************************************************
  /** Returns true if the given string is a syntactically valid email address.
   */
    public static boolean isValid(String address){
        if (address==null) return false;
        return EMAIL.matcher(address.trim().toLowerCase()).matches();
    }


  //**************************************************************************
  //** equals
  //**************************************************************************
  /** Returns true if the given object is an EmailAddress with the same name and address.
   */
    public boolean equals(Object obj){
        if (this==obj) return true;
        if (!(obj instanceof EmailAddress)) return false;
        EmailAddress o = (EmailAddress) obj;
        return address.equals(o.address) && java.util.Objects.equals(name, o.name);
    }


  //**************************************************************************
  //** hashCode
  //**************************************************************************
  /** Returns a hash code derived from the name and address.
   */
    public int hashCode(){
        return java.util.Objects.hash(name, address);
    }


  //**************************************************************************
  //** toString
  //**************************************************************************
  /** Returns "name &lt;address&gt;", or just the address when there is no name.
   */
    public String toString(){
        return name==null ? address : (name + " <" + address + ">");
    }
}
