package javaxt.azure.graph;
import java.util.*;
import javaxt.json.*;

//******************************************************************************
//**  ContactFolder
//******************************************************************************
/**
 *   Represents a Microsoft Graph contact folder
 *
 ******************************************************************************/

public class ContactFolder extends Folder {

  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Wraps a Graph contact folder, bound to a user-scoped connection (see
   *  Connection.forUser).
   */
    public ContactFolder(JSONObject json, Connection conn){
        super(json, conn);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Binds to the mailbox's default contact folder
   *  (<code>/users/{id}/contacts</code>) on the given user-scoped connection.
   */
    public ContactFolder(Connection conn){
        super(new JSONObject(), conn);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Binds to a named contact folder by its id, on the given user-scoped
   *  connection.
   */
    public ContactFolder(String folderId, Connection conn){
        super(folderId, conn);
    }


  //**************************************************************************
  //** getContacts
  //**************************************************************************
  /** Returns all the contacts in this folder using default query options.
   */
    public List<Contact> getContacts() throws GraphException {
        return getContacts(new ContactQuery());
    }


  //**************************************************************************
  //** getContacts
  //**************************************************************************
  /** Returns the contacts in this folder, following paging to the end (or
   *  until the query's limit, if any, is reached).
   */
    public List<Contact> getContacts(ContactQuery opts) throws GraphException {
        if (opts==null) opts = new ContactQuery();

        Connection.Query q = new Connection.Query(contactsBase());
        opts.apply(q);

        ArrayList<Contact> contacts = new ArrayList<>();
        for (JSONObject json : opts.getItems(conn, getUserID(), q.toString(), null)){
            contacts.add(bind(json));
        }
        return contacts;
    }


  //**************************************************************************
  //** getContact
  //**************************************************************************
  /** Returns a single contact by its (mailbox-unique) id.
   */
    public Contact getContact(String id) throws GraphException {
        return bind(conn.get(contactPath(id)));
    }


  //**************************************************************************
  //** findByEmail
  //**************************************************************************
  /** Returns the contacts in this folder that have the given email address.
   */
    public List<Contact> findByEmail(String address) throws GraphException {
        if (address==null) return new ArrayList<>();
        String a = address.trim().toLowerCase().replace("'", "''");
        Connection.Query q = new Connection.Query(contactsBase());
        q.set("$filter", "emailAddresses/any(a:a/address eq '" + a + "')");
        ArrayList<Contact> contacts = new ArrayList<>();
        for (JSONObject json : conn.getList(q.toString(), null)) contacts.add(bind(json));
        return contacts;
    }


  //**************************************************************************
  //** createContact
  //**************************************************************************
  /** Creates the contact in this folder. The passed Contact is updated in place
   *  with the server copy and returned.
   */
    public Contact createContact(Contact contact) throws GraphException {
        JSONObject server = conn.post(contactsBase(), contact.toJson());
        contact.conn = conn;
        contact.replaceJson(server);
        contact.setResourcePath(resourcePath(server));
        return contact;
    }


  //**************************************************************************
  //** updateContact
  //**************************************************************************
  /** PATCHes the contact's pending changes and returns it updated with the
   *  server copy. Read-only keys are stripped from the body. The write is
   *  unconditional (last-writer-wins) by default; use
   *  updateContact(Contact,boolean) to opt into optimistic concurrency.
   */
    public Contact updateContact(Contact contact) throws GraphException {
        return updateContact(contact, false);
    }


  //**************************************************************************
  //** updateContact
  //**************************************************************************
  /** @param useIfMatch when true, sends <code>If-Match: changeKey</code> (412 on
   *  a server-side change since load).
   */
    public Contact updateContact(Contact contact, boolean useIfMatch) throws GraphException {
        String id = contact.getID();
        if (id==null) throw new GraphException("Cannot update a contact without an id; use createContact");

        Map<String, String> headers = new LinkedHashMap<>();
        if (useIfMatch && contact.getChangeKey()!=null) headers.put("If-Match", contact.getChangeKey());

        JSONObject server = conn.execute("PATCH", contactPath(id), contact.writableChanges(), headers).toJson();
        contact.conn = conn;
        if (server!=null && !server.get("id").isNull()){
            contact.replaceJson(server);
            contact.setResourcePath(resourcePath(server));
        }
        else{
            contact.setResourcePath(contactPath(id));
            contact.resetChanges();
        }
        return contact;
    }


  //**************************************************************************
  //** deleteContact
  //**************************************************************************
  /** Deletes the contact with the given id from the mailbox. A null id is a
   *  no-op.
   */
    public void deleteContact(String id) throws GraphException {
        if (id==null) return;
        conn.delete(contactPath(id));
    }


  //**************************************************************************
  //** getContactsDelta
  //**************************************************************************
  /** Incremental sync of this folder's contacts. Pass a null
   *  <code>deltaLink</code> for the initial sync; pass a previously returned
   *  delta link for subsequent syncs. Removed contacts are reported by id.
   */
    public ContactSync getContactsDelta(String deltaLink) throws GraphException {
        String url = (deltaLink!=null) ? deltaLink : contactsBase() + "/delta";
        Connection.Delta delta = conn.getDelta(url, null);
        ArrayList<Contact> items = new ArrayList<>();
        ArrayList<String> removed = new ArrayList<>();
        for (JSONObject json : delta.getItems()){
            if (Connection.Delta.isRemoved(json)){
                String id = json.get("id").isNull() ? null : json.get("id").toString();
                if (id!=null) removed.add(id);
            }
            else{
                items.add(bind(json));
            }
        }
        return new ContactSync(items, removed, delta.getDeltaLink());
    }


  //**************************************************************************
  //** getChildFolders
  //**************************************************************************
  /** Returns the child contact folders of this folder.
   */
    public List<ContactFolder> getChildFolders() throws GraphException {
        String url = (getID()==null)
            ? "/users/" + getUserID() + "/contactFolders"
            : "/users/" + getUserID() + "/contactFolders/" + getID() + "/childFolders";
        ArrayList<ContactFolder> list = new ArrayList<>();
        for (JSONObject json : conn.getList(url, null)) list.add(new ContactFolder(json, conn));
        return list;
    }


  //**************************************************************************
  //** contactsBase
  //**************************************************************************
  /** Returns the base path for this folder's contacts collection.
   */
    private String contactsBase(){
        return (getID()==null)
            ? "/users/" + getUserID() + "/contacts"
            : "/users/" + getUserID() + "/contactFolders/" + getID() + "/contacts";
    }


  //**************************************************************************
  //** contactPath
  //**************************************************************************
  /** Returns the resource path for a single contact by id.
   */
    private String contactPath(String id){
        return "/users/" + getUserID() + "/contacts/" + id;
    }


  //**************************************************************************
  //** bind
  //**************************************************************************
  /** Wraps a contact JSON object in a Contact bound to this connection.
   */
    private Contact bind(JSONObject json){
        Contact c = new Contact(json, conn);
        c.setResourcePath(resourcePath(json));
        return c;
    }


  //**************************************************************************
  //** resourcePath
  //**************************************************************************
  /** Returns the resource path derived from a contact JSON object's id, or null.
   */
    private String resourcePath(JSONObject json){
        if (json==null || json.get("id").isNull()) return null;
        return contactPath(json.get("id").toString());
    }



  //**************************************************************************
  //** ContactQuery Class
  //**************************************************************************
  /** Options for contact queries (see ItemQuery for the common
   *  options), plus <code>$skip</code>, a limit on the number of contacts
   *  returned, and <code>$search</code>. Setters chain.
   */
    public static class ContactQuery extends ItemQuery<ContactQuery> {


      /** Sets the number of contacts to skip (<code>$skip</code>). Use with
       *  setTop() and setLimit() to fetch a single page at an offset. Ignored
       *  when searching (not supported by Graph).
       */
        public ContactQuery setSkip(Integer skip){ return super.setSkip(skip); }


      /** Sets the maximum number of contacts to return in total (paging stops
       *  once it's reached). Null for no limit (the default).
       */
        public ContactQuery setLimit(Integer limit){ return super.setLimit(limit); }


      /** Sets the text to search for (<code>$search</code>, KQL: matches the
       *  start of any word in the name, company, title, email addresses, and
       *  phone numbers). The text is quoted and escaped. The filter, orderby,
       *  and skip options are ignored while searching (Graph doesn't allow
       *  $orderby or $skip with $search). If the connection uses immutable
       *  ids (the default), the results use immutable ids too (see
       *  ItemQuery).
       */
        public ContactQuery setSearch(String search){ return super.setSearch(search); }
    }


  //**************************************************************************
  //** ContactSync Class
  //**************************************************************************
  /** Result of an incremental contact sync: added/changed contacts, ids of
   *  removed contacts, and the delta link to persist for the next sync.
   */
    public static class ContactSync {

        private final List<Contact> contacts;
        private final List<String> removedIds;
        private final String deltaLink;

        ContactSync(List<Contact> contacts, List<String> removedIds, String deltaLink){
            this.contacts = contacts;
            this.removedIds = removedIds;
            this.deltaLink = deltaLink;
        }


      /** Returns the contacts added or changed since the last sync.
       */
        public List<Contact> getContacts(){ return contacts; }


      /** Returns the ids of contacts removed since the last sync.
       */
        public List<String> getRemovedIds(){ return removedIds; }


      /** Returns the delta link to persist and pass to the next sync.
       */
        public String getDeltaLink(){ return deltaLink; }
    }
}
