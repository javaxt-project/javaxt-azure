package javaxt.azure.graph;
import java.util.*;
import javaxt.json.*;

//******************************************************************************
//**  Email
//******************************************************************************
/**
 *   Represents a Microsoft Graph mail message. Exposes Graph-native property
 *   access (via {@link Node}/{@link Item}) alongside typed accessors and the mail
 *   operations (send, reply, forward, move, delete, attachments). A new outgoing
 *   message is built with the no-arg constructor and passed to
 *   {@link User#sendMail} or {@link User#createDraft}:
 *   <pre>
 *   Email m = new Email();
 *   m.setSubject("Appointment reminder");
 *   m.setTextBody("See you tomorrow at 10am.");
 *   m.addToRecipient(new EmailAddress("patient@example.com"));
 *   user.sendMail(m, true);
 *   </pre>
 *
 ******************************************************************************/

public class Email extends Item {

  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Email(){
        super(new JSONObject(), null);
    }

    public Email(JSONObject json, Connection conn){
        super(json, conn);
    }


  //**************************************************************************
  //** Subject / Body
  //**************************************************************************
    public String getSubject(){ return str("subject"); }
    public void setSubject(String subject){ set("subject", subject); }

    public String getBodyPreview(){ return str("bodyPreview"); }


  //**************************************************************************
  //** From / Sender
  //**************************************************************************
    public EmailAddress getFrom(){ return single("from"); }
    public EmailAddress getSender(){ return single("sender"); }


  //**************************************************************************
  //** Recipients
  //**************************************************************************
    public List<EmailAddress> getToRecipients(){ return recipients("toRecipients"); }
    public void setToRecipients(List<EmailAddress> to){ setRecipients("toRecipients", to); }
    public void addToRecipient(EmailAddress e){ addRecipient("toRecipients", e); }

    public List<EmailAddress> getCcRecipients(){ return recipients("ccRecipients"); }
    public void setCcRecipients(List<EmailAddress> cc){ setRecipients("ccRecipients", cc); }
    public void addCcRecipient(EmailAddress e){ addRecipient("ccRecipients", e); }

    public List<EmailAddress> getBccRecipients(){ return recipients("bccRecipients"); }
    public void setBccRecipients(List<EmailAddress> bcc){ setRecipients("bccRecipients", bcc); }
    public void addBccRecipient(EmailAddress e){ addRecipient("bccRecipients", e); }

    public List<EmailAddress> getReplyTo(){ return recipients("replyTo"); }


  //**************************************************************************
  //** Dates
  //**************************************************************************
    public javaxt.utils.Date getReceivedDateTime(){ return date("receivedDateTime"); }
    public javaxt.utils.Date getSentDateTime(){ return date("sentDateTime"); }


  //**************************************************************************
  //** Flags / metadata
  //**************************************************************************
    public boolean isRead(){ return !get("isRead").isNull() && get("isRead").toBoolean(); }
  /** Sets the local read flag (use {@link #markRead(boolean)} to persist it). */
    public void setRead(boolean read){ set("isRead", read); }

    public boolean isDraft(){ return !get("isDraft").isNull() && get("isDraft").toBoolean(); }

    public String getImportance(){ return str("importance"); }
    public void setImportance(String importance){ set("importance", importance); }

    public String getInternetMessageId(){ return str("internetMessageId"); }
    public String getConversationId(){ return str("conversationId"); }
    public String getParentFolderID(){ return str("parentFolderId"); }
    public String getWebLink(){ return str("webLink"); }

  /** Returns the raw followup flag object, or null. */
    public JSONObject getFlag(){
        return get("flag").isNull() ? null : get("flag").toJSONObject();
    }


  //**************************************************************************
  //** Operations
  //**************************************************************************
  /** Sends this draft message (POST /messages/{id}/send). */
    public void send() throws GraphException {
        requirePath();
        conn.post(resourcePath + "/send", null);
    }

  /** Creates a draft reply to the sender, returning the draft to edit/send. */
    public Email reply(String comment) throws GraphException {
        return draftFrom("createReply", comment, null);
    }

  /** Creates a draft reply to all recipients, returning the draft. */
    public Email replyAll(String comment) throws GraphException {
        return draftFrom("createReplyAll", comment, null);
    }

  /** Creates a draft forward to the given recipients, returning the draft. */
    public Email forward(String comment, List<EmailAddress> to) throws GraphException {
        JSONArray recips = new JSONArray();
        if (to!=null){
            for (EmailAddress e : to){
                if (e==null) continue;
                JSONObject r = new JSONObject();
                r.set("emailAddress", e.toJson());
                recips.add(r);
            }
        }
        return draftFrom("createForward", comment, recips);
    }

  /** Marks the message read/unread (PATCH isRead) and updates the local flag. */
    public void markRead(boolean read) throws GraphException {
        requirePath();
        JSONObject payload = new JSONObject();
        payload.set("isRead", read);
        conn.patch(resourcePath, payload);
        toJson().set("isRead", read);
        resetChanges();
    }

  /** Moves the message to another folder. Note: Graph assigns the moved copy a
   *  new id, so re-fetch if you need to operate on it further.
   */
    public void move(String destinationFolderId) throws GraphException {
        requirePath();
        JSONObject payload = new JSONObject();
        payload.set("destinationId", destinationFolderId);
        conn.post(resourcePath + "/move", payload);
    }

  /** Deletes the message (moves it to Deleted Items). */
    public void delete() throws GraphException {
        requirePath();
        conn.delete(resourcePath);
    }


  //**************************************************************************
  //** helpers
  //**************************************************************************
    private Email draftFrom(String action, String comment, JSONArray toRecipients) throws GraphException {
        requirePath();
        JSONObject payload = new JSONObject();
        if (comment!=null) payload.set("comment", comment);
        if (toRecipients!=null) payload.set("toRecipients", toRecipients);
        JSONObject server = conn.post(resourcePath + "/" + action, payload);
        Email draft = new Email(server, conn);
        if (!server.get("id").isNull()){
            draft.setResourcePath(resourcePath.substring(0, resourcePath.lastIndexOf("/messages/")) +
                "/messages/" + server.get("id").toString());
        }
        return draft;
    }

    private EmailAddress single(String key){
        if (get(key).isNull()) return null;
        JSONObject o = get(key).toJSONObject();
        if (o.get("emailAddress").isNull()) return null;
        try{ return new EmailAddress(o.get("emailAddress").toJSONObject()); }
        catch(Exception e){ return null; }
    }

    private List<EmailAddress> recipients(String key){
        ArrayList<EmailAddress> list = new ArrayList<>();
        if (!get(key).isNull()){
            for (JSONValue v : get(key).toJSONArray()){
                JSONObject o = v.toJSONObject();
                if (o.get("emailAddress").isNull()) continue;
                try{ list.add(new EmailAddress(o.get("emailAddress").toJSONObject())); }
                catch(Exception e){ /* skip malformed */ }
            }
        }
        return list;
    }

    private void setRecipients(String key, List<EmailAddress> emails){
        JSONArray arr = new JSONArray();
        if (emails!=null){
            for (EmailAddress e : emails){
                if (e==null) continue;
                JSONObject r = new JSONObject();
                r.set("emailAddress", e.toJson());
                arr.add(r);
            }
        }
        set(key, arr);
    }

    private void addRecipient(String key, EmailAddress e){
        if (e==null) return;
        JSONArray arr = get(key).isNull() ? new JSONArray() : get(key).toJSONArray();
        JSONObject r = new JSONObject();
        r.set("emailAddress", e.toJson());
        arr.add(r);
        set(key, arr);
    }

    private javaxt.utils.Date date(String key){
        return parseDate(str(key));
    }
}
