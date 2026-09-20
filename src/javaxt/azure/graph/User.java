package javaxt.azure.graph;
import java.util.*;
import javaxt.json.*;

//******************************************************************************
//**  User
//******************************************************************************
/**
 *   Represents a Microsoft Graph user (mailbox owner) and is the entry point
 *   for that user's calendars.
 *
 ******************************************************************************/

public class User extends Node {

    private static final String USER_SELECT = "id,mail,userPrincipalName,displayName";


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public User(JSONObject json, Connection conn){
        super(json, conn);
    }


  //**************************************************************************
  //** Metadata
  //**************************************************************************
    public String getEmail(){
        return get("mail").isNull() ? null : get("mail").toString();
    }

    public String getUserPrincipalName(){
        return get("userPrincipalName").isNull() ? null : get("userPrincipalName").toString();
    }

    public String getDisplayName(){
        return get("displayName").isNull() ? null : get("displayName").toString();
    }


  //**************************************************************************
  //** getUser
  //**************************************************************************
  /** Looks up a single user by id or user principal name (UPN). */
    public static User getUser(String idOrUPN, Connection conn) throws GraphException {
        Connection.Query qb = new Connection.Query("/users/" + idOrUPN);
        qb.set("$select", USER_SELECT);
        return new User(conn.get(qb.toString()), conn);
    }


  //**************************************************************************
  //** getUsers
  //**************************************************************************
  /** Returns all users in the tenant, following paging (no silent truncation). */
    public static List<User> getUsers(Connection conn) throws GraphException {
        Connection.Query qb = new Connection.Query("/users");
        qb.set("$select", USER_SELECT);
        ArrayList<User> users = new ArrayList<>();
        for (JSONObject json : conn.getList(qb.toString(), null)){
            users.add(new User(json, conn));
        }
        return users;
    }


  //**************************************************************************
  //** findByEmail
  //**************************************************************************
  /** Finds a user by primary email (<code>mail</code>) or, failing that, by any
   *  proxy address. Returns null if no match is found.
   */
    public static User findByEmail(String email, Connection conn) throws GraphException {
        if (email==null) return null;
        String addr = email.trim().toLowerCase();

        Connection.Query qb = new Connection.Query("/users");
        qb.set("$select", USER_SELECT);
        qb.set("$filter", "mail eq '" + escape(addr) + "'");
        List<JSONObject> matches = conn.getList(qb.toString(), null);
        if (!matches.isEmpty()) return new User(matches.get(0), conn);

      //Fall back to proxyAddresses (requires ConsistencyLevel: eventual + $count)
        Connection.Query qb2 = new Connection.Query("/users");
        qb2.set("$select", USER_SELECT);
        qb2.set("$filter", "proxyAddresses/any(p:p eq 'smtp:" + escape(addr) + "')");
        qb2.set("$count", "true");
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("ConsistencyLevel", "eventual");
        List<JSONObject> matches2 = conn.getList(qb2.toString(), headers);
        if (!matches2.isEmpty()) return new User(matches2.get(0), conn);

        return null;
    }


  //**************************************************************************
  //** getCalendars
  //**************************************************************************
  /** Returns all of the user's calendars (including shared calendars). */
    public List<Calendar> getCalendars() throws GraphException {
        return getCalendars(false);
    }


  //**************************************************************************
  //** getCalendars
  //**************************************************************************
  /** Returns the user's calendars.
   *  @param ownedOnly when true, keeps only calendars whose owner matches the
   *  user's mail (dropping shared calendars).
   */
    public List<Calendar> getCalendars(boolean ownedOnly) throws GraphException {
        String userID = getID();
        String email = getEmail();
        ArrayList<Calendar> calendars = new ArrayList<>();
        for (JSONObject json : conn.getList("/users/" + userID + "/calendars", null)){
            Calendar calendar = new Calendar(json, userID, conn);
            if (ownedOnly){
                EmailAddress owner = calendar.getOwner();
                if (owner==null || email==null || !owner.getAddress().equalsIgnoreCase(email)) continue;
            }
            calendars.add(calendar);
        }
        return calendars;
    }


  //**************************************************************************
  //** getDefaultCalendar
  //**************************************************************************
    public Calendar getDefaultCalendar() throws GraphException {
        String userID = getID();
        return new Calendar(conn.get("/users/" + userID + "/calendar"), userID, conn);
    }


  //**************************************************************************
  //** getMailboxTimeZone
  //**************************************************************************
  /** Returns the mailbox's configured time zone (requires MailboxSettings.Read). */
    public String getMailboxTimeZone() throws GraphException {
        JSONObject json = conn.get("/users/" + getID() + "/mailboxSettings/timeZone");
        return json.get("value").isNull() ? null : json.get("value").toString();
    }


  //**************************************************************************
  //** getDefaultContactFolder
  //**************************************************************************
  /** Returns the mailbox's default contact folder
   *  (<code>/users/{id}/contacts</code>).
   */
    public ContactFolder getDefaultContactFolder(){
        JSONObject json = new JSONObject();
        json.set("displayName", "Contacts");
        return new ContactFolder(json, getID(), conn);
    }


  //**************************************************************************
  //** getContactFolders
  //**************************************************************************
  /** Returns the user-created contact folders (the default folder's children). */
    public List<ContactFolder> getContactFolders() throws GraphException {
        String userID = getID();
        ArrayList<ContactFolder> list = new ArrayList<>();
        for (JSONObject json : conn.getList("/users/" + userID + "/contactFolders", null)){
            list.add(new ContactFolder(json, userID, conn));
        }
        return list;
    }


  //**************************************************************************
  //** getContactFolder
  //**************************************************************************
  /** Returns the named contact folder, or null if none matches. */
    public ContactFolder getContactFolder(String displayName) throws GraphException {
        String userID = getID();
        Connection.Query q = new Connection.Query("/users/" + userID + "/contactFolders");
        q.set("$filter", "displayName eq '" + escape(displayName) + "'");
        List<JSONObject> matches = conn.getList(q.toString(), null);
        return matches.isEmpty() ? null : new ContactFolder(matches.get(0), userID, conn);
    }


  //**************************************************************************
  //** createContactFolder
  //**************************************************************************
    public ContactFolder createContactFolder(String displayName) throws GraphException {
        String userID = getID();
        JSONObject payload = new JSONObject();
        payload.set("displayName", displayName);
        JSONObject server = conn.post("/users/" + userID + "/contactFolders", payload);
        return new ContactFolder(server, userID, conn);
    }


  //**************************************************************************
  //** sendMail
  //**************************************************************************
  /** Sends a message directly (POST /users/{id}/sendMail).
   *  @param saveToSentItems whether to keep a copy in Sent Items.
   */
    public void sendMail(Email message, boolean saveToSentItems) throws GraphException {
        JSONObject payload = new JSONObject();
        payload.set("message", message.toJson());
        payload.set("saveToSentItems", saveToSentItems);
        conn.post("/users/" + getID() + "/sendMail", payload);
    }


  //**************************************************************************
  //** createDraft
  //**************************************************************************
  /** Creates a draft message in the mailbox and returns it (bound so it can be
   *  edited, have attachments added, and sent).
   */
    public Email createDraft(Email message) throws GraphException {
        String userID = getID();
        JSONObject server = conn.post("/users/" + userID + "/messages", message.toJson());
        message.conn = conn;
        message.replaceJson(server);
        if (!server.get("id").isNull()){
            message.setResourcePath("/users/" + userID + "/messages/" + server.get("id").toString());
        }
        return message;
    }


  //**************************************************************************
  //** getMailFolders
  //**************************************************************************
  /** Returns the top-level mail folders. Use {@link EmailFolder#getChildFolders()}
   *  to descend the hierarchy.
   */
    public List<EmailFolder> getMailFolders() throws GraphException {
        String userID = getID();
        ArrayList<EmailFolder> list = new ArrayList<>();
        for (JSONObject json : conn.getList("/users/" + userID + "/mailFolders", null)){
            list.add(new EmailFolder(json, userID, conn));
        }
        return list;
    }


  //**************************************************************************
  //** getMailFolder
  //**************************************************************************
  /** Returns a mail folder by well-known name (inbox, drafts, sentitems,
   *  deleteditems, junkemail, archive, ...) or by id.
   */
    public EmailFolder getMailFolder(String wellKnownNameOrId) throws GraphException {
        String userID = getID();
        return new EmailFolder(conn.get("/users/" + userID + "/mailFolders/" + wellKnownNameOrId), userID, conn);
    }


  //**************************************************************************
  //** getMessage
  //**************************************************************************
    public Email getMessage(String id) throws GraphException {
        String userID = getID();
        Email m = new Email(conn.get("/users/" + userID + "/messages/" + id), conn);
        m.setResourcePath("/users/" + userID + "/messages/" + id);
        return m;
    }


  //**************************************************************************
  //** escape
  //**************************************************************************
  /** Escapes single quotes in an OData string literal. */
    private static String escape(String s){
        return s.replace("'", "''");
    }
}
