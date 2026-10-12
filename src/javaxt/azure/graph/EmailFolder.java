package javaxt.azure.graph;
import java.util.*;
import javaxt.json.*;

//******************************************************************************
//**  EmailFolder
//******************************************************************************
/**
 *   Represents a Microsoft Graph mail folder. Well-known folders (inbox,
 *   drafts, sentitems, deleteditems, junkemail, archive) are obtained via the
 *   User.getMailFolder(String) method. Messages are addressed by their
 *  (mailbox-unique) id regardless of folder.
 *
 ******************************************************************************/

public class EmailFolder extends Folder {

  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Wraps a Graph mailFolder resource, bound to a user-scoped connection (see
   *  {@link Connection#forUser}).
   */
    public EmailFolder(JSONObject json, Connection conn){
        super(json, conn);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Binds to a mail folder by well-known name (inbox, drafts, sentitems,
   *  deleteditems, junkemail, archive, ...) or id, on the given user-scoped
   *  connection. Folder metadata (counts) is not loaded until fetched.
   */
    public EmailFolder(String wellKnownNameOrId, Connection conn){
        super(wellKnownNameOrId, conn);
    }


  //**************************************************************************
  //** getTotalItemCount
  //**************************************************************************
  /** Returns the total number of messages in this folder, or null if unknown.
   */
    public Integer getTotalItemCount(){
        return get("totalItemCount").isNull() ? null : get("totalItemCount").toInteger();
    }


  //**************************************************************************
  //** getUnreadItemCount
  //**************************************************************************
  /** Returns the number of unread messages in this folder, or null if unknown.
   */
    public Integer getUnreadItemCount(){
        return get("unreadItemCount").isNull() ? null : get("unreadItemCount").toInteger();
    }


  //**************************************************************************
  //** getEmails
  //**************************************************************************
  /** Returns all messages in this folder using default query options.
   */
    public List<Email> getEmails() throws GraphException {
        return getEmails(new EmailQuery());
    }


  //**************************************************************************
  //** getEmails
  //**************************************************************************
  /** Returns the messages in this folder. Pages lazily and stops once the
   *  query's limit (if any) is reached, so listing a large mailbox never
   *  pages the whole folder unintentionally.
   */
    public List<Email> getEmails(EmailQuery opts) throws GraphException {
        if (opts==null) opts = new EmailQuery();

        Connection.Query q = new Connection.Query(messagesBase());
        opts.apply(q);

        ArrayList<Email> messages = new ArrayList<>();
        for (JSONObject json : opts.getItems(conn, getUserID(), q.toString(), null)){
            messages.add(bind(json));
        }
        return messages;
    }


  //**************************************************************************
  //** getEmailsDelta
  //**************************************************************************
  /** Incremental sync of this folder's messages. Pass a null
   *  <code>deltaLink</code> for the initial sync; pass a previously returned
   *  delta link for subsequent syncs. Removed messages are reported by id.
   */
    public EmailSync getEmailsDelta(String deltaLink) throws GraphException {
        String url = (deltaLink!=null) ? deltaLink : messagesBase() + "/delta";
        Connection.Delta delta = conn.getDelta(url, null);
        ArrayList<Email> items = new ArrayList<>();
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
        return new EmailSync(items, removed, delta.getDeltaLink());
    }


  //**************************************************************************
  //** getChildFolders
  //**************************************************************************
  /** Returns the immediate child folders of this mail folder.
   */
    public List<EmailFolder> getChildFolders() throws GraphException {
        String url = "/users/" + getUserID() + "/mailFolders/" + getID() + "/childFolders";
        ArrayList<EmailFolder> list = new ArrayList<>();
        for (JSONObject json : conn.getList(url, null)) list.add(new EmailFolder(json, conn));
        return list;
    }


  //**************************************************************************
  //** messagesBase
  //**************************************************************************
  /** Returns the base messages URL for this folder.
   */
    private String messagesBase(){
        return "/users/" + getUserID() + "/mailFolders/" + getID() + "/messages";
    }


  //**************************************************************************
  //** bind
  //**************************************************************************
  /** Wraps a Graph message JSON object in an Email and sets its resource path.
   */
    private Email bind(JSONObject json){
        Email m = new Email(json, conn);
        if (!json.get("id").isNull()){
            m.setResourcePath("/users/" + getUserID() + "/messages/" + json.get("id").toString());
        }
        return m;
    }


  //**************************************************************************
  //** EmailQuery Class
  //**************************************************************************
  /** Used to set options for message queries (see ItemQuery for the
   *  common options). Example: <pre>setSkip(250).setTop(50).setLimit(50)</pre>
   *  for messages 251-300.
   */
    public static class EmailQuery extends ItemQuery<EmailQuery> {


      /** Used to set the number of messages to skip before the first message
       *  is returned. Use with setTop() and setLimit() to fetch a single page
       *  at an arbitrary offset without downloading the messages before it.
       *  Ignored when searching.
       */
        public EmailQuery setSkip(Integer skip){ return super.setSkip(skip); }


      /** Used to set the maximum number of messages to return in total. Null
       *  value implies no limit.
       */
        public EmailQuery setLimit(Integer limit){ return super.setLimit(limit); }


      /** Used for full-text search (KQL, matches the start of words in the
       *  sender, recipients, subject, and body). The text is quoted and
       *  escaped. Note that the Graph disables filter, orderby, skip, and
       *  count while searching. If the connection uses immutable ids (the
       *  default), the results use immutable ids too (see ItemQuery).
       */
        public EmailQuery setSearch(String search){ return super.setSearch(search); }
    }


  //**************************************************************************
  //** EmailSync Class
  //**************************************************************************
  /** Result of an incremental message sync: added/changed messages, ids of
   *  removed messages, and the delta link to persist for the next sync.
   */
    public static class EmailSync {

        private final List<Email> messages;
        private final List<String> removedIds;
        private final String deltaLink;

        EmailSync(List<Email> messages, List<String> removedIds, String deltaLink){
            this.messages = messages;
            this.removedIds = removedIds;
            this.deltaLink = deltaLink;
        }


      /** Returns the messages added or changed since the previous sync.
       */
        public List<Email> getEmails(){ return messages; }


      /** Returns the ids of messages removed since the previous sync.
       */
        public List<String> getRemovedIds(){ return removedIds; }


      /** Returns the delta link to persist and pass to the next sync.
       */
        public String getDeltaLink(){ return deltaLink; }
    }
}
