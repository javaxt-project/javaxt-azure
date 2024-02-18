package javaxt.azure.graph;

import java.util.*;
import javaxt.json.*;


//******************************************************************************
//**  Calendar
//******************************************************************************
/**
 *   Used to encapsulate a Calendar and Events
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
  //** isDefault
  //**************************************************************************
    public boolean isDefault(){
        return get("isDefaultCalendar").toBoolean();
    }


  //**************************************************************************
  //** getEvents
  //**************************************************************************
    public ArrayList<Event> getEvents(Integer limit) throws Exception {
        ArrayList<Event> events = new ArrayList<>();

        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        if (limit!=null) params.put("$top", limit+"");

        JSONObject json = conn.getResponse(getURL(params));
        for (JSONValue r : json.get("value").toJSONArray()){
            events.add(new Event(r.toJSONObject(), conn));
        }

        return events;
    }


  //**************************************************************************
  //** getEvents
  //**************************************************************************
    public ArrayList<Event> getEvents(javaxt.utils.Date startDate,
        javaxt.utils.Date endDate) throws Exception {

        LinkedHashMap<String, String> params = new LinkedHashMap<>();

      //Create date filter
        params.put("$filter", "start/dateTime ge '" + startDate.toISOString() +
        "' and end/dateTime le '" + endDate.toISOString() + "'");

      //Add order by
        params.put("$orderby", "start/dateTime asc");

        return getEvents(params);
    }


  //**************************************************************************
  //** getEvents
  //**************************************************************************
    private ArrayList<Event> getEvents(LinkedHashMap<String, String> params) throws Exception {
        ArrayList<Event> events = new ArrayList<>();


      //Get calendar events
        params.put("$count", "true");
        JSONObject json = conn.getResponse(getURL(params));
        Integer count = json.get("@odata.count").toInteger();
        JSONArray records = json.get("value").toJSONArray();
        for (JSONValue r : records){
            events.add(new Event(r.toJSONObject(), conn));
        }
        if (events.isEmpty()) return events;



      //Get more calendar events as needed
        while (events.size()<count){
            params.put("$skip", events.size()+"");
            json = conn.getResponse(getURL(params));
            for (JSONValue r : json.get("value").toJSONArray()){
                events.add(new Event(r.toJSONObject(), conn));
            }
        }

        return events;
    }


  //**************************************************************************
  //** Event Class
  //**************************************************************************
  /** Used to represent a calendar event
   */
    public class Event extends Node {
        public Event(JSONObject json, Connection conn){
            super(json, conn);
        }

        public String getSubject(){
            return get("subject").toString();
        }

        public javaxt.utils.Date getStartDate(){
            return getDate("start");
        }

        public javaxt.utils.Date getEndDate(){
            return getDate("end");
        }

        private javaxt.utils.Date getDate(String key){
            try{
                String dt = get(key).get("dateTime").toString();
                String tz = get(key).get("timeZone").toString();
                javaxt.utils.Date d = new javaxt.utils.Date(dt);
                d.setTimeZone(tz, true);
                return d;
            }
            catch(Exception e){
                return null;
            }
        }
    }


  //**************************************************************************
  //** getURL
  //**************************************************************************
    private String getURL(LinkedHashMap<String, String> params){
        String calendarID = getID();
        String url = "/users/" + userID + "/calendars/" + calendarID + "/events";

        if (!params.isEmpty()){

          //Update url
            url += "?";
            Iterator<String> it = params.keySet().iterator();
            while (it.hasNext()){
                String key = it.next();
                String val = params.get(key);
                val = val.replace(" ", "%20");
                url += "&" + key + "=" + val;
            }
        }

        return url;
    }


}