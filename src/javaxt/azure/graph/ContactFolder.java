package javaxt.azure.graph;
import java.util.*;
import javaxt.json.*;

//******************************************************************************
//**  ContactFolder
//******************************************************************************
/**
 *   Represents a Microsoft Graph contact folder and the operations on the
 *   contacts it holds. The mailbox's default folder (obtained via
 *   {@link User#getDefaultContactFolder()}) reads/writes contacts under
 *   <code>/users/{id}/contacts</code>; named folders use
 *   <code>/users/{id}/contactFolders/{folderId}/contacts</code>. Individual
 *   contacts are addressed by their (mailbox-unique) id regardless of folder.
 *
 ******************************************************************************/

public class ContactFolder extends Folder {

  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Wraps a Graph contact folder for the given mailbox (userID), bound to the
   *  connection used to reach it.
   */
    public ContactFolder(JSONObject json, String userID, Connection conn){
        super(json, userID, conn);
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
  /** Returns the contacts in this folder, following paging to the end.
   */
    public List<Contact> getContacts(ContactQuery opts) throws GraphException {
        if (opts==null) opts = new ContactQuery();

        Connection.Query q = new Connection.Query(contactsBase());
        q.set("$top", opts.top!=null ? opts.top : 50);
        if (!opts.select.isEmpty()) q.set("$select", String.join(",", opts.select));
        if (opts.filter!=null) q.set("$filter", opts.filter);
        if (opts.orderBy!=null) q.set("$orderby", opts.orderBy);
        String expand = Item.expandExtensions(opts.expandExtensions);
        if (expand!=null) q.set("$expand", expand);

        ArrayList<Contact> contacts = new ArrayList<>();
        for (JSONObject json : conn.getList(q.toString(), null)) contacts.add(bind(json));
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
   *  {@link #updateContact(Contact,boolean)} to opt into optimistic concurrency.
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
            ? "/users/" + userID + "/contactFolders"
            : "/users/" + userID + "/contactFolders/" + getID() + "/childFolders";
        ArrayList<ContactFolder> list = new ArrayList<>();
        for (JSONObject json : conn.getList(url, null)) list.add(new ContactFolder(json, userID, conn));
        return list;
    }


  //**************************************************************************
  //** contactsBase
  //**************************************************************************
  /** Returns the base path for this folder's contacts collection.
   */
    private String contactsBase(){
        return (getID()==null)
            ? "/users/" + userID + "/contacts"
            : "/users/" + userID + "/contactFolders/" + getID() + "/contacts";
    }


  //**************************************************************************
  //** contactPath
  //**************************************************************************
  /** Returns the resource path for a single contact by id.
   */
    private String contactPath(String id){
        return "/users/" + userID + "/contacts/" + id;
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
  /** Options for contact queries: <code>$select</code>, open-extension
   *  <code>$expand</code>, page size (<code>$top</code>), <code>$filter</code>
   *  and <code>$orderby</code>. Setters chain.
   */
    public static class ContactQuery {

        private Integer top;
        private final List<String> select = new ArrayList<>();
        private final List<String> expandExtensions = new ArrayList<>();
        private String filter;
        private String orderBy;


      //**********************************************************************
      //** setTop
      //**********************************************************************
      /** Sets the page size (<code>$top</code>) for the query. Returns this query for chaining.
       */
        public ContactQuery setTop(Integer top){ this.top = top; return this; }


      //**********************************************************************
      //** select
      //**********************************************************************
      /** Adds one or more properties to the <code>$select</code> list. Returns this query for chaining.
       */
        public ContactQuery select(String... properties){
            if (properties!=null) for (String p : properties) if (p!=null) select.add(p);
            return this;
        }


      //**********************************************************************
      //** expandExtension
      //**********************************************************************
      /** Adds one or more open-extension names to <code>$expand</code>. Returns this query for chaining.
       */
        public ContactQuery expandExtension(String... extensionNames){
            if (extensionNames!=null) for (String n : extensionNames) if (n!=null) expandExtensions.add(n);
            return this;
        }


      //**********************************************************************
      //** setFilter
      //**********************************************************************
      /** Sets the <code>$filter</code> expression for the query. Returns this query for chaining.
       */
        public ContactQuery setFilter(String filter){ this.filter = filter; return this; }


      //**********************************************************************
      //** setOrderBy
      //**********************************************************************
      /** Sets the <code>$orderby</code> expression for the query. Returns this query for chaining.
       */
        public ContactQuery setOrderBy(String orderBy){ this.orderBy = orderBy; return this; }
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


      //**********************************************************************
      //** getContacts
      //**********************************************************************
      /** Returns the contacts added or changed since the last sync.
       */
        public List<Contact> getContacts(){ return contacts; }


      //**********************************************************************
      //** getRemovedIds
      //**********************************************************************
      /** Returns the ids of contacts removed since the last sync.
       */
        public List<String> getRemovedIds(){ return removedIds; }


      //**********************************************************************
      //** getDeltaLink
      //**********************************************************************
      /** Returns the delta link to persist and pass to the next sync.
       */
        public String getDeltaLink(){ return deltaLink; }
    }
}
