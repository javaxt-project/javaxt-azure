package javaxt.azure.graph;
import javaxt.json.JSONObject;

//******************************************************************************
//**  PhysicalAddress
//******************************************************************************
/**
 *   Value type mirroring the Graph <code>physicalAddress</code> shape
 *   (<code>{ street, city, state, countryOrRegion, postalCode }</code>). Unlike
 *   EWS, Graph exposes a single <code>street</code> string; multi-line street
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
    public PhysicalAddress(){}


  //**************************************************************************
  //** Constructor
  //**************************************************************************
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
  //** getters / setters
  //**************************************************************************
    public String getStreet(){ return street; }
    public void setStreet(String street){ this.street = street; }

    public String getCity(){ return city; }
    public void setCity(String city){ this.city = city; }

    public String getState(){ return state; }
    public void setState(String state){ this.state = state; }

    public String getCountryOrRegion(){ return countryOrRegion; }
    public void setCountryOrRegion(String countryOrRegion){ this.countryOrRegion = countryOrRegion; }

    public String getPostalCode(){ return postalCode; }
    public void setPostalCode(String postalCode){ this.postalCode = postalCode; }


  //**************************************************************************
  //** isEmpty
  //**************************************************************************
    public boolean isEmpty(){
        return street==null && city==null && state==null &&
               countryOrRegion==null && postalCode==null;
    }


  //**************************************************************************
  //** toJson
  //**************************************************************************
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
    private static String str(JSONObject json, String key){
        return json.get(key).isNull() ? null : json.get(key).toString();
    }


  //**************************************************************************
  //** equals / hashCode
  //**************************************************************************
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

    public int hashCode(){
        return java.util.Objects.hash(street, city, state, countryOrRegion, postalCode);
    }
}
