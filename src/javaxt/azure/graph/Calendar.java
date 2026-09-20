package javaxt.azure.graph;
import java.util.*;
import javaxt.json.*;

//******************************************************************************
//**  Calendar
//******************************************************************************
/**
 *   Represents a Microsoft Graph calendar and the operations on its events.
 *   Range queries use <code>calendarView</code>, so recurring appointments are
 *   returned as expanded occurrences (matching the EWS <code>CalendarView</code>
 *   behaviour the EMR relied on), and all list operations follow
 *   <code>@odata.nextLink</code> rather than <code>$skip</code>.
 *
 ******************************************************************************/

public class Calendar extends Node {

    private final String userID;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Calendar(JSONObject json, String userID, Connection conn){
        super(json, conn);
        this.userID = userID;
    }


  //**************************************************************************
  //** Metadata
  //**************************************************************************
    public String getName(){
        return get("name").isNull() ? null : get("name").toString();
    }

    public boolean isDefaultCalendar(){
        return !get("isDefaultCalendar").isNull() && get("isDefaultCalendar").toBoolean();
    }

  /** Retained for backwards compatibility; prefer {@link #isDefaultCalendar()}. */
    public boolean isDefault(){
        return isDefaultCalendar();
    }

    public boolean canEdit(){
        return !get("canEdit").isNull() && get("canEdit").toBoolean();
    }

    public String getColor(){
        return get("color").isNull() ? null : get("color").toString();
    }

  /** Returns the calendar owner as an {@link EmailAddress} (<code>{name,address}</code>). */
    public EmailAddress getOwner(){
        if (get("owner").isNull()) return null;
        try{ return new EmailAddress(get("owner").toJSONObject()); }
        catch(Exception e){ return null; }
    }


  //**************************************************************************
  //** getEvents
  //**************************************************************************
  /** Returns all events (occurrences expanded) that fall within the given
   *  window, using <code>calendarView</code> and following paging to the end.
   */
    public List<CalendarEvent> getEvents(javaxt.utils.Date start, javaxt.utils.Date end) throws GraphException {
        return getEvents(start, end, new EventQuery());
    }


  //**************************************************************************
  //** getEvents
  //**************************************************************************
  /** Same as {@link #getEvents(javaxt.utils.Date, javaxt.utils.Date)} with query
   *  options ($select, $expand extensions, page size, time zone, plain-text body).
   */
    public List<CalendarEvent> getEvents(javaxt.utils.Date start, javaxt.utils.Date end, EventQuery opts) throws GraphException {
        if (opts==null) opts = new EventQuery();

        Connection.Query q = new Connection.Query("/users/" + userID + "/calendars/" + getID() + "/calendarView");
        q.set("startDateTime", start.toISOString());
        q.set("endDateTime", end.toISOString());
        q.set("$top", opts.top!=null ? opts.top : 50);
        if (!opts.select.isEmpty()) q.set("$select", String.join(",", opts.select));
        if (opts.filter!=null) q.set("$filter", opts.filter);
        if (opts.orderBy!=null) q.set("$orderby", opts.orderBy);
        String expand = Item.expandExtensions(opts.expandExtensions);
        if (expand!=null) q.set("$expand", expand);
        if (!q.has("$orderby") && opts.filter==null) q.set("$orderby", "start/dateTime");

        Map<String, String> headers = new LinkedHashMap<>();
        String prefer = opts.buildPrefer();
        if (prefer!=null) headers.put("Prefer", prefer);

        ArrayList<CalendarEvent> events = new ArrayList<>();
        for (JSONObject json : conn.getList(q.toString(), headers)){
            events.add(bind(json));
        }
        return events;
    }


  //**************************************************************************
  //** getEventsDelta
  //**************************************************************************
  /** Incremental calendarView sync. Pass a null <code>deltaLink</code> for the
   *  initial sync (which uses the start/end window); pass a previously returned
   *  delta link for subsequent syncs. Removed events are reported by id.
   */
    public EventSync getEventsDelta(javaxt.utils.Date start, javaxt.utils.Date end, String deltaLink) throws GraphException {
        String url;
        if (deltaLink!=null){
            url = deltaLink;
        }
        else{
            Connection.Query q = new Connection.Query("/users/" + userID + "/calendars/" + getID() + "/calendarView/delta");
            q.set("startDateTime", start.toISOString());
            q.set("endDateTime", end.toISOString());
            url = q.toString();
        }

        Connection.Delta delta = conn.getDelta(url, null);
        ArrayList<CalendarEvent> items = new ArrayList<>();
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
        return new EventSync(items, removed, delta.getDeltaLink());
    }


  //**************************************************************************
  //** getEvent
  //**************************************************************************
    public CalendarEvent getEvent(String id) throws GraphException {
        return bind(conn.get("/users/" + userID + "/events/" + id));
    }


  //**************************************************************************
  //** createEvent
  //**************************************************************************
  /** Creates the event on this calendar. On success the passed CalendarEvent is updated
   *  in place with the server copy (id, changeKey, iCalUId, ...) and returned.
   */
    public CalendarEvent createEvent(CalendarEvent event) throws GraphException {
        String url = "/users/" + userID + "/calendars/" + getID() + "/events";
        JSONObject server = conn.post(url, event.toJson());
        event.conn = conn;
        event.replaceJson(server);
        event.setResourcePath(resourcePath(server));
        return event;
    }


  //**************************************************************************
  //** updateEvent
  //**************************************************************************
  /** Applies the event's pending changes via PATCH and returns the event updated
   *  with the server copy. Read-only keys are stripped from the body. By default
   *  the write is unconditional (last-writer-wins), which is what the EMR wants
   *  after resolving conflicts itself; use {@link #updateEvent(CalendarEvent,boolean)}
   *  to opt into optimistic concurrency. Updating an occurrence creates an
   *  exception, as intended.
   */
    public CalendarEvent updateEvent(CalendarEvent event) throws GraphException {
        return updateEvent(event, false);
    }

  /** @param useIfMatch when true, sends <code>If-Match: changeKey</code> so the
   *  PATCH fails with 412 if the item changed server-side since it was loaded.
   */
    public CalendarEvent updateEvent(CalendarEvent event, boolean useIfMatch) throws GraphException {
        String id = event.getID();
        if (id==null) throw new GraphException("Cannot update an event without an id; use createEvent");
        String url = "/users/" + userID + "/events/" + id;

        Map<String, String> headers = new LinkedHashMap<>();
        if (useIfMatch && event.getChangeKey()!=null) headers.put("If-Match", event.getChangeKey());

        JSONObject server = conn.execute("PATCH", url, event.writableChanges(), headers).toJson();
        event.conn = conn;
        if (server!=null && !server.get("id").isNull()){
            event.replaceJson(server);
            event.setResourcePath(resourcePath(server));
        }
        else{
            event.setResourcePath("/users/" + userID + "/events/" + id);
            event.resetChanges();
        }
        return event;
    }


  //**************************************************************************
  //** deleteEvent
  //**************************************************************************
  /** Deletes an event by id. A missing event surfaces as a
   *  {@link GraphException} with status 404. Never deletes a series master on
   *  behalf of a single-occurrence delete.
   */
    public void deleteEvent(String id) throws GraphException {
        if (id==null) return;
        conn.delete("/users/" + userID + "/events/" + id);
    }


  //**************************************************************************
  //** cancelEvent
  //**************************************************************************
  /** Cancels a meeting and notifies attendees. Only valid when the mailbox owner
   *  is the organizer.
   */
    public void cancelEvent(String id, String comment) throws GraphException {
        JSONObject payload = new JSONObject();
        if (comment!=null) payload.set("comment", comment);
        conn.post("/users/" + userID + "/events/" + id + "/cancel", payload);
    }


  //**************************************************************************
  //** bind
  //**************************************************************************
    private CalendarEvent bind(JSONObject json){
        CalendarEvent event = new CalendarEvent(json, conn);
        event.setResourcePath(resourcePath(json));
        return event;
    }

    private String resourcePath(JSONObject json){
        if (json==null || json.get("id").isNull()) return null;
        return "/users/" + userID + "/events/" + json.get("id").toString();
    }




  //**************************************************************************
  //** EventQuery Class
  //**************************************************************************
  /** Options for event queries: <code>$select</code>, open-extension
   *  <code>$expand</code>, page size (<code>$top</code>), result time zone
   *  (<code>Prefer: outlook.timezone</code>) and plain-text bodies
   *  (<code>Prefer: outlook.body-content-type="text"</code>). Setters chain.
   */
    public static class EventQuery {

        private Integer top;
        private final List<String> select = new ArrayList<>();
        private final List<String> expandExtensions = new ArrayList<>();
        private String timeZone;
        private boolean textBody;
        private String filter;
        private String orderBy;

      /** Page size ($top). Default 50; calendarView allows up to 1000. */
        public EventQuery setTop(Integer top){ this.top = top; return this; }
        public Integer getTop(){ return top; }

        public EventQuery select(String... properties){
            if (properties!=null) for (String p : properties) if (p!=null) select.add(p);
            return this;
        }

        public EventQuery expandExtension(String... extensionNames){
            if (extensionNames!=null) for (String n : extensionNames) if (n!=null) expandExtensions.add(n);
            return this;
        }

        public EventQuery setTimeZone(String timeZone){ this.timeZone = timeZone; return this; }
        public EventQuery setTextBody(boolean textBody){ this.textBody = textBody; return this; }
        public EventQuery setFilter(String filter){ this.filter = filter; return this; }
        public EventQuery setOrderBy(String orderBy){ this.orderBy = orderBy; return this; }

      /** Returns the Prefer value implied by this query (timezone/body), or null. */
        public String buildPrefer(){
            ArrayList<String> parts = new ArrayList<>();
            if (timeZone!=null && !timeZone.trim().isEmpty()) parts.add("outlook.timezone=\"" + timeZone + "\"");
            if (textBody) parts.add("outlook.body-content-type=\"text\"");
            return parts.isEmpty() ? null : String.join(", ", parts);
        }
    }


  //**************************************************************************
  //** EventSync Class
  //**************************************************************************
  /** Result of an incremental calendar sync: the added/changed events, the ids
   *  of removed events, and the delta link to persist for the next sync.
   */
    public static class EventSync {

        private final List<CalendarEvent> events;
        private final List<String> removedIds;
        private final String deltaLink;

        EventSync(List<CalendarEvent> events, List<String> removedIds, String deltaLink){
            this.events = events;
            this.removedIds = removedIds;
            this.deltaLink = deltaLink;
        }

        public List<CalendarEvent> getEvents(){ return events; }
        public List<String> getRemovedIds(){ return removedIds; }
        public String getDeltaLink(){ return deltaLink; }
    }
}
