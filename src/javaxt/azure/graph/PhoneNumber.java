package javaxt.azure.graph;

//******************************************************************************
//**  PhoneNumber
//******************************************************************************
/**
 *   A typed phone number. Graph contacts do not have a single "phone number"
 *   type; they expose <code>businessPhones[]</code>, <code>homePhones[]</code>
 *   and a single <code>mobilePhone</code>. This class pairs a number with a
 *   {@link #getType() type} so {@link Contact} can flatten those fields into one
 *   list and back. See {@link Contact#getPhoneNumbers()} /
 *   {@link Contact#setPhoneNumbers}.
 *
 ******************************************************************************/

public class PhoneNumber {

    public static final String BUSINESS = "business";
    public static final String HOME = "home";
    public static final String MOBILE = "mobile";
    public static final String OTHER = "other";

    private final String type;
    private final String number;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public PhoneNumber(String type, String number){
        this.type = (type==null || type.trim().isEmpty()) ? OTHER : type.trim().toLowerCase();
        this.number = (number==null) ? null : number.trim();
    }


  //**************************************************************************
  //** getType / getNumber
  //**************************************************************************
    public String getType(){ return type; }
    public String getNumber(){ return number; }


  //**************************************************************************
  //** getDigits
  //**************************************************************************
  /** Returns the number stripped to digits (keeping a leading "+"), e.g. for
   *  normalized comparison.
   */
    public String getDigits(){
        if (number==null) return null;
        String s = number.replaceAll("[^0-9+]", "");
        if (s.indexOf('+') > 0) s = s.replace("+", "");  //keep only a leading +
        return s;
    }


  //**************************************************************************
  //** equals / hashCode / toString
  //**************************************************************************
    public boolean equals(Object obj){
        if (this==obj) return true;
        if (!(obj instanceof PhoneNumber)) return false;
        PhoneNumber o = (PhoneNumber) obj;
        return java.util.Objects.equals(type, o.type) &&
               java.util.Objects.equals(getDigits(), o.getDigits());
    }

    public int hashCode(){
        return java.util.Objects.hash(type, getDigits());
    }

    public String toString(){
        return type + ": " + number;
    }
}
