package javaxt.azure.graph;

import java.util.*;
import javaxt.json.*;


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

        String calendarID = getID();
        String url = "/users/" + userID + "/calendars/"+calendarID+"/events";
        HashSet<String> params = new HashSet<>();
        if (limit!=null) params.add("$top=" + limit);
        params.add("$count=true");

        if (!params.isEmpty()){
            url += "?";
            for (String param : params){
                url += "&" + param;
            }
        }


        JSONObject json = conn.getResponse(url);
        for (JSONValue v : json.get("value").toJSONArray()){
            events.add(new Event(v.toJSONObject(), conn));
        }

        return events;
    }


  //**************************************************************************
  //** Event Class
  //**************************************************************************
    public class Event extends Node {
        public Event(JSONObject json, Connection conn){
            super(json, conn);
        }

        public javaxt.utils.Date getStartDate(){
            try{
                return new javaxt.utils.Date(get("start").get("dateTime").toString());
            }
            catch(Exception e){
                return null;
            }
        }
    }
}