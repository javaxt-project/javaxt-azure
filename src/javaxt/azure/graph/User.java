package javaxt.azure.graph;

import java.util.*;
import javaxt.json.*;

public class User extends Node {

  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public User(JSONObject json, Connection conn){
        super(json, conn);
    }


  //**************************************************************************
  //** getEmail
  //**************************************************************************
    public String getEmail(){
        return get("mail").toString();
    }


  //**************************************************************************
  //** getCalendars
  //**************************************************************************
    public ArrayList<Calendar> getCalendars() throws Exception {
        ArrayList<Calendar> calendars = new ArrayList<>();
        String userID = getID();
        String email = getEmail();
        String url = "/users/" + userID + "/calendars";
        JSONObject json = conn.getResponse(url);
        for (JSONValue v : json.get("value").toJSONArray()){
            Calendar calendar = new Calendar(v.toJSONObject(), userID, conn);
            String owner = calendar.get("owner").get("address").toString();
            if (owner.equalsIgnoreCase(email)){
                calendars.add(calendar);
            }
        }
        return calendars;
    }


  //**************************************************************************
  //** getUsers
  //**************************************************************************
    public static ArrayList<User> getUsers(Connection conn) throws Exception {
        ArrayList<User> users = new ArrayList<>();
        JSONObject json = conn.getResponse("/users");
        for (JSONValue v : json.get("value").toJSONArray()){
            JSONObject user = v.toJSONObject();
            users.add(new User(user, conn));
        }
        return users;
    }

}