package javaxt.azure.graph;

import java.util.*;
import javaxt.json.*;

public class Node {

    protected Connection conn;
    private JSONObject json;

    protected Node(JSONObject json, Connection conn){
        this.json = json;
        this.conn = conn;
    }

    public String getID(){
        return json.get("id").toString();
    }

    public JSONValue get(String key){
        return json.get(key);
    }

    public String toString(){
        return json.toString(4);
    }
}