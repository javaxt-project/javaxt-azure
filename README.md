# JavaXT Azure
The javaxt-azure library is used to connect to a Microsoft 365 tenant via the
Microsoft Graph API. It provides a small, Graph-native client for calendar,
contacts, mail, and SharePoint, built on `javaxt-core` with no other dependencies.


## Capabilities
- **Calendar** – list, create, update, and delete events (recurring series are
  expanded into occurrences via `calendarView`), meeting invitations and
  responses, all-day events, reminders, and incremental delta sync.
- **Contacts** – list, create, update, and delete contacts and contact folders,
  with email/phone/address helpers and delta sync.
- **Email** – read mail folders and messages, send mail, create drafts, reply,
  reply-all, forward, mark read, move, delete, and work with attachments.
- **SharePoint** – browse a site's document libraries (drives) and download files.

Under the hood the connection handles app-only (client-credentials)
authentication, automatic paging (`@odata.nextLink`), throttling/retry
(`Retry-After`), immutable ids, and open-extension metadata for stamping your own
identifiers on Graph items.


## Usage

### Connect and list upcoming calendar events

```java
import javaxt.azure.graph.*;

Connection conn = new Connection(tenantID, clientID, clientSecret);
User user = User.findByEmail("someone@contoso.com", conn);
Calendar calendar = user.getDefaultCalendar();

javaxt.utils.Date start = new javaxt.utils.Date();
javaxt.utils.Date end = start.clone().add(30, "days");
for (CalendarEvent event : calendar.getEvents(start, end)){
    System.out.println(event.getStart() + "  " + event.getSubject());
}
```

### Create a new event

```java
CalendarEvent event = new CalendarEvent();
event.setSubject("Lunch");
event.setStart(new javaxt.utils.Date("2026-09-25T16:00:00.000Z"));
event.setEnd(new javaxt.utils.Date("2026-09-25T17:00:00.000Z"));
calendar.createEvent(event);
```


## Permissions & tenant setup

The library authenticates with **app-only (client-credentials)** — the only mode
that has been tested. Register an application in the tenant and grant it the
following **application** permissions on Microsoft Graph, then grant **admin
consent**:

| Capability | Permission (application) |
|---|---|
| User lookup | `User.Read.All` |
| Calendar | `Calendars.ReadWrite` |
| Contacts | `Contacts.ReadWrite` |
| Mail (read / manage) | `Mail.ReadWrite` |
| Mail (send) | `Mail.Send` |
| Mailbox time zone | `MailboxSettings.Read` |
| SharePoint files | `Sites.Read.All` |

Use the narrower read-only variants where they suffice (e.g. `Calendars.Read`,
`Contacts.Read`, `Mail.Read`). For SharePoint, `Sites.Read.All` grants read access
across all site collections; if your tenant prefers least privilege, `Sites.Selected`
can scope access to specific sites instead.

Because these are **application** permissions, the app can reach **every** mailbox
in the tenant. To restrict that to a chosen set of mailboxes, apply an Exchange
Online **Application Access Policy** scoped to a mail-enabled security group:

```powershell
New-ApplicationAccessPolicy -AppId <clientId> `
  -PolicyScopeGroupId <security-group>@<tenant> `
  -AccessRight RestrictAccess -Description "javaxt-azure sync"

# verify a mailbox is (or isn't) in scope
Test-ApplicationAccessPolicy -AppId <clientId> -Identity someone@<tenant>
```

The Application Access Policy governs Exchange resources (mail, calendar,
contacts); SharePoint access is governed separately (e.g. via `Sites.Selected`
grants). Delegated (per-user) permissions are out of scope here — the connection
is built for app-only client credentials, though it is structured so delegated
token providers can be added later.


## License
All JavaXT libraries are free and open source released under a permissive MIT license.
This software comes with no guarantees or warranties. You may use this software in any open source or commercial project.
