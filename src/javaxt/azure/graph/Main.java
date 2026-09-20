package javaxt.azure.graph;
import java.util.*;
import java.nio.file.*;
import javaxt.json.*;

//******************************************************************************
//**  Main
//******************************************************************************
/**
 *   Small command-line utility used to validate tenant setup against Microsoft
 *   Graph. Reads Azure credentials from a config JSON file and lists calendars
 *   or events for a given mailbox.
 *
 *   <p>Usage:</p>
 *   <pre>
 *   java -jar javaxt-azure.jar -config config.json -user someone@contoso.com -list calendars
 *   java -jar javaxt-azure.jar -config config.json -user someone@contoso.com -list events \
 *        -start 2026-09-01 -end 2026-10-01
 *   </pre>
 *
 *   The config file must contain an <code>azure</code> object with
 *   <code>tenantID</code>, <code>clientID</code> and <code>secret</code>.
 *
 ******************************************************************************/

public class Main {

    public static void main(String[] inputs) throws Exception {

        HashMap<String, String> args = parseArgs(inputs);

        String configPath = args.get("-config");
        if (configPath==null){
            System.out.println("Usage: -config <json> [-user <upn>] -list calendars|events " +
                "[-start yyyy-MM-dd -end yyyy-MM-dd]");
            return;
        }

        JSONObject config = new JSONObject(new String(Files.readAllBytes(Paths.get(configPath)),
            java.nio.charset.StandardCharsets.UTF_8));
        JSONObject azure = config.get("azure").toJSONObject();

        Connection conn = new Connection(
            azure.get("tenantID").toString(),
            azure.get("clientID").toString(),
            azure.get("secret").toString());

        String list = args.get("-list");
        String upn = args.get("-user");

        if ("calendars".equalsIgnoreCase(list)){
            User user = User.getUser(upn, conn);
            for (Calendar cal : user.getCalendars()){
                System.out.println((cal.isDefaultCalendar() ? "* " : "  ") + cal.getName() +
                    "  [" + cal.getID() + "]");
            }
        }
        else if ("events".equalsIgnoreCase(list)){
            User user = User.getUser(upn, conn);
            Calendar cal = user.getDefaultCalendar();
            javaxt.utils.Date start = args.containsKey("-start")
                ? new javaxt.utils.Date(args.get("-start")) : new javaxt.utils.Date().add(-7, "days");
            javaxt.utils.Date end = args.containsKey("-end")
                ? new javaxt.utils.Date(args.get("-end")) : new javaxt.utils.Date().add(30, "days");

            List<CalendarEvent> events = cal.getEvents(start, end, new Calendar.EventQuery().setTextBody(true));
            System.out.println(events.size() + " event(s) between " + start.toISOString() + " and " + end.toISOString());
            for (CalendarEvent e : events){
                System.out.println("  " + e.getStart() + "  " + e.getSubject() + "  (" + e.getType() + ")");
            }
        }
        else if ("contacts".equalsIgnoreCase(list)){
            User user = User.getUser(upn, conn);
            ContactFolder folder = user.getDefaultContactFolder();
            List<Contact> contacts = folder.getContacts();
            System.out.println(contacts.size() + " contact(s):");
            for (Contact c : contacts){
                System.out.println("  " + c.getDisplayName() + "  " + c.getEmailAddresses() + "  " + c.getPhoneNumbers());
            }
        }
        else if ("folders".equalsIgnoreCase(list)){
            User user = User.getUser(upn, conn);
            for (ContactFolder f : user.getContactFolders()){
                System.out.println("  " + f.getDisplayName() + "  [" + f.getID() + "]");
            }
        }
        else if ("mailfolders".equalsIgnoreCase(list)){
            User user = User.getUser(upn, conn);
            for (EmailFolder f : user.getMailFolders()){
                System.out.println("  " + f.getDisplayName() + "  (total=" + f.getTotalItemCount() +
                    ", unread=" + f.getUnreadItemCount() + ")");
            }
        }
        else if ("inbox".equalsIgnoreCase(list) || "messages".equalsIgnoreCase(list)){
            User user = User.getUser(upn, conn);
            EmailFolder inbox = user.getMailFolder("inbox");
            List<Email> messages = inbox.getEmails(new EmailFolder.EmailQuery()
                .setTop(10).setLimit(10).setOrderBy("receivedDateTime desc")
                .select("subject","from","receivedDateTime","isRead"));
            System.out.println("Inbox: " + inbox.getTotalItemCount() + " total, " +
                inbox.getUnreadItemCount() + " unread. Latest:");
            for (Email m : messages){
                String from = (m.getFrom()==null) ? "?" : m.getFrom().getAddress();
                System.out.println("  " + (m.isRead() ? "  " : "* ") + m.getReceivedDateTime() +
                    "  " + m.getSubject() + "  <" + from + ">");
            }
        }
        else{
            System.out.println("Nothing to do. Specify -list calendars|events|contacts|folders|mailfolders|inbox");
        }
    }


  //**************************************************************************
  //** parseArgs
  //**************************************************************************
    private static HashMap<String, String> parseArgs(String[] inputs){
        HashMap<String, String> args = new HashMap<>();
        if (inputs!=null){
            for (int i=0; i<inputs.length; i++){
                String key = inputs[i];
                if (key.startsWith("-")){
                    String val = (i+1<inputs.length && !inputs[i+1].startsWith("-")) ? inputs[++i] : "";
                    args.put(key, val);
                }
            }
        }
        return args;
    }
}
