package javaxt.azure.graph.sync;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

import javaxt.json.*;
import javaxt.utils.ThreadPool;
import static javaxt.utils.Console.console;


//******************************************************************************
//**  Sharepoint Sync Class
//******************************************************************************
/**
 *   Used to sync a document database in SharePoint with a local file system.
 *
 ******************************************************************************/

public class SharePoint {

    private JSONObject source;
    private ThreadPool pool;
    private int numThreads = 4;
    private AtomicLong lastEvent;
    private AtomicBoolean isRunning;
    private javaxt.utils.Timer timer;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Used to instantiate the sync service
   *  @param source Configuration information. Example:
   <pre>
    {
        "host" : "acme.sharepoint.com",
        "clientID" : "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        "tenantID" : "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        "secret" : "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
        "sites" : [
            {
                "site" : "Personnel",
                "folders" : [
                    {
                        "drive" : "Documents",
                        "path" : "Personnel"
                    }
                ],
                "localCache" : "/share/acme/personnel"
            }
        ],
        "sync" : {
            "startTime" : "22:00",
            "interval" : "8h"
        }
    }
   </pre>
   */
    public SharePoint(JSONObject source){
        this.source = source;
        this.pool = new ThreadPool(numThreads){
            public void process(Object obj){
                Object[] arr = (Object[]) obj;
                String relPath = (String) arr[0];
                javaxt.azure.graph.SharePoint.Drive drive = (javaxt.azure.graph.SharePoint.Drive) arr[1];
                javaxt.io.Directory localCache = (javaxt.io.Directory) arr[2];
                try{
                    downloadFolder(relPath, drive, localCache);
                }
                catch(Exception e){
                }
                lastEvent.set(System.currentTimeMillis());
            }
        };
        lastEvent = new AtomicLong(0);
        isRunning = new AtomicBoolean(false);
    }


  //**************************************************************************
  //** start
  //**************************************************************************
  /** Used to schedule a sync task using the "sync" config
   */
    public synchronized void start(){
        if (timer!=null) return;

        JSONObject sync = source.get("sync").toJSONObject();
        if (sync==null) return;

        String startTime = sync.get("startTime").toString();
        if (startTime==null) return;

        String interval = sync.get("interval").toString();
        if (interval==null) return;


      //Parse startTime

        Integer startHour = null;
        Integer startMinute = null;
        try{
            if (startTime.contains(":")){
                int idx = startTime.indexOf(":");
                startHour = Integer.parseInt(startTime.substring(0, idx));
                if (startHour<0 || startHour>23) throw new Exception();

                startMinute = Integer.parseInt(startTime.substring(idx+1));
                if (startMinute<0 || startMinute>59) throw new Exception();
            }
            else{
                startHour = Integer.parseInt(startTime);
            }
        }
        catch(Exception e){}
        javaxt.utils.Date startDate = new javaxt.utils.Date();
        startDate.removeTimeStamp();
        if (startHour!=null){
            startDate.add(startHour, "hours");
            if (startMinute!=null) startDate.add(startMinute, "minutes");
        }



      //Parse interval
        long _interval;
        try{
            interval = interval.toLowerCase();
            if (interval.contains("d")){
                interval = interval.substring(0, interval.indexOf("d")).trim();
                _interval = (Integer.parseInt(interval)*24)*60*60*1000;
            }
            else if (interval.contains("h")){
                interval = interval.substring(0, interval.indexOf("h")).trim();
                _interval = Integer.parseInt(interval)*60*60*1000;
            }
            else if (interval.contains("m")){
                interval = interval.substring(0, interval.indexOf("m")).trim();
                _interval = Integer.parseInt(interval)*60*1000;
            }
            else{
                throw new Exception();
            }
        }
        catch(Exception e){
            return;
        }


        javaxt.utils.Date now = new javaxt.utils.Date();
        while (startDate.isBefore(now)){
            startDate = new javaxt.utils.Date(startDate.getTime()+_interval);
        }




      //Schedule task
        SharePoint me = this;
        timer = new javaxt.utils.Timer();
        timer.scheduleAtFixedRate( new TimerTask(){
            public void run(){
                try{
                    console.log("Starting new run at " + new javaxt.utils.Date());
                    me.run();
                }
                catch(Exception e){
                    //e.printStackTrace();
                }
            }
        }, startDate.getDate(), _interval);

    }


  //**************************************************************************
  //** cancel
  //**************************************************************************
  /** Used to cancel the current sync task previously scheduled via the
   *  start() method
   */
    public synchronized void cancel(){
        if (timer==null) return;
        timer.cancel();
        timer = null;
    }


  //**************************************************************************
  //** run
  //**************************************************************************
  /** Used to execute a sync task. Will return immediately if another sync
   *  task is running.
   */
    public void run() throws Exception {


      //Check whether we are in the middle of a run
        if (isRunning.get()){
            console.log("Aborting run at " + new javaxt.utils.Date() +
            ". Another task is still running.");
            return;
        }


        isRunning.set(true);
        javaxt.utils.Date startDate = new javaxt.utils.Date();


      //Start threads
        pool.start();


      //Start timer task to watch for lastEvent. If the lastEvent was more than
      //5 minutes ago, we can assume that the threads are done.
        lastEvent.set(0);
        javaxt.utils.Timer timer = new javaxt.utils.Timer();
        timer.scheduleAtFixedRate(
            new TimerTask(){
                public void run(){
                    long maxIdle = 5*60*1000; //5 minutes


                    long t = lastEvent.get();
                    long d = System.currentTimeMillis()-t;
                    //console.log("Last Event: ", new javaxt.utils.Date(t));


                    if (d>maxIdle){
                        //console.log("Stopping threads. Last event was " + StringUtils.getElapsedTime(t) + " ago");
                        pool.done();
                        timer.cancel();
                    }
                }
            },
            2*60*1000, //2 minute delay
            5*60*1000 //run every 5 minutes
        );



      //Get connection info
        javaxt.azure.graph.Connection conn = new javaxt.azure.graph.Connection(
            source.get("tenantID").toString(),
            source.get("clientID").toString(),
            source.get("secret").toString()
        );


      //Get SharePoint host
        javaxt.azure.graph.SharePoint sharepoint = new javaxt.azure.graph.SharePoint(
        source.get("host").toString(), conn);


      //Get sites
        for (JSONValue s : source.get("sites").toJSONArray()){
            JSONObject site = s.toJSONObject();

          //Get SharePoint site
            String siteName = site.get("site").toString();
            javaxt.azure.graph.SharePoint.Site st = sharepoint.getSite(siteName);


          //Get cache directory
            javaxt.io.Directory localCache = new javaxt.io.Directory(
            site.get("localCache").toString());


            for (JSONValue f : site.get("folders").toJSONArray()){
                JSONObject folder = f.toJSONObject();
                javaxt.azure.graph.SharePoint.Drive drive = st.getDrive(folder.get("drive").toString());
                String path = folder.get("path").toString();
                pool.add(new Object[]{path, drive, localCache});
                //downloadFolder(path, drive, localCache);
            }
        }


      //Wait for all the threads to finish
        pool.join();



      //Delete empty folders
        for (JSONValue site : source.get("sites").toJSONArray()){

            javaxt.io.Directory localCache = new javaxt.io.Directory(
            site.get("localCache").toString());

            for (Object o : localCache.getChildren(true)){
                if (o instanceof javaxt.io.Directory){
                    javaxt.io.Directory dir = (javaxt.io.Directory) o;
                    if (dir.isEmpty()){
                        console.log("delete", dir);
                    }
                }
            }
        }


      //Update status
        isRunning.set(false);


        javaxt.utils.Date endDate = new javaxt.utils.Date();
        console.log("Completed run at " + endDate + ". Synced in " +
        endDate.compareTo(startDate, "minutes") + " minutes");
    }


  //**************************************************************************
  //** downloadFolder
  //**************************************************************************
    private void downloadFolder(String relPath,
        javaxt.azure.graph.SharePoint.Drive drive,
        javaxt.io.Directory localCache) throws Exception {

        try{
            if (relPath.startsWith("/")) relPath = relPath.substring(1);
            if (relPath.endsWith("/")) relPath = relPath.substring(0, relPath.length()-1);
            String[] path = relPath.split("/");


          //Download items
            lastEvent.set(System.currentTimeMillis());
            javaxt.azure.graph.SharePoint.Item parentFolder = drive.getFolder(path);
            HashSet<String> files = new HashSet<>();
            for (javaxt.azure.graph.SharePoint.Item item : parentFolder.getChildren()){
                lastEvent.set(System.currentTimeMillis());
                if (item.isFolder()){
                    String folderName = item.getName();
                    javaxt.io.Directory dir = new javaxt.io.Directory(localCache.toString() + folderName);
                    pool.add(new Object[]{relPath + "/" + item.getName(), drive, dir});
                    //downloadFolder(relPath + "/" + item.getName(), drive, dir);
                }
                else{
                    javaxt.io.File file = downloadFile(item, localCache);
                    if (file!=null){
                        files.add(file.toFile().getCanonicalPath()); //vs getAbsoluteFile()
                    }
                }
            }



          //Delete local files that may have been deleted, moved, or renamed in SharePoint
            HashSet<javaxt.io.File> deletions = new HashSet<>();
            for (javaxt.io.File file : localCache.getFiles()){
                if (!files.contains(file.toFile().getCanonicalPath())){ //vs getAbsoluteFile()
                    deletions.add(file);
                }
            }
            for (javaxt.io.File file : deletions){
                console.log("DELETE: ", file);
                file.delete();
                notify("delete", file);
            }

        }
        catch(Exception e){
            console.log("failed to download folder " + relPath);
            e.printStackTrace();
        }

        lastEvent.set(System.currentTimeMillis());
    }


  //**************************************************************************
  //** downloadFile
  //**************************************************************************
    private javaxt.io.File downloadFile(javaxt.azure.graph.SharePoint.Item item,
        javaxt.io.Directory localCache) throws Exception {

        javaxt.io.File file = new javaxt.io.File(localCache, item.getName());
        int maxAttempts = 5;
        for (int i=0; i<5; i++){
            try {
                boolean downloaded = item.download(file);
                if (downloaded){
                    console.log(file);
                    notify("create", file);
                }
                lastEvent.set(System.currentTimeMillis());
                return file;
            }
            catch (Exception e){
                lastEvent.set(System.currentTimeMillis());
                if (e instanceof java.net.SocketException){
                    if (i<maxAttempts-1){
                        Thread.sleep(1500);
                    }
                    else{
                        console.log("failed to download " + file);
                        //throw e;
                        e.printStackTrace();
                    }
                }
                else{
                    console.log("failed to download " + file);
                    //throw e;
                    e.printStackTrace();
                }
            }
        }

        return null;
    }


  //**************************************************************************
  //** notify
  //**************************************************************************
  /** This method is called whenever a file is added or deleted during the
   *  sync process. This method can be safely overridden to process these
   *  events.
   */
    public void notify(String event, javaxt.io.File file){}

}