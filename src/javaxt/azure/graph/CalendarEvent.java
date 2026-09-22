package javaxt.azure.graph;
import java.util.*;
import javaxt.json.*;

//******************************************************************************
//**  CalendarEvent
//******************************************************************************
/**
 *   Represents a Microsoft Graph calendar event. Exposes Graph-native property
 *   access (via the {@link Node} base class) alongside typed convenience
 *   accessors. CalendarEvent is a top-level class with a public no-arg constructor, so a
 *   new event can be built without a {@link Calendar} instance:
 *   <pre>
 *   CalendarEvent e = new CalendarEvent();
 *   e.setSubject("Follow-up");
 *   e.setStart(start, "America/New_York");   // start is a javaxt.utils.Date
 *   e.setEnd(end, "America/New_York");
 *   calendar.createEvent(e);
 *   </pre>
 *
 ******************************************************************************/

public class CalendarEvent extends Item {

  //Meeting response values accepted by respond(...)
    public static final String ACCEPT = "accept";
    public static final String DECLINE = "decline";
    public static final String TENTATIVE = "tentativelyAccept";


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new, empty event to be populated and passed to
   *  {@link Calendar#createEvent}.
   */
    public CalendarEvent(){
        super(new JSONObject(), null);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Wraps an existing Graph event JSON payload and binds it to the given
   *  connection so it can be refreshed or updated.
   */
    public CalendarEvent(JSONObject json, Connection conn){
        super(json, conn);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Binds to an existing event by id (e.g. to PATCH an event tracked by id)
   *  without marking <code>id</code> as a changed field.
   */
    public CalendarEvent(String id){
        super(new JSONObject(), null);
        if (id!=null) toJson().set("id", id);
    }


  //**************************************************************************
  //** getSubject
  //**************************************************************************
  /** Returns the event subject/title.
   */
    public String getSubject(){ return str("subject"); }


  //**************************************************************************
  //** setSubject
  //**************************************************************************
  /** Sets the event subject/title.
   */
    public void setSubject(String subject){ set("subject", subject); }


  //**************************************************************************
  //** getBodyPreview
  //**************************************************************************
  /** Returns a short plain-text preview of the event body.
   */
    public String getBodyPreview(){ return str("bodyPreview"); }


  //**************************************************************************
  //** getStart
  //**************************************************************************
  /** Returns the event start as a javaxt.utils.Date. The returned date's
   *  time zone is set to the event's Graph time zone, so its local getters
   *  (getHour(), getDay(), ...) reflect wall-clock time while toISOString() gives
   *  the correct UTC instant. Returns null if there is no start.
   */
    public javaxt.utils.Date getStart(){ return parseDateTimeZone(get("start")); }


  //**************************************************************************
  //** getEnd
  //**************************************************************************
  /** Returns the event end as a javaxt.utils.Date; see {@link #getStart()}
   *  for the time-zone semantics of the returned date. Returns null if there is
   *  no end.
   */
    public javaxt.utils.Date getEnd(){ return parseDateTimeZone(get("end")); }


  //**************************************************************************
  //** getStartTimeZone
  //**************************************************************************
  /** Returns the raw Graph time zone name of the start (e.g. "UTC",
   *  "America/New_York" or "Eastern Standard Time"), or null.
   */
    public String getStartTimeZone(){ return dateTimeZoneName(get("start")); }


  //**************************************************************************
  //** getEndTimeZone
  //**************************************************************************
  /** Returns the raw Graph time zone name of the end (e.g. "UTC",
   *  "America/New_York" or "Eastern Standard Time"), or null.
   */
    public String getEndTimeZone(){
        return dateTimeZoneName(get("end"));
    }


  //**************************************************************************
  //** setStart
  //**************************************************************************
  /** Sets the start; the Graph time zone stored is the date's own time zone.
   */
    public void setStart(javaxt.utils.Date start){
        setDateTimeZone("start", start, null);
    }


  //**************************************************************************
  //** setStart
  //**************************************************************************
  /** Sets the start, expressed as wall-clock time in the given Graph time zone.
   */
    public void setStart(javaxt.utils.Date start, String timeZone){
        setDateTimeZone("start", start, timeZone);
    }


  //**************************************************************************
  //** setEnd
  //**************************************************************************
  /** Sets the end; the Graph time zone stored is the date's own time zone.
   */
    public void setEnd(javaxt.utils.Date end){
        setDateTimeZone("end", end, null);
    }


  //**************************************************************************
  //** setEnd
  //**************************************************************************
  /** Sets the end, expressed as wall-clock time in the given Graph time zone.
   */
    public void setEnd(javaxt.utils.Date end, String timeZone){
        setDateTimeZone("end", end, timeZone);
    }


  //**************************************************************************
  //** isAllDay
  //**************************************************************************
  /** Returns true if this is an all-day event.
   */
    public boolean isAllDay(){
        return !get("isAllDay").isNull() && get("isAllDay").toBoolean();
    }


  //**************************************************************************
  //** setAllDay
  //**************************************************************************
  /** Flags this event as an all-day event.
   */
    public void setAllDay(boolean allDay){
        set("isAllDay", allDay);
    }


  //**************************************************************************
  //** setAllDayEvent
  //**************************************************************************
  /** Configures this event as an all-day event spanning <code>days</code> days
   *  starting on the calendar date of <code>start</code>. Per Graph, the start is
   *  local midnight and the end is the exclusive local midnight <code>days</code>
   *  days later.
   */
    public CalendarEvent setAllDayEvent(javaxt.utils.Date start, int days, String timeZone){
        if (start==null) return this;
        if (days<1) days = 1;
        String tz = (timeZone==null || timeZone.trim().isEmpty()) ? "UTC" : timeZone;
      //Use the wall-clock calendar date of the supplied value (not an instant
      //converted into tz) so the boundaries land on the intended date regardless
      //of the machine's default time zone.
        String startDay = start.toString("yyyy-MM-dd");
        String endDay = start.clone().add(days, "days").toString("yyyy-MM-dd");
        setAllDay(true);
        putDateTimeZone("start", startDay + "T00:00:00.0000000", tz);
        putDateTimeZone("end", endDay + "T00:00:00.0000000", tz);
        return this;
    }


  //**************************************************************************
  //** parseDateTimeZone
  //**************************************************************************
  /** Parses a Graph <code>dateTimeTimeZone</code> object into a Date whose
   *  instant is correct for the object's time zone.
   */
    private static javaxt.utils.Date parseDateTimeZone(JSONValue v){
        if (v==null || v.isNull()) return null;
        JSONObject o = v.toJSONObject();
        if (o.get("dateTime").isNull()) return null;
        String dt = o.get("dateTime").toString();
        String tz = o.get("timeZone").isNull() ? null : o.get("timeZone").toString();
        javaxt.utils.Date d = parseDate(dt);
        if (d==null) return null;
        java.util.TimeZone zone = zone(tz);
        if (zone!=null) d.setTimeZone(zone, true);   //reinterpret wall clock in the zone
        return d;
    }


  //**************************************************************************
  //** dateTimeZoneName
  //**************************************************************************
  /** Returns the time zone name from a Graph <code>dateTimeTimeZone</code>
   *  object, or null.
   */
    private static String dateTimeZoneName(JSONValue v){
        if (v==null || v.isNull()) return null;
        JSONObject o = v.toJSONObject();
        return o.get("timeZone").isNull() ? null : o.get("timeZone").toString();
    }


  //**************************************************************************
  //** setDateTimeZone
  //**************************************************************************
  /** Stores the given Date under the key as a Graph <code>dateTimeTimeZone</code>
   *  object, using the supplied time zone (or the date's own when null).
   */
    private void setDateTimeZone(String key, javaxt.utils.Date date, String timeZone){
        if (date==null){ set(key, null); return; }
        String tz = (timeZone==null || timeZone.trim().isEmpty()) ? zoneId(date) : timeZone;
        String dt = date.toString("yyyy-MM-dd'T'HH:mm:ss.SSS'0000'", zone(tz));
        putDateTimeZone(key, dt, tz);
    }


  //**************************************************************************
  //** putDateTimeZone
  //**************************************************************************
  /** Stores a raw dateTime string and time zone under the key as a Graph
   *  <code>dateTimeTimeZone</code> object.
   */
    private void putDateTimeZone(String key, String dateTime, String timeZone){
        JSONObject json = new JSONObject();
        json.set("dateTime", dateTime);
        json.set("timeZone", timeZone);
        set(key, json);
    }


  //**************************************************************************
  //** zoneId
  //**************************************************************************
  /** Returns the id of the date's time zone, or "UTC" when none is set.
   */
    private static String zoneId(javaxt.utils.Date date){
        java.util.TimeZone tz = date.getTimeZone();
        return (tz==null) ? "UTC" : tz.getID();
    }


  //**************************************************************************
  //** zone
  //**************************************************************************
  /** Resolves a Windows or IANA time-zone name to a TimeZone.
   */
    private static java.util.TimeZone zone(String name){
        if (name==null || name.trim().isEmpty()) return null;
        java.util.TimeZone tz = javaxt.utils.Date.getTimeZone(name);
        if (tz!=null) return tz;
        return java.util.TimeZone.getTimeZone(name);   //IANA fallback
    }


  //**************************************************************************
  //** getLocation
  //**************************************************************************
  /** Returns the event's primary location, or null if none is set.
   */
    public Location getLocation(){
        return get("location").isNull() ? null : new Location(get("location").toJSONObject());
    }


  //**************************************************************************
  //** setLocation
  //**************************************************************************
  /** Sets the event's primary location (pass null to clear it).
   */
    public void setLocation(Location location){ set("location", location==null ? null : location.toJson()); }


  //**************************************************************************
  //** getLocations
  //**************************************************************************
  /** Returns all locations associated with the event, or an empty list.
   */
    public List<Location> getLocations(){
        ArrayList<Location> list = new ArrayList<>();
        if (!get("locations").isNull()){
            for (JSONValue v : get("locations").toJSONArray()) list.add(new Location(v.toJSONObject()));
        }
        return list;
    }


  //**************************************************************************
  //** getOrganizer
  //**************************************************************************
  /** Returns the meeting organizer as an {@link EmailAddress}, or null.
   */
    public EmailAddress getOrganizer(){
        if (get("organizer").isNull()) return null;
        JSONObject org = get("organizer").toJSONObject();
        if (org.get("emailAddress").isNull()) return null;
        try{ return new EmailAddress(org.get("emailAddress").toJSONObject()); }
        catch(Exception e){ return null; }
    }


  //**************************************************************************
  //** getAttendees
  //**************************************************************************
  /** Returns the meeting attendees, or an empty list.
   */
    public List<Attendee> getAttendees(){
        ArrayList<Attendee> list = new ArrayList<>();
        if (!get("attendees").isNull()){
            for (JSONValue v : get("attendees").toJSONArray()) list.add(new Attendee(v.toJSONObject()));
        }
        return list;
    }


  //**************************************************************************
  //** setAttendees
  //**************************************************************************
  /** Replaces the event's attendee list with the given attendees.
   */
    public void setAttendees(List<Attendee> attendees){
        JSONArray arr = new JSONArray();
        if (attendees!=null) for (Attendee a : attendees) if (a!=null) arr.add(a.toJson());
        set("attendees", arr);
    }


  //**************************************************************************
  //** addAttendee
  //**************************************************************************
  /** Appends a single attendee to the event's attendee list.
   */
    public void addAttendee(Attendee attendee){
        if (attendee==null) return;
        JSONArray arr = get("attendees").isNull() ? new JSONArray() : get("attendees").toJSONArray();
        arr.add(attendee.toJson());
        set("attendees", arr);
    }


  //**************************************************************************
  //** getShowAs
  //**************************************************************************
  /** Returns how the event shows on the calendar (free|tentative|busy|oof|workingElsewhere|unknown).
   */
    public String getShowAs(){ return str("showAs"); }


  //**************************************************************************
  //** setShowAs
  //**************************************************************************
  /** Sets how the event shows on the calendar (free|tentative|busy|oof|workingElsewhere).
   */
    public void setShowAs(String showAs){ set("showAs", showAs); }


  //**************************************************************************
  //** getSensitivity
  //**************************************************************************
  /** Returns the event sensitivity (normal|personal|private|confidential).
   */
    public String getSensitivity(){
        return str("sensitivity");
    }


  //**************************************************************************
  //** setSensitivity
  //**************************************************************************
  /** Sets the event sensitivity (normal|personal|private|confidential).
   */
    public void setSensitivity(String sensitivity){
        set("sensitivity", sensitivity);
    }


  //**************************************************************************
  //** getImportance
  //**************************************************************************
  /** Returns the event importance (low|normal|high).
   */
    public String getImportance(){
        return str("importance");
    }


  //**************************************************************************
  //** isCancelled
  //**************************************************************************
  /** Returns true if the event has been cancelled.
   */
    public boolean isCancelled(){
        return !get("isCancelled").isNull() && get("isCancelled").toBoolean();
    }


  //**************************************************************************
  //** isReminderOn
  //**************************************************************************
  /** Returns true if a reminder is enabled for the event.
   */
    public boolean isReminderOn(){
        return !get("isReminderOn").isNull() && get("isReminderOn").toBoolean();
    }


  //**************************************************************************
  //** setReminder
  //**************************************************************************
  /** Turns the reminder on and sets how many minutes before the start it fires.
   */
    public void setReminder(int minutesBeforeStart){
        set("isReminderOn", true);
        set("reminderMinutesBeforeStart", minutesBeforeStart);
    }


  //**************************************************************************
  //** isOrganizer
  //**************************************************************************
  /** True when the mailbox that loaded this event is the meeting organizer.
   */
    public boolean isOrganizer(){
        return !get("isOrganizer").isNull() && get("isOrganizer").toBoolean();
    }


  //**************************************************************************
  //** isResponseRequested
  //**************************************************************************
  /** True when a response (accept/decline/tentative) is requested of attendees.
   */
    public boolean isResponseRequested(){
        return !get("responseRequested").isNull() && get("responseRequested").toBoolean();
    }


  //**************************************************************************
  //** getResponseStatus
  //**************************************************************************
  /** Returns the raw <code>responseStatus</code> object
   *  (<code>{response, time}</code>) for the mailbox that loaded this event, or null.
   */
    public JSONObject getResponseStatus(){
        return get("responseStatus").isNull() ? null : get("responseStatus").toJSONObject();
    }


  //**************************************************************************
  //** getResponse
  //**************************************************************************
  /** The mailbox owner's response to this meeting:
   *  none|organizer|tentativelyAccepted|accepted|declined|notResponded, or null.
   */
    public String getResponse(){
        JSONObject rs = getResponseStatus();
        return (rs==null || rs.get("response").isNull()) ? null : rs.get("response").toString();
    }


  //**************************************************************************
  //** getResponseTime
  //**************************************************************************
  /** The time the mailbox owner responded, or null.
   */
    public javaxt.utils.Date getResponseTime(){
        JSONObject rs = getResponseStatus();
        if (rs==null || rs.get("time").isNull()) return null;
        return parseDate(rs.get("time").toString());
    }


  //**************************************************************************
  //** isOnlineMeeting
  //**************************************************************************
  /** Returns true if the event is an online meeting (e.g. Teams).
   */
    public boolean isOnlineMeeting(){
        return !get("isOnlineMeeting").isNull() && get("isOnlineMeeting").toBoolean();
    }


  //**************************************************************************
  //** getOnlineMeetingProvider
  //**************************************************************************
  /** e.g. "teamsForBusiness", or null when this is not an online meeting.
   */
    public String getOnlineMeetingProvider(){
        return str("onlineMeetingProvider");
    }


  //**************************************************************************
  //** getOnlineMeetingUrl
  //**************************************************************************
  /** The join URL for the online meeting, or null.
   */
    public String getOnlineMeetingUrl(){
        if (get("onlineMeeting").isNull()) return null;
        JSONObject m = get("onlineMeeting").toJSONObject();
        return m.get("joinUrl").isNull() ? null : m.get("joinUrl").toString();
    }


  //**************************************************************************
  //** respond
  //**************************************************************************
  /** Responds to a meeting invitation on behalf of the mailbox that loaded this
   *  event. Only meaningful for an attendee (not the organizer).
   *  @param response one of {@link #ACCEPT}, {@link #DECLINE} or {@link #TENTATIVE}.
   *  @param comment optional comment sent to the organizer, or null.
   *  @param sendResponse whether to notify the organizer of the response.
   */
    public void respond(String response, String comment, boolean sendResponse) throws GraphException {
        requirePath();
        String action = normalizeResponse(response);
        JSONObject payload = new JSONObject();
        if (comment!=null) payload.set("comment", comment);
        payload.set("sendResponse", sendResponse);
        conn.post(resourcePath + "/" + action, payload);
    }


  //**************************************************************************
  //** normalizeResponse
  //**************************************************************************
  /** Maps a response verb to the Graph action name (accept, decline or
   *  tentativelyAccept), throwing on an unrecognized value.
   */
    private static String normalizeResponse(String response) throws GraphException {
        String r = (response==null) ? "" : response.trim().toLowerCase();
        if (r.equals("accept") || r.equals("accepted")) return ACCEPT;
        if (r.equals("decline") || r.equals("declined")) return DECLINE;
        if (r.equals("tentative") || r.equals("tentativelyaccept") || r.equals("tentativelyaccepted")) return TENTATIVE;
        throw new GraphException("Invalid response '" + response + "' (expected accept, decline, or tentativelyAccept)");
    }


  //**************************************************************************
  //** getType
  //**************************************************************************
  /** singleInstance | occurrence | exception | seriesMaster
   */
    public String getType(){ return str("type"); }


  //**************************************************************************
  //** getSeriesMasterID
  //**************************************************************************
  /** Returns the id of the series master for an occurrence/exception, or null.
   */
    public String getSeriesMasterID(){ return str("seriesMasterId"); }


  //**************************************************************************
  //** getICalUId
  //**************************************************************************
  /** Returns the iCalendar UID that is stable across a recurring series.
   */
    public String getICalUId(){ return str("iCalUId"); }


  //**************************************************************************
  //** getWebLink
  //**************************************************************************
  /** Returns the URL to open the event in Outlook on the web.
   */
    public String getWebLink(){ return str("webLink"); }


  //**************************************************************************
  //** getRecurrence
  //**************************************************************************
  /** Returns the raw recurrence object (no typed recurrence editing yet).
   */
    public JSONObject getRecurrence(){
        return get("recurrence").isNull() ? null : get("recurrence").toJSONObject();
    }


  //**************************************************************************
  //** Attendee Class
  //**************************************************************************
  /** A meeting attendee: an {@link EmailAddress}, a type
   *  (required|optional|resource) and, for events read from Graph, a response
   *  status.
   */
    public static class Attendee {

        public static final String REQUIRED = "required";
        public static final String OPTIONAL = "optional";
        public static final String RESOURCE = "resource";

        private final EmailAddress emailAddress;
        private final String type;
        private final String response;
        private final String responseTime;


      //************************************************************************
      //** Constructor
      //************************************************************************
      /** Creates an attendee with the given email address and type
       *  ({@link #REQUIRED}, {@link #OPTIONAL} or {@link #RESOURCE}).
       */
        public Attendee(EmailAddress emailAddress, String type){
            this.emailAddress = emailAddress;
            this.type = (type==null) ? REQUIRED : type.toLowerCase();
            this.response = null;
            this.responseTime = null;
        }


      //************************************************************************
      //** Constructor
      //************************************************************************
      /** Creates an attendee from a Graph attendee JSON object, including its
       *  response status when present.
       */
        public Attendee(JSONObject json){
            if (json!=null){
                this.emailAddress = json.get("emailAddress").isNull() ? null
                    : new EmailAddress(json.get("emailAddress").toJSONObject());
                this.type = json.get("type").isNull() ? REQUIRED : json.get("type").toString().toLowerCase();
                if (!json.get("status").isNull()){
                    JSONObject status = json.get("status").toJSONObject();
                    this.response = status.get("response").isNull() ? null : status.get("response").toString();
                    this.responseTime = status.get("time").isNull() ? null : status.get("time").toString();
                }
                else{
                    this.response = null;
                    this.responseTime = null;
                }
            }
            else{
                this.emailAddress = null;
                this.type = REQUIRED;
                this.response = null;
                this.responseTime = null;
            }
        }


      //************************************************************************
      //** getEmailAddress
      //************************************************************************
      /** Returns the attendee's email address.
       */
        public EmailAddress getEmailAddress(){ return emailAddress; }


      //************************************************************************
      //** getType
      //************************************************************************
      /** Returns the attendee type (required|optional|resource).
       */
        public String getType(){ return type; }


      //************************************************************************
      //** getResponse
      //************************************************************************
      /** Returns the attendee's response (e.g. accepted|declined|tentativelyAccepted|none), or null.
       */
        public String getResponse(){ return response; }


      //************************************************************************
      //** getResponseTime
      //************************************************************************
      /** Returns the time the attendee responded, or null.
       */
        public String getResponseTime(){ return responseTime; }


      //************************************************************************
      //** toJson
      //************************************************************************
      /** Serializes this attendee to a Graph attendee JSON object.
       */
        public JSONObject toJson(){
            JSONObject json = new JSONObject();
            if (emailAddress!=null) json.set("emailAddress", emailAddress.toJson());
            json.set("type", type);
            return json;
        }


      //************************************************************************
      //** equals
      //************************************************************************
      /** Returns true if the given object is an Attendee with the same email address and type.
       */
        public boolean equals(Object obj){
            if (this==obj) return true;
            if (!(obj instanceof Attendee)) return false;
            Attendee o = (Attendee) obj;
            return Objects.equals(emailAddress, o.emailAddress) && Objects.equals(type, o.type);
        }


      //************************************************************************
      //** hashCode
      //************************************************************************
      /** Returns a hash code derived from the email address and type.
       */
        public int hashCode(){ return Objects.hash(emailAddress, type); }
    }


  //**************************************************************************
  //** Location Class
  //**************************************************************************
  /** An event location. The <code>displayName</code> is the human-readable
   *  location text.
   */
    public static class Location {

        private String displayName;
        private PhysicalAddress address;


      //************************************************************************
      //** Constructor
      //************************************************************************
      /** Creates a location with the given display name.
       */
        public Location(String displayName){
            this.displayName = displayName;
        }


      //************************************************************************
      //** Constructor
      //************************************************************************
      /** Creates a location from a Graph location JSON object, including its
       *  physical address when present.
       */
        public Location(JSONObject json){
            if (json!=null){
                this.displayName = json.get("displayName").isNull() ? null : json.get("displayName").toString();
                if (!json.get("address").isNull()){
                    this.address = new PhysicalAddress(json.get("address").toJSONObject());
                }
            }
        }


      //************************************************************************
      //** getDisplayName
      //************************************************************************
      /** Returns the location's display name.
       */
        public String getDisplayName(){ return displayName; }


      //************************************************************************
      //** setDisplayName
      //************************************************************************
      /** Sets the location's display name.
       */
        public void setDisplayName(String displayName){ this.displayName = displayName; }


      //************************************************************************
      //** getAddress
      //************************************************************************
      /** Returns the location's physical address, or null.
       */
        public PhysicalAddress getAddress(){ return address; }


      //************************************************************************
      //** setAddress
      //************************************************************************
      /** Sets the location's physical address.
       */
        public void setAddress(PhysicalAddress address){ this.address = address; }


      //************************************************************************
      //** toJson
      //************************************************************************
      /** Serializes this location to a Graph location JSON object.
       */
        public JSONObject toJson(){
            JSONObject json = new JSONObject();
            if (displayName!=null) json.set("displayName", displayName);
            if (address!=null && !address.isEmpty()) json.set("address", address.toJson());
            return json;
        }


      //************************************************************************
      //** toString
      //************************************************************************
      /** Returns the location's display name.
       */
        public String toString(){ return displayName; }


      //************************************************************************
      //** equals
      //************************************************************************
      /** Returns true if the given object is a Location with the same display name and address.
       */
        public boolean equals(Object obj){
            if (this==obj) return true;
            if (!(obj instanceof Location)) return false;
            Location o = (Location) obj;
            return Objects.equals(displayName, o.displayName) && Objects.equals(address, o.address);
        }


      //************************************************************************
      //** hashCode
      //************************************************************************
      /** Returns a hash code derived from the display name and address.
       */
        public int hashCode(){ return Objects.hash(displayName, address); }
    }
}
