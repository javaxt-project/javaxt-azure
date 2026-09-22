package javaxt.azure.graph;
import java.util.*;
import javaxt.json.*;

//******************************************************************************
//**  Contact
//******************************************************************************
/**
 *   Represents a Microsoft Graph personal contact. Exposes Graph-native property
 *   access (via {@link Node}/{@link Item}) alongside typed convenience accessors,
 *   including helpers that flatten Graph's separate phone fields
 *   (<code>businessPhones</code>, <code>homePhones</code>, <code>mobilePhone</code>)
 *   into a single {@link PhoneNumber} list and back.
 *
 *   <p>Graph limits: at most 3 <code>emailAddresses</code>, 2
 *   <code>businessPhones</code>, 2 <code>homePhones</code> and 1
 *   <code>mobilePhone</code>. The setters here enforce those limits (extra values
 *   are dropped).</p>
 *
 ******************************************************************************/

public class Contact extends Item {

  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new, empty contact to be populated and passed to
   *  {@link ContactFolder#createContact}.
   */
    public Contact(){
        super(new JSONObject(), null);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Wraps a contact returned by Microsoft Graph, bound to the connection used
   *  to fetch it so it can be updated or deleted.
   */
    public Contact(JSONObject json, Connection conn){
        super(json, conn);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Binds to an existing contact by id (e.g. to PATCH a contact tracked by id)
   *  without marking <code>id</code> as a changed field.
   */
    public Contact(String id){
        super(new JSONObject(), null);
        if (id!=null) toJson().set("id", id);
    }


  //**************************************************************************
  //** getGivenName
  //**************************************************************************
  /** Returns the contact's given (first) name.
   */
    public String getGivenName(){ return str("givenName"); }


  //**************************************************************************
  //** setGivenName
  //**************************************************************************
  /** Sets the contact's given (first) name.
   */
    public void setGivenName(String v){ set("givenName", v); }


  //**************************************************************************
  //** getSurname
  //**************************************************************************
  /** Returns the contact's surname (last name).
   */
    public String getSurname(){ return str("surname"); }


  //**************************************************************************
  //** setSurname
  //**************************************************************************
  /** Sets the contact's surname (last name).
   */
    public void setSurname(String v){ set("surname", v); }


  //**************************************************************************
  //** getMiddleName
  //**************************************************************************
  /** Returns the contact's middle name.
   */
    public String getMiddleName(){ return str("middleName"); }


  //**************************************************************************
  //** setMiddleName
  //**************************************************************************
  /** Sets the contact's middle name.
   */
    public void setMiddleName(String v){ set("middleName", v); }


  //**************************************************************************
  //** getDisplayName
  //**************************************************************************
  /** Returns the contact's display name.
   */
    public String getDisplayName(){ return str("displayName"); }


  //**************************************************************************
  //** setDisplayName
  //**************************************************************************
  /** Sets the contact's display name.
   */
    public void setDisplayName(String v){ set("displayName", v); }


  //**************************************************************************
  //** getFileAs
  //**************************************************************************
  /** Returns the name under which Graph files the contact (used for sorting).
   */
    public String getFileAs(){ return str("fileAs"); }


  //**************************************************************************
  //** setFileAs
  //**************************************************************************
  /** Sets the name under which Graph files the contact (used for sorting).
   */
    public void setFileAs(String v){ set("fileAs", v); }


  //**************************************************************************
  //** getNickName
  //**************************************************************************
  /** Returns the contact's nickname.
   */
    public String getNickName(){ return str("nickName"); }


  //**************************************************************************
  //** setNickName
  //**************************************************************************
  /** Sets the contact's nickname.
   */
    public void setNickName(String v){ set("nickName", v); }


  //**************************************************************************
  //** getInitials
  //**************************************************************************
  /** Returns the contact's initials.
   */
    public String getInitials(){ return str("initials"); }


  //**************************************************************************
  //** getTitle
  //**************************************************************************
  /** Returns the contact's honorific title (e.g. "Dr", "Mr").
   */
    public String getTitle(){ return str("title"); }


  //**************************************************************************
  //** setTitle
  //**************************************************************************
  /** Sets the contact's honorific title (e.g. "Dr", "Mr").
   */
    public void setTitle(String v){ set("title", v); }


  //**************************************************************************
  //** getJobTitle
  //**************************************************************************
  /** Returns the contact's job title.
   */
    public String getJobTitle(){ return str("jobTitle"); }


  //**************************************************************************
  //** setJobTitle
  //**************************************************************************
  /** Sets the contact's job title.
   */
    public void setJobTitle(String v){ set("jobTitle", v); }


  //**************************************************************************
  //** getCompanyName
  //**************************************************************************
  /** Returns the name of the contact's company.
   */
    public String getCompanyName(){ return str("companyName"); }


  //**************************************************************************
  //** setCompanyName
  //**************************************************************************
  /** Sets the name of the contact's company.
   */
    public void setCompanyName(String v){ set("companyName", v); }


  //**************************************************************************
  //** getDepartment
  //**************************************************************************
  /** Returns the contact's department.
   */
    public String getDepartment(){ return str("department"); }


  //**************************************************************************
  //** setDepartment
  //**************************************************************************
  /** Sets the contact's department.
   */
    public void setDepartment(String v){ set("department", v); }


  //**************************************************************************
  //** getPersonalNotes
  //**************************************************************************
  /** Returns any free-text personal notes stored on the contact.
   */
    public String getPersonalNotes(){ return str("personalNotes"); }


  //**************************************************************************
  //** setPersonalNotes
  //**************************************************************************
  /** Sets free-text personal notes on the contact.
   */
    public void setPersonalNotes(String v){ set("personalNotes", v); }


  //**************************************************************************
  //** getParentFolderID
  //**************************************************************************
  /** Returns the id of the folder that contains this contact.
   */
    public String getParentFolderID(){ return str("parentFolderId"); }


  //**************************************************************************
  //** getBirthday
  //**************************************************************************
  /** Returns the contact's birthday (interpreted in UTC, as Graph stores it), or
   *  null. Graph may use a placeholder year when the year is unknown.
   */
    public javaxt.utils.Date getBirthday(){
        javaxt.utils.Date d = parseDate(str("birthday"));
        if (d!=null) d.setTimeZone("UTC");
        return d;
    }


  //**************************************************************************
  //** setBirthday
  //**************************************************************************
  /** Sets the contact's birthday. Stored as midnight UTC on the given date.
   */
    public void setBirthday(javaxt.utils.Date birthday){
        if (birthday==null){ set("birthday", null); return; }
        set("birthday", birthday.toString("yyyy-MM-dd") + "T00:00:00Z");
    }


  //**************************************************************************
  //** getEmailAddresses
  //**************************************************************************
  /** Returns the contact's email addresses (Graph stores at most 3). Malformed
   *  entries are skipped.
   */
    public List<EmailAddress> getEmailAddresses(){
        ArrayList<EmailAddress> list = new ArrayList<>();
        if (!get("emailAddresses").isNull()){
            for (JSONValue v : get("emailAddresses").toJSONArray()){
                try{ list.add(new EmailAddress(v.toJSONObject())); }
                catch(Exception e){ /* skip malformed */ }
            }
        }
        return list;
    }


  //**************************************************************************
  //** setEmailAddresses
  //**************************************************************************
  /** Sets the contact's email addresses (truncated to the Graph limit of 3).
   */
    public void setEmailAddresses(List<EmailAddress> emails){
        JSONArray arr = new JSONArray();
        if (emails!=null){
            for (EmailAddress e : emails){
                if (e==null) continue;
                arr.add(e.toJson());
                if (arr.length()>=3) break;
            }
        }
        set("emailAddresses", arr);
    }


  //**************************************************************************
  //** getBusinessPhones
  //**************************************************************************
  /** Returns the contact's business phone numbers (Graph stores at most 2).
   */
    public List<String> getBusinessPhones(){ return strList("businessPhones"); }


  //**************************************************************************
  //** getHomePhones
  //**************************************************************************
  /** Returns the contact's home phone numbers (Graph stores at most 2).
   */
    public List<String> getHomePhones(){ return strList("homePhones"); }


  //**************************************************************************
  //** getMobilePhone
  //**************************************************************************
  /** Returns the contact's single mobile phone number.
   */
    public String getMobilePhone(){ return str("mobilePhone"); }


  //**************************************************************************
  //** getPhoneNumbers
  //**************************************************************************
  /** Flattens Graph's business/home/mobile phone fields into one typed list.
   */
    public List<PhoneNumber> getPhoneNumbers(){
        ArrayList<PhoneNumber> list = new ArrayList<>();
        for (String s : getBusinessPhones()) list.add(new PhoneNumber(PhoneNumber.BUSINESS, s));
        for (String s : getHomePhones()) list.add(new PhoneNumber(PhoneNumber.HOME, s));
        String mobile = getMobilePhone();
        if (mobile!=null) list.add(new PhoneNumber(PhoneNumber.MOBILE, mobile));
        return list;
    }


  //**************************************************************************
  //** setPhoneNumbers
  //**************************************************************************
  /** Unflattens a typed phone list into Graph's fields, honouring the limits
   *  (business max 2, home max 2, mobile 1). Numbers typed "other", and any
   *  beyond the limits, are dropped (Graph contacts have no other-phone field).
   */
    public void setPhoneNumbers(List<PhoneNumber> phones){
        JSONArray business = new JSONArray();
        JSONArray home = new JSONArray();
        String mobile = null;
        if (phones!=null){
            for (PhoneNumber p : phones){
                if (p==null || p.getNumber()==null) continue;
                String type = p.getType();
                if (PhoneNumber.BUSINESS.equals(type)){
                    if (business.length()<2) business.add(p.getNumber());
                }
                else if (PhoneNumber.HOME.equals(type)){
                    if (home.length()<2) home.add(p.getNumber());
                }
                else if (PhoneNumber.MOBILE.equals(type)){
                    if (mobile==null) mobile = p.getNumber();
                }
                //"other" has no Graph field -> dropped
            }
        }
        set("businessPhones", business);
        set("homePhones", home);
        set("mobilePhone", mobile);
    }


  //**************************************************************************
  //** getHomeAddress
  //**************************************************************************
  /** Returns the contact's home address, or null if not set.
   */
    public PhysicalAddress getHomeAddress(){ return address("homeAddress"); }


  //**************************************************************************
  //** setHomeAddress
  //**************************************************************************
  /** Sets the contact's home address (a null or empty address clears it).
   */
    public void setHomeAddress(PhysicalAddress a){ setAddress("homeAddress", a); }


  //**************************************************************************
  //** getBusinessAddress
  //**************************************************************************
  /** Returns the contact's business address, or null if not set.
   */
    public PhysicalAddress getBusinessAddress(){ return address("businessAddress"); }


  //**************************************************************************
  //** setBusinessAddress
  //**************************************************************************
  /** Sets the contact's business address (a null or empty address clears it).
   */
    public void setBusinessAddress(PhysicalAddress a){ setAddress("businessAddress", a); }


  //**************************************************************************
  //** getOtherAddress
  //**************************************************************************
  /** Returns the contact's other address, or null if not set.
   */
    public PhysicalAddress getOtherAddress(){ return address("otherAddress"); }


  //**************************************************************************
  //** setOtherAddress
  //**************************************************************************
  /** Sets the contact's other address (a null or empty address clears it).
   */
    public void setOtherAddress(PhysicalAddress a){ setAddress("otherAddress", a); }


  //**************************************************************************
  //** strList
  //**************************************************************************
  /** Returns the string values for an array key, skipping nulls.
   */
    private List<String> strList(String key){
        ArrayList<String> list = new ArrayList<>();
        if (!get(key).isNull()){
            for (JSONValue v : get(key).toJSONArray()) if (!v.isNull()) list.add(v.toString());
        }
        return list;
    }


  //**************************************************************************
  //** address
  //**************************************************************************
  /** Returns the PhysicalAddress for a key, or null.
   */
    private PhysicalAddress address(String key){
        return get(key).isNull() ? null : new PhysicalAddress(get(key).toJSONObject());
    }


  //**************************************************************************
  //** setAddress
  //**************************************************************************
  /** Sets the address for a key, clearing it when null or empty.
   */
    private void setAddress(String key, PhysicalAddress a){
        set(key, (a==null || a.isEmpty()) ? null : a.toJson());
    }
}
