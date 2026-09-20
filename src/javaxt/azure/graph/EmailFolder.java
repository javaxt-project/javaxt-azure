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
    public EmailFolder(JSONObject json, String userID, Connection conn){
        super(json, userID, conn);
    }


  //**************************************************************************
  //** counts
  //**************************************************************************
    public Integer getTotalItemCount(){
        return get("totalItemCount").isNull() ? null : get("totalItemCount").toInteger();
    }

    public Integer getUnreadItemCount(){
        return get("unreadItemCount").isNull() ? null : get("unreadItemCount").toInteger();
    }


  //**************************************************************************
  //** getEmails
  //**************************************************************************
    public List<Email> getEmails() throws GraphException {
        return getEmails(new EmailQuery());
    }

  /** Returns the messages in this folder, following paging to the end. */
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
    public List<EmailFolder> getChildFolders() throws GraphException {
        String url = "/users/" + userID + "/mailFolders/" + getID() + "/childFolders";
        ArrayList<EmailFolder> list = new ArrayList<>();
        for (JSONObject json : conn.getList(url, null)) list.add(new EmailFolder(json, userID, conn));
        return list;
    }


  //**************************************************************************
  //** path helpers
  //**************************************************************************
    private String messagesBase(){
        return "/users/" + userID + "/mailFolders/" + getID() + "/messages";
    }

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

      /** Page size ($top per request). Default 50. */
        public EmailQuery setTop(Integer top){ this.top = top; return this; }

      /** Maximum number of messages to return in total; paging stops once
       *  reached (so a large folder is never fully paged). Null means no limit.
       */
        public EmailQuery setLimit(Integer limit){ this.limit = limit; return this; }

        public EmailQuery select(String... properties){
            if (properties!=null) for (String p : properties) if (p!=null) select.add(p);
            return this;
        }

        public EmailQuery expandExtension(String... extensionNames){
            if (extensionNames!=null) for (String n : extensionNames) if (n!=null) expandExtensions.add(n);
            return this;
        }

        public EmailQuery setFilter(String filter){ this.filter = filter; return this; }
        public EmailQuery setOrderBy(String orderBy){ this.orderBy = orderBy; return this; }

      /** Full-text search. Note: Graph disables $orderby/$count while searching. */
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

        public List<Email> getEmails(){ return messages; }
        public List<String> getRemovedIds(){ return removedIds; }
        public String getDeltaLink(){ return deltaLink; }
    }
}
