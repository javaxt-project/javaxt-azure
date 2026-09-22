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
  /** Creates a new, empty outgoing message to populate and send via
   *  {@link User#sendMail} or {@link User#createDraft}.
   */
    public Email(){
        super(new JSONObject(), null);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a message wrapping a Graph mail resource returned by the server.
   */
    public Email(JSONObject json, Connection conn){
        super(json, conn);
    }


  //**************************************************************************
  //** getSubject
  //**************************************************************************
  /** Returns the message subject.
   */
    public String getSubject(){ return str("subject"); }


  //**************************************************************************
  //** setSubject
  //**************************************************************************
  /** Sets the message subject.
   */
    public void setSubject(String subject){ set("subject", subject); }


  //**************************************************************************
  //** getBodyPreview
  //**************************************************************************
  /** Returns a short, text-only preview of the message body (Graph-generated).
   */
    public String getBodyPreview(){ return str("bodyPreview"); }


  //**************************************************************************
  //** getFrom
  //**************************************************************************
  /** Returns the author of the message (the "from" mailbox), or null.
   */
    public EmailAddress getFrom(){ return single("from"); }


  //**************************************************************************
  //** getSender
  //**************************************************************************
  /** Returns the account that actually sent the message (the "sender" mailbox,
   *  which may differ from "from" when sent on behalf of another), or null.
   */
    public EmailAddress getSender(){ return single("sender"); }


  //**************************************************************************
  //** getToRecipients
  //**************************************************************************
  /** Returns the To recipients of the message.
   */
    public List<EmailAddress> getToRecipients(){ return recipients("toRecipients"); }


  //**************************************************************************
  //** setToRecipients
  //**************************************************************************
  /** Replaces the To recipients of the message.
   */
    public void setToRecipients(List<EmailAddress> to){ setRecipients("toRecipients", to); }


  //**************************************************************************
  //** addToRecipient
  //**************************************************************************
  /** Adds a single address to the To recipients of the message.
   */
    public void addToRecipient(EmailAddress e){ addRecipient("toRecipients", e); }


  //**************************************************************************
  //** getCcRecipients
  //**************************************************************************
  /** Returns the Cc recipients of the message.
   */
    public List<EmailAddress> getCcRecipients(){ return recipients("ccRecipients"); }


  //**************************************************************************
  //** setCcRecipients
  //**************************************************************************
  /** Replaces the Cc recipients of the message.
   */
    public void setCcRecipients(List<EmailAddress> cc){ setRecipients("ccRecipients", cc); }


  //**************************************************************************
  //** addCcRecipient
  //**************************************************************************
  /** Adds a single address to the Cc recipients of the message.
   */
    public void addCcRecipient(EmailAddress e){ addRecipient("ccRecipients", e); }


  //**************************************************************************
  //** getBccRecipients
  //**************************************************************************
  /** Returns the Bcc recipients of the message.
   */
    public List<EmailAddress> getBccRecipients(){ return recipients("bccRecipients"); }


  //**************************************************************************
  //** setBccRecipients
  //**************************************************************************
  /** Replaces the Bcc recipients of the message.
   */
    public void setBccRecipients(List<EmailAddress> bcc){ setRecipients("bccRecipients", bcc); }


  //**************************************************************************
  //** addBccRecipient
  //**************************************************************************
  /** Adds a single address to the Bcc recipients of the message.
   */
    public void addBccRecipient(EmailAddress e){ addRecipient("bccRecipients", e); }


  //**************************************************************************
  //** getReplyTo
  //**************************************************************************
  /** Returns the Reply-To addresses suggested by the message author.
   */
    public List<EmailAddress> getReplyTo(){ return recipients("replyTo"); }


  //**************************************************************************
  //** getReceivedDateTime
  //**************************************************************************
  /** Returns the date and time the message was received, or null.
   */
    public javaxt.utils.Date getReceivedDateTime(){ return date("receivedDateTime"); }


  //**************************************************************************
  //** getSentDateTime
  //**************************************************************************
  /** Returns the date and time the message was sent, or null.
   */
    public javaxt.utils.Date getSentDateTime(){ return date("sentDateTime"); }


  //**************************************************************************
  //** isRead
  //**************************************************************************
  /** Returns true if the message has been read.
   */
    public boolean isRead(){ return !get("isRead").isNull() && get("isRead").toBoolean(); }


  //**************************************************************************
  //** setRead
  //**************************************************************************
  /** Sets the local read flag (use {@link #markRead(boolean)} to persist it).
   */
    public void setRead(boolean read){ set("isRead", read); }


  //**************************************************************************
  //** isDraft
  //**************************************************************************
  /** Returns true if the message is an unsent draft.
   */
    public boolean isDraft(){ return !get("isDraft").isNull() && get("isDraft").toBoolean(); }


  //**************************************************************************
  //** getImportance
  //**************************************************************************
  /** Returns the message importance ("low", "normal", or "high").
   */
    public String getImportance(){ return str("importance"); }


  //**************************************************************************
  //** setImportance
  //**************************************************************************
  /** Sets the message importance ("low", "normal", or "high").
   */
    public void setImportance(String importance){ set("importance", importance); }


  //**************************************************************************
  //** getInternetMessageId
  //**************************************************************************
  /** Returns the RFC 2822 Internet message id.
   */
    public String getInternetMessageId(){ return str("internetMessageId"); }


  //**************************************************************************
  //** getConversationId
  //**************************************************************************
  /** Returns the id of the conversation (thread) the message belongs to.
   */
    public String getConversationId(){ return str("conversationId"); }


  //**************************************************************************
  //** getParentFolderID
  //**************************************************************************
  /** Returns the id of the mail folder that currently holds the message.
   */
    public String getParentFolderID(){ return str("parentFolderId"); }


  //**************************************************************************
  //** getWebLink
  //**************************************************************************
  /** Returns a URL that opens the message in Outlook on the web.
   */
    public String getWebLink(){ return str("webLink"); }


  //**************************************************************************
  //** getFlag
  //**************************************************************************
  /** Returns the raw followup flag object, or null.
   */
    public JSONObject getFlag(){
        return get("flag").isNull() ? null : get("flag").toJSONObject();
    }


  //**************************************************************************
  //** send
  //**************************************************************************
  /** Sends this draft message (POST /messages/{id}/send).
   */
    public void send() throws GraphException {
        requirePath();
        conn.post(resourcePath + "/send", null);
    }


  //**************************************************************************
  //** reply
  //**************************************************************************
  /** Creates a draft reply to the sender, returning the draft to edit/send.
   */
    public Email reply(String comment) throws GraphException {
        return draftFrom("createReply", comment, null);
    }


  //**************************************************************************
  //** replyAll
  //**************************************************************************
  /** Creates a draft reply to all recipients, returning the draft.
   */
    public Email replyAll(String comment) throws GraphException {
        return draftFrom("createReplyAll", comment, null);
    }


  //**************************************************************************
  //** forward
  //**************************************************************************
  /** Creates a draft forward to the given recipients, returning the draft.
   */
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


  //**************************************************************************
  //** markRead
  //**************************************************************************
  /** Marks the message read/unread (PATCH isRead) and updates the local flag.
   */
    public void markRead(boolean read) throws GraphException {
        requirePath();
        JSONObject payload = new JSONObject();
        payload.set("isRead", read);
        conn.patch(resourcePath, payload);
        toJson().set("isRead", read);
        resetChanges();
    }


  //**************************************************************************
  //** move
  //**************************************************************************
  /** Moves the message to another folder. Note: Graph assigns the moved copy a
   *  new id, so re-fetch if you need to operate on it further.
   */
    public void move(String destinationFolderId) throws GraphException {
        requirePath();
        JSONObject payload = new JSONObject();
        payload.set("destinationId", destinationFolderId);
        conn.post(resourcePath + "/move", payload);
    }


  //**************************************************************************
  //** delete
  //**************************************************************************
  /** Deletes the message (moves it to Deleted Items).
   */
    public void delete() throws GraphException {
        requirePath();
        conn.delete(resourcePath);
    }


  //**************************************************************************
  //** draftFrom
  //**************************************************************************
  /** Creates a draft from a Graph reply/forward action and wires up its
   *  resource path.
   */
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


  //**************************************************************************
  //** single
  //**************************************************************************
  /** Returns the EmailAddress wrapped in the given single-recipient property,
   *  or null.
   */
    private EmailAddress single(String key){
        if (get(key).isNull()) return null;
        JSONObject o = get(key).toJSONObject();
        if (o.get("emailAddress").isNull()) return null;
        try{ return new EmailAddress(o.get("emailAddress").toJSONObject()); }
        catch(Exception e){ return null; }
    }


  //**************************************************************************
  //** recipients
  //**************************************************************************
  /** Returns the list of EmailAddress values for the given recipient-collection
   *  property.
   */
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


  //**************************************************************************
  //** setRecipients
  //**************************************************************************
  /** Replaces the given recipient-collection property with the supplied
   *  addresses.
   */
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


  //**************************************************************************
  //** addRecipient
  //**************************************************************************
  /** Appends a single address to the given recipient-collection property.
   */
    private void addRecipient(String key, EmailAddress e){
        if (e==null) return;
        JSONArray arr = get(key).isNull() ? new JSONArray() : get(key).toJSONArray();
        JSONObject r = new JSONObject();
        r.set("emailAddress", e.toJson());
        arr.add(r);
        set(key, arr);
    }


  //**************************************************************************
  //** date
  //**************************************************************************
  /** Parses the string value of the given property into a Date, or null.
   */
    private javaxt.utils.Date date(String key){
        return parseDate(str(key));
    }
}
