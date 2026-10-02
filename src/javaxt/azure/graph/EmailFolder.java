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
            if (opts.skip!=null && opts.skip>0) q.set("$skip", opts.skip);
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
  /** Used to set options for message queries. Example:
   *  <pre>setSkip(250).setTop(50).setLimit(50)</pre> for messages 251-300.
   */
    public static class EmailQuery {

        private Integer top;
        private Integer skip;
        private Integer limit;
        private final List<String> select = new ArrayList<>();
        private final List<String> expandExtensions = new ArrayList<>();
        private String filter;
        private String orderBy;
        private String search;


      /** Used to set the page size. Default is 50 records.
       */
        public EmailQuery setTop(Integer top){ this.top = top; return this; }


      /** Used to set the number of messages to skip before the first message
       *  is returned. Use with setTop() and setLimit() to fetch a single page
       *  at an arbitrary offset without downloading the messages before it.
       */
        public EmailQuery setSkip(Integer skip){ this.skip = skip; return this; }


      /** Used to set the maximum number of messages to return in total. Null
       *  value implies no limit.
       */
        public EmailQuery setLimit(Integer limit){ this.limit = limit; return this; }


      /** Used to restrict the properties returned for each message.
       */
        public EmailQuery select(String... properties){
            if (properties!=null){
                for (String p : properties){
                    if (p!=null) select.add(p);
                }
            }
            return this;
        }


      /** Used to expand the named open extensions on each message.
       */
        public EmailQuery expandExtension(String... extensionNames){
            if (extensionNames!=null){
                for (String n : extensionNames){
                    if (n!=null) expandExtensions.add(n);
                }
            }
            return this;
        }


      /** Used to set an filter expression. Note that this is ignored when
       *  search is set.
       */
        public EmailQuery setFilter(String filter){ this.filter = filter; return this; }



      /** Used to set an orderby expression. Note that this is ignored when
       *  search is set.
       */
        public EmailQuery setOrderBy(String orderBy){ this.orderBy = orderBy; return this; }


      /** Used for full-text search. Note that the Graph disables orderby and
       *  count while searching.
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
