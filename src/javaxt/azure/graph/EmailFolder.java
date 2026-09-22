package javaxt.azure.graph;
import java.util.*;
import javaxt.json.*;

//******************************************************************************
//**  EmailFolder
//******************************************************************************
/**
 *   Represents a Microsoft Graph mail folder and the operations on the messages
 *   it holds. Well-known folders (inbox, drafts, sentitems, deleteditems,
 *   junkemail, archive) are obtained via {@link User#getMailFolder(String)};
 *   messages are addressed by their (mailbox-unique) id regardless of folder.
 *
 ******************************************************************************/

public class EmailFolder extends Folder {

  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a mail folder wrapping a Graph mailFolder resource for the given
   *  user.
   */
    public EmailFolder(JSONObject json, String userID, Connection conn){
        super(json, userID, conn);
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
  /** Returns the messages in this folder, following paging to the end.
   */
    public List<Email> getEmails(EmailQuery opts) throws GraphException {
        if (opts==null) opts = new EmailQuery();

        Connection.Query q = new Connection.Query(messagesBase());
        q.set("$top", opts.top!=null ? opts.top : 50);
        if (!opts.select.isEmpty()) q.set("$select", String.join(",", opts.select));
        if (opts.search!=null){
            //$search disables $orderby and $count
            q.set("$search", "\"" + opts.search + "\"");
        }
        else{
            if (opts.filter!=null) q.set("$filter", opts.filter);
            if (opts.orderBy!=null) q.set("$orderby", opts.orderBy);
        }
        String expand = Item.expandExtensions(opts.expandExtensions);
        if (expand!=null) q.set("$expand", expand);

      //Page lazily and stop once the requested limit is reached, so listing a
      //large mailbox never pages the whole folder unintentionally.
        int limit = (opts.limit==null) ? Integer.MAX_VALUE : opts.limit;
        ArrayList<Email> messages = new ArrayList<>();
        try{
            for (JSONObject json : conn.getPage(q.toString(), null)){
                messages.add(bind(json));
                if (messages.size() >= limit) break;
            }
        }
        catch(RuntimeException e){
            if (e.getCause() instanceof GraphException) throw (GraphException) e.getCause();
            throw e;
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
        String url = "/users/" + userID + "/mailFolders/" + getID() + "/childFolders";
        ArrayList<EmailFolder> list = new ArrayList<>();
        for (JSONObject json : conn.getList(url, null)) list.add(new EmailFolder(json, userID, conn));
        return list;
    }


  //**************************************************************************
  //** messagesBase
  //**************************************************************************
  /** Returns the base messages URL for this folder.
   */
    private String messagesBase(){
        return "/users/" + userID + "/mailFolders/" + getID() + "/messages";
    }


  //**************************************************************************
  //** bind
  //**************************************************************************
  /** Wraps a Graph message JSON object in an Email and sets its resource path.
   */
    private Email bind(JSONObject json){
        Email m = new Email(json, conn);
        if (!json.get("id").isNull()){
            m.setResourcePath("/users/" + userID + "/messages/" + json.get("id").toString());
        }
        return m;
    }


  //**************************************************************************
  //** EmailQuery Class
  //**************************************************************************
  /** Options for message queries: <code>$select</code>, open-extension
   *  <code>$expand</code>, page size (<code>$top</code>), and either
   *  <code>$filter</code>/<code>$orderby</code> or a full-text
   *  <code>$search</code> (which disables filter/orderby). Setters chain.
   */
    public static class EmailQuery {

        private Integer top;
        private Integer limit;
        private final List<String> select = new ArrayList<>();
        private final List<String> expandExtensions = new ArrayList<>();
        private String filter;
        private String orderBy;
        private String search;


      //**************************************************************************
      //** setTop
      //**************************************************************************
      /** Page size ($top per request). Default 50.
       */
        public EmailQuery setTop(Integer top){ this.top = top; return this; }


      //**************************************************************************
      //** setLimit
      //**************************************************************************
      /** Maximum number of messages to return in total; paging stops once
       *  reached (so a large folder is never fully paged). Null means no limit.
       */
        public EmailQuery setLimit(Integer limit){ this.limit = limit; return this; }


      //**************************************************************************
      //** select
      //**************************************************************************
      /** Restricts the properties returned for each message ($select).
       */
        public EmailQuery select(String... properties){
            if (properties!=null) for (String p : properties) if (p!=null) select.add(p);
            return this;
        }


      //**************************************************************************
      //** expandExtension
      //**************************************************************************
      /** Expands the named open extensions on each message ($expand).
       */
        public EmailQuery expandExtension(String... extensionNames){
            if (extensionNames!=null) for (String n : extensionNames) if (n!=null) expandExtensions.add(n);
            return this;
        }


      //**************************************************************************
      //** setFilter
      //**************************************************************************
      /** Sets an OData $filter expression (ignored when $search is set).
       */
        public EmailQuery setFilter(String filter){ this.filter = filter; return this; }


      //**************************************************************************
      //** setOrderBy
      //**************************************************************************
      /** Sets an OData $orderby expression (ignored when $search is set).
       */
        public EmailQuery setOrderBy(String orderBy){ this.orderBy = orderBy; return this; }


      //**************************************************************************
      //** setSearch
      //**************************************************************************
      /** Full-text search. Note: Graph disables $orderby/$count while searching.
       */
        public EmailQuery setSearch(String search){ this.search = search; return this; }
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


      //**************************************************************************
      //** getEmails
      //**************************************************************************
      /** Returns the messages added or changed since the previous sync.
       */
        public List<Email> getEmails(){ return messages; }


      //**************************************************************************
      //** getRemovedIds
      //**************************************************************************
      /** Returns the ids of messages removed since the previous sync.
       */
        public List<String> getRemovedIds(){ return removedIds; }


      //**************************************************************************
      //** getDeltaLink
      //**************************************************************************
      /** Returns the delta link to persist and pass to the next sync.
       */
        public String getDeltaLink(){ return deltaLink; }
    }
}
