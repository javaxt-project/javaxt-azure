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
  /** Creates a Folder for the given user's mailbox from Graph JSON.
   */
    protected Folder(JSONObject json, String userID, Connection conn){
        super(json, conn);
        this.userID = userID;
    }


  //**************************************************************************
  //** getUserID
  //**************************************************************************
  /** Returns the id of the user (mailbox) that owns this folder.
   */
    public String getUserID(){
        return userID;
    }


  //**************************************************************************
  //** getDisplayName
  //**************************************************************************
  /** Returns the folder's display name, or null.
   */
    public String getDisplayName(){
        return get("displayName").isNull() ? null : get("displayName").toString();
    }


  //**************************************************************************
  //** getParentFolderID
  //**************************************************************************
  /** Returns the id of the parent folder, or null for a top-level folder.
   */
    public String getParentFolderID(){
        return get("parentFolderId").isNull() ? null : get("parentFolderId").toString();
    }


  //**************************************************************************
  //** getChildFolderCount
  //**************************************************************************
  /** Returns the number of child folders, or null if not populated.
   */
    public Integer getChildFolderCount(){
        return get("childFolderCount").isNull() ? null : get("childFolderCount").toInteger();
    }
}
