package javaxt.azure.graph;

import java.util.*;
import javaxt.json.*;
import javax.net.ssl.*;

public class SharePoint {

    private final Connection conn;
    private final String host;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public SharePoint(String host, Connection conn){
        this.host = host;
        this.conn = conn;
    }


  //**************************************************************************
  //** getSite
  //**************************************************************************
    public Site getSite(String name) throws Exception {
        return new Site(name);
    }


  //**************************************************************************
  //** Site Class
  //**************************************************************************
    public class Site {
        private final String siteID;

        public Site(String name) throws Exception {
            String url = "/sites/" + host + ":/sites/" + name + "?select=id";
            JSONObject siteInfo = conn.getResponse(url);
            siteID = siteInfo.get("id").toString();
        }

        public ArrayList<Drive> getDrives() throws Exception {
            ArrayList<Drive> drives = new ArrayList<>();
            String url = "/sites/" + siteID + "/drives";
            for (JSONValue v : conn.getResponse(url).get("value").toJSONArray()){
                drives.add(new Drive(v.toJSONObject()));
            }
            return drives;
        }

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
    public class Drive {
        private final JSONObject json;

        private Drive(JSONObject json){
            this.json = json;
        }

        public String getName(){
            return json.get("name").toString();
        }

        public String getID(){
            return json.get("id").toString();
        }

        public ArrayList<Item> getChildren() throws Exception {
            ArrayList<Item> items = new ArrayList<>();
            String url = "/drives/" + getID() + "/root/children";
            for (JSONValue v : conn.getResponse(url).get("value").toJSONArray()){
                items.add(new Item(v.toJSONObject(), this));
            }
            return items;
        }

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

        public String toString(){
            return json.toString(4);
        }
    }


  //**************************************************************************
  //** Item Class
  //**************************************************************************
  /** Used to represent a file or folder on a drive
   */
    public class Item {
        private final JSONObject json;
        private final Drive drive;

        private Item(JSONObject json, Drive drive){
            this.json = json;
            this.drive = drive;
        }

        public String getName(){
            return json.get("name").toString();
        }

        public String getID(){
            return json.get("id").toString();
        }

        public boolean isFolder(){
            return json.has("folder");
        }

        public JSONValue get(String key){
            return json.get(key);
        }

        public ArrayList<Item> getChildren() throws Exception {
            ArrayList<Item> items = new ArrayList<>();
            if (!isFolder()) return items; //throw Error?

            String url = "/drives/" + drive.getID() + "/items/" + getID() + "/children";
            for (JSONValue v : conn.getResponse(url).get("value").toJSONArray()){
                items.add(new Item(v.toJSONObject(), drive));
            }
            return items;
        }

        public boolean download(String path) throws Exception {
            return download(new javaxt.io.File(path));
        }


        public boolean download(javaxt.io.File file) throws Exception {
            if (isFolder()) return false;

          //Get date
            javaxt.utils.Date lastModified = new javaxt.utils.Date(
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

        public String toString(){
            return json.toString(4);
        }
    }

}