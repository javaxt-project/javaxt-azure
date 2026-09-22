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

  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a Folder bound to the given user-scoped connection. The connection
   *  must be scoped to a user (see {@link Connection#forUser}); the mailbox is
   *  derived from it.
   */
    protected Folder(JSONObject json, Connection conn){
        super(json, conn);
        validate();
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Binds a Folder to an existing folder id on the given user-scoped
   *  connection.
   */
    protected Folder(String id, Connection conn){
        super(id, conn);
        validate();
    }


  //**************************************************************************
  //** validate
  //**************************************************************************
  /** Ensures the connection is scoped to a user (mailbox).
   */
    private void validate(){
        if (conn==null || conn.getUserID()==null){
            throw new IllegalArgumentException(
                "Folder requires a user-scoped connection; use Connection.forUser(...)");
        }
    }


  //**************************************************************************
  //** getUserID
  //**************************************************************************
  /** Returns the id of the user (mailbox) that owns this folder.
   */
    public String getUserID(){
        return conn.getUserID();
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
