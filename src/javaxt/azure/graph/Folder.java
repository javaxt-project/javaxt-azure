package javaxt.azure.graph;
import javaxt.json.JSONObject;

//******************************************************************************
//**  Folder
//******************************************************************************
/**
 *   Base class for Graph mailbox folders that form a hierarchy and hold items:
 *   {@link ContactFolder} and {@link EmailFolder}. Holds the folder metadata
 *   these share; each subclass adds access to its item type and child folders.
 *
 *   <p>Note: a {@link Calendar} is deliberately <b>not</b> a Folder. Graph models
 *   calendars as a flat, non-hierarchical container (grouped by calendarGroup),
 *   with different property names, so it extends {@link Node} directly.</p>
 *
 ******************************************************************************/

public abstract class Folder extends Node {

    protected final String userID;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    protected Folder(JSONObject json, String userID, Connection conn){
        super(json, conn);
        this.userID = userID;
    }


  //**************************************************************************
  //** getUserID
  //**************************************************************************
    public String getUserID(){
        return userID;
    }


  //**************************************************************************
  //** getDisplayName
  //**************************************************************************
    public String getDisplayName(){
        return get("displayName").isNull() ? null : get("displayName").toString();
    }


  //**************************************************************************
  //** getParentFolderID
  //**************************************************************************
    public String getParentFolderID(){
        return get("parentFolderId").isNull() ? null : get("parentFolderId").toString();
    }


  //**************************************************************************
  //** getChildFolderCount
  //**************************************************************************
    public Integer getChildFolderCount(){
        return get("childFolderCount").isNull() ? null : get("childFolderCount").toInteger();
    }
}
