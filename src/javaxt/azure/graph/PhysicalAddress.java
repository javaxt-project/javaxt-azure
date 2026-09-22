package javaxt.azure.graph;
import javaxt.json.JSONObject;

//******************************************************************************
//**  PhysicalAddress
//******************************************************************************
/**
 *   Value type mirroring the Graph <code>physicalAddress</code> shape
 *   (<code>{ street, city, state, countryOrRegion, postalCode }</code>).
 *   Graph exposes a single <code>street</code> string; multi-line street
 *   values are newline-separated.
 *
 ******************************************************************************/

public class PhysicalAddress {

    private String street;
    private String city;
    private String state;
    private String countryOrRegion;
    private String postalCode;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates an empty PhysicalAddress.
   */
    public PhysicalAddress(){}


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a PhysicalAddress from a Graph physicalAddress JSON object.
   */
    public PhysicalAddress(JSONObject json){
        if (json!=null){
            street = str(json, "street");
            city = str(json, "city");
            state = str(json, "state");
            countryOrRegion = str(json, "countryOrRegion");
            postalCode = str(json, "postalCode");
        }
    }


  //**************************************************************************
  //** getStreet
  //**************************************************************************
  /** Returns the street (may be multi-line, newline-separated), or null.
   */
    public String getStreet(){ return street; }


  //**************************************************************************
  //** setStreet
  //**************************************************************************
  /** Sets the street (may be multi-line, newline-separated).
   */
    public void setStreet(String street){ this.street = street; }


  //**************************************************************************
  //** getCity
  //**************************************************************************
  /** Returns the city, or null.
   */
    public String getCity(){ return city; }


  //**************************************************************************
  //** setCity
  //**************************************************************************
  /** Sets the city.
   */
    public void setCity(String city){ this.city = city; }


  //**************************************************************************
  //** getState
  //**************************************************************************
  /** Returns the state or province, or null.
   */
    public String getState(){ return state; }


  //**************************************************************************
  //** setState
  //**************************************************************************
  /** Sets the state or province.
   */
    public void setState(String state){ this.state = state; }


  //**************************************************************************
  //** getCountryOrRegion
  //**************************************************************************
  /** Returns the country or region, or null.
   */
    public String getCountryOrRegion(){ return countryOrRegion; }


  //**************************************************************************
  //** setCountryOrRegion
  //**************************************************************************
  /** Sets the country or region.
   */
    public void setCountryOrRegion(String countryOrRegion){
        this.countryOrRegion = countryOrRegion;
    }


  //**************************************************************************
  //** getPostalCode
  //**************************************************************************
  /** Returns the postal code, or null.
   */
    public String getPostalCode(){ return postalCode; }


  //**************************************************************************
  //** setPostalCode
  //**************************************************************************
  /** Sets the postal code.
   */
    public void setPostalCode(String postalCode){
        this.postalCode = postalCode;
    }


  //**************************************************************************
  //** isEmpty
  //**************************************************************************
  /** Returns true if none of the address fields are set.
   */
    public boolean isEmpty(){
        return street==null && city==null && state==null &&
               countryOrRegion==null && postalCode==null;
    }


  //**************************************************************************
  //** toJson
  //**************************************************************************
  /** Returns this address as a Graph physicalAddress JSON object, omitting
   *  null fields.
   */
    public JSONObject toJson(){
        JSONObject json = new JSONObject();
        if (street!=null) json.set("street", street);
        if (city!=null) json.set("city", city);
        if (state!=null) json.set("state", state);
        if (countryOrRegion!=null) json.set("countryOrRegion", countryOrRegion);
        if (postalCode!=null) json.set("postalCode", postalCode);
        return json;
    }


  //**************************************************************************
  //** str
  //**************************************************************************
  /** Returns the string value for the given key, or null.
   */
    private static String str(JSONObject json, String key){
        return json.get(key).isNull() ? null : json.get(key).toString();
    }


  //**************************************************************************
  //** equals
  //**************************************************************************
  /** Returns true if the given object is a PhysicalAddress with equal fields.
   */
    public boolean equals(Object obj){
        if (this==obj) return true;
        if (!(obj instanceof PhysicalAddress)) return false;
        PhysicalAddress o = (PhysicalAddress) obj;
        return java.util.Objects.equals(street, o.street) &&
               java.util.Objects.equals(city, o.city) &&
               java.util.Objects.equals(state, o.state) &&
               java.util.Objects.equals(countryOrRegion, o.countryOrRegion) &&
               java.util.Objects.equals(postalCode, o.postalCode);
    }


  //**************************************************************************
  //** hashCode
  //**************************************************************************
  /** Returns a hash code derived from all address fields.
   */
    public int hashCode(){
        return java.util.Objects.hash(
            street, city, state, countryOrRegion, postalCode
        );
    }
}
