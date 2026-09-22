package javaxt.azure.graph;

import java.util.*;
import javaxt.json.*;
import javax.net.ssl.*;

//******************************************************************************
//**  SharePoint
//******************************************************************************
/**
 *   Provides read access to files and folders stored in a SharePoint site's
 *   document libraries (drives) via Microsoft Graph.
 *
 ******************************************************************************/

public class SharePoint {

    private final Connection conn;
    private final String host;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a SharePoint client for the given host (e.g.
   *  "contoso.sharepoint.com"), using the given Graph connection.
   */
    public SharePoint(String host, Connection conn){
        this.host = host;
        this.conn = conn;
    }


  //**************************************************************************
  //** getSite
  //**************************************************************************
  /** Returns the site with the given name (the path under
   *  <code>/sites/</code>).
   */
    public Site getSite(String name) throws Exception {
        return new Site(name);
    }


  //**************************************************************************
  //** Site Class
  //**************************************************************************
  /** Represents a SharePoint site and its document libraries (drives).
   */
    public class Site {
        private final String siteID;

      //**********************************************************************
      //** Constructor
      //**********************************************************************
      /** Looks up and stores the site id for the given site name.
       */
        public Site(String name) throws Exception {
            String url = "/sites/" + host + ":/sites/" + name + "?select=id";
            JSONObject siteInfo = conn.getResponse(url);
            siteID = siteInfo.get("id").toString();
        }


      //**********************************************************************
      //** getDrives
      //**********************************************************************
      /** Returns all document libraries (drives) in the site.
       */
        public ArrayList<Drive> getDrives() throws Exception {
            ArrayList<Drive> drives = new ArrayList<>();
            String url = "/sites/" + siteID + "/drives";
            for (JSONValue v : conn.getResponse(url).get("value").toJSONArray()){
                drives.add(new Drive(v.toJSONObject()));
            }
            return drives;
        }


      //**********************************************************************
      //** getDrive
      //**********************************************************************
      /** Returns the drive with the given name, or null if none matches.
       */
        public Drive getDrive(String name) throws Exception {
            for (Drive drive : getDrives()){
                if (drive.getName().equalsIgnoreCase(name)){
                    return drive;
                }
            }
            return null;
        }

    }


  //**************************************************************************
  //** Drive Class
  //**************************************************************************
  /** Represents a document library (drive) and the files/folders it contains.
   */
    public class Drive {
        private final JSONObject json;

      //**********************************************************************
      //** Constructor
      //**********************************************************************
      /** Wraps a Graph drive JSON object.
       */
        private Drive(JSONObject json){
            this.json = json;
        }


      //**********************************************************************
      //** getName
      //**********************************************************************
      /** Returns the drive name.
       */
        public String getName(){
            return json.get("name").toString();
        }


      //**********************************************************************
      //** getID
      //**********************************************************************
      /** Returns the drive id.
       */
        public String getID(){
            return json.get("id").toString();
        }


      //**********************************************************************
      //** getChildren
      //**********************************************************************
      /** Returns the items in the drive's root folder.
       */
        public ArrayList<Item> getChildren() throws Exception {
            ArrayList<Item> items = new ArrayList<>();
            String url = "/drives/" + getID() + "/root/children";
            for (JSONValue v : conn.getResponse(url).get("value").toJSONArray()){
                items.add(new Item(v.toJSONObject(), this));
            }
            return items;
        }


      //**********************************************************************
      //** getFolder
      //**********************************************************************
      /** Returns the folder at the given path (a sequence of folder names from
       *  the drive root), or null if the path cannot be resolved.
       */
        public Item getFolder(String... names) throws Exception {
            for (Item item : getChildren()){
                if (item.getName().equalsIgnoreCase(names[0])){
                    if (item.isFolder()){
                        if (names.length==1) return item;
                        else{

                            Item currFolder = item;
                            for (int i=1; i<names.length; i++){
                                String folderName = names[i];
                                currFolder = findFolder(folderName, currFolder);
                                if (currFolder==null) return null;
                                if (i==names.length-1) return currFolder;
                            }
                        }
                    }
                }
            }
            return null;
        }


      //**********************************************************************
      //** findFolder
      //**********************************************************************
      /** Returns the named child folder of the given folder, or null.
       */
        private Item findFolder(String name, Item folder) throws Exception {
            for (Item item : folder.getChildren()){
                if (item.getName().equalsIgnoreCase(name)){
                    if (item.isFolder()){
                        return item;
                    }
                }
            }
            return null;
        }


      //**********************************************************************
      //** toString
      //**********************************************************************
      /** Returns the drive's underlying JSON as a formatted string.
       */
        public String toString(){
            return json.toString(4);
        }
    }


  //**************************************************************************
  //** Item Class
  //**************************************************************************
  /** Represents a file or folder (driveItem) on a drive.
   */
    public class Item {
        private final JSONObject json;
        private final Drive drive;

      //**********************************************************************
      //** Constructor
      //**********************************************************************
      /** Wraps a Graph driveItem JSON object bound to its drive.
       */
        private Item(JSONObject json, Drive drive){
            this.json = json;
            this.drive = drive;
        }


      //**********************************************************************
      //** getName
      //**********************************************************************
      /** Returns the item name (file or folder name).
       */
        public String getName(){
            return json.get("name").toString();
        }


      //**********************************************************************
      //** getID
      //**********************************************************************
      /** Returns the item id.
       */
        public String getID(){
            return json.get("id").toString();
        }


      //**********************************************************************
      //** isFolder
      //**********************************************************************
      /** Returns true if this item is a folder.
       */
        public boolean isFolder(){
            return json.has("folder");
        }


      //**********************************************************************
      //** get
      //**********************************************************************
      /** Returns the raw value for the given key.
       */
        public JSONValue get(String key){
            return json.get(key);
        }


      //**********************************************************************
      //** getChildren
      //**********************************************************************
      /** Returns the child items of this folder (empty if it is a file).
       */
        public ArrayList<Item> getChildren() throws Exception {
            ArrayList<Item> items = new ArrayList<>();
            if (!isFolder()) return items; //throw Error?

            String url = "/drives/" + drive.getID() + "/items/" + getID() + "/children";
            for (JSONValue v : conn.getResponse(url).get("value").toJSONArray()){
                items.add(new Item(v.toJSONObject(), drive));
            }
            return items;
        }


      //**********************************************************************
      //** download
      //**********************************************************************
      /** Downloads this file to the given local path. See
       *  {@link #download(javaxt.io.File)}.
       */
        public boolean download(String path) throws Exception {
            return download(new javaxt.io.File(path));
        }


      //**********************************************************************
      //** download
      //**********************************************************************
      /** Downloads this file to the given local file, skipping the download when
       *  the local copy is already up to date. Returns true if the file was
       *  (re)written, false if it was skipped or this item is a folder.
       */
        public boolean download(javaxt.io.File file) throws Exception {
            if (isFolder()) return false;

          //Get date (parseDate truncates Graph's 7-digit fractional seconds so
          //the mtime comparison below isn't skewed by up to a few hours)
            javaxt.utils.Date lastModified = Node.parseDate(
            this.get("fileSystemInfo").get("lastModifiedDateTime").toString());


          //Skip download if the file exists and if it hasn't been modified
            if (file.exists()){
                if (lastModified.getTime()>file.getDate().getTime()){

                }
                else{
                    return false;
                }
            }


          //Get download link
            String downloadUrl = this.get("@microsoft.graph.downloadUrl").toString();


          //The download URL does not require an authorization header. However,
          //Azure is returning a 401 when we try to download via the javaxt
          //request/response classes. I suspect it has something to do with TLS
          //(the Azure endpoint requires TLS 1.2). The following is a workaround
            java.net.URL url = new java.net.URL(downloadUrl);
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, new java.security.SecureRandom());
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
            connection.setRequestMethod("GET");

          //Save file
            try (java.io.InputStream is = connection.getInputStream()) {
                file.write(is);
            }
            catch(java.io.IOException e){
                if (e.getMessage().contains("Server returned HTTP response code: 429")){
                    Thread.sleep(1500);
                    return download(file);
                }
                else{
                    throw e;
                }
            }

          //Update timestamp
            file.setDate(lastModified.getDate());

            return true;
        }


      //**********************************************************************
      //** toString
      //**********************************************************************
      /** Returns the item's underlying JSON as a formatted string.
       */
        public String toString(){
            return json.toString(4);
        }
    }

}
