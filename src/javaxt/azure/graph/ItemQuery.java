package javaxt.azure.graph;
import java.util.*;
import javaxt.json.*;

//******************************************************************************
//**  ItemQuery
//******************************************************************************
/**
 *   Base class for query options for folders and calendars (e.g. select,
 *   filter, orderby, top, etc).
 *
 *   <p>Note that this library used <code>IdType="ImmutableId"</code> by
 *   default, which the Graph ignores for <code>$search</code> queries (returns
 *   regular REST ids instead). As a workaround, search results are translated
 *   to immutable ids before they're returned, using Graph's
 *   <code>user-translateexchangeids</code> id translation function. This
 *   requires the <code>User.Read.All</code> application permission
 *   (<code>User.ReadBasic.All</code> delegated).</p>
 *
 ******************************************************************************/

public abstract class ItemQuery<T extends ItemQuery<T>> {

    private Integer top;
    private Integer skip;
    private Integer limit;
    private final List<String> select = new ArrayList<>();
    private final List<String> expandExtensions = new ArrayList<>();
    private String filter;
    private String orderBy;
    private String search;

  /** Default page size ($top) */
    private static final int DEFAULT_TOP = 50;

  /** Maximum number of ids per id translation request (Graph limit) */
    private static final int MAX_TRANSLATE_BATCH = 1000;


    protected ItemQuery(){}

    @SuppressWarnings("unchecked")
    private T self(){ return (T) this; }


  //**************************************************************************
  //** getTop
  //**************************************************************************
  /** Returns the page size (<code>$top</code>). Default is 50.
   */
    public Integer getTop(){
        return top;
    }


  //**************************************************************************
  //** setTop
  //**************************************************************************
  /** Sets the page size (<code>$top</code>). Default is 50.
   */
    public T setTop(Integer top){ this.top = top; return self(); }


  //**************************************************************************
  //** select
  //**************************************************************************
  /** Adds one or more properties to the <code>$select</code> list.
   */
    public T select(String... properties){
        if (properties!=null){
            for (String p : properties){
                if (p!=null) select.add(p);
            }
        }
        return self();
    }


  //**************************************************************************
  //** expandExtension
  //**************************************************************************
  /** Adds one or more open-extension names to <code>$expand</code>.
   */
    public T expandExtension(String... extensionNames){
        if (extensionNames!=null){
            for (String n : extensionNames){
                if (n!=null) expandExtensions.add(n);
            }
        }
        return self();
    }

  //**************************************************************************
  //** getFilter
  //**************************************************************************
  /** Returns the <code>$filter</code> expression.
   */
    public String getFilter(){
        return filter;
    }


  //**************************************************************************
  //** setFilter
  //**************************************************************************
  /** Sets the <code>$filter</code> expression. Ignored when searching.
   */
    public T setFilter(String filter){ this.filter = filter; return self(); }


  //**************************************************************************
  //** setOrderBy
  //**************************************************************************
  /** Sets the <code>$orderby</code> expression. Ignored when searching.
   */
    public T setOrderBy(String orderBy){ this.orderBy = orderBy; return self(); }


  //**************************************************************************
  //** setSkip
  //**************************************************************************
  /** Sets the number of items to skip (<code>$skip</code>). Use with
   *  setTop() and setLimit() to fetch a single page at an offset. Ignored
   *  when searching (not supported by Graph). Made public by subclasses that
   *  support it.
   */
    protected T setSkip(Integer skip){ this.skip = skip; return self(); }


  //**************************************************************************
  //** setLimit
  //**************************************************************************
  /** Sets the maximum number of items to return in total (paging stops once
   *  it's reached). Null for no limit (the default). Made public by
   *  subclasses that support it.
   */
    protected T setLimit(Integer limit){ this.limit = limit; return self(); }


  //**************************************************************************
  //** setSearch
  //**************************************************************************
  /** Sets the text to search for (<code>$search</code>, KQL). The text is
   *  quoted and escaped. The filter, orderby, and skip options are ignored
   *  while searching (Graph doesn't allow $orderby or $skip with $search).
   *  Results use immutable ids if the connection does (see the class notes).
   *  Made public by subclasses that support it.
   */
    protected T setSearch(String search){ this.search = search; return self(); }


  //**************************************************************************
  //** apply
  //**************************************************************************
  /** Adds the query options to a request URL.
   */
    protected void apply(Connection.Query q){
        q.set("$top", top!=null ? top : DEFAULT_TOP);
        if (!select.isEmpty()) q.set("$select", String.join(",", select));
        if (search!=null){
            //$search disables $filter, $orderby, $skip and $count
            q.set("$search", quoteSearch(search));
        }
        else{
            if (filter!=null) q.set("$filter", filter);
            if (orderBy!=null) q.set("$orderby", orderBy);
            if (skip!=null && skip>0) q.set("$skip", skip);
        }
        String expand = Item.expandExtensions(expandExtensions);
        if (expand!=null) q.set("$expand", expand);
    }


  //**************************************************************************
  //** getItems
  //**************************************************************************
  /** Fetches the items for a request URL built with apply(). Follows paging
   *  until the limit (if any) is reached. Search results are translated to
   *  immutable ids if the connection uses them.
   *  @param userID Id of the mailbox that contains the items.
   *  @param headers Optional extra request headers (e.g. a Prefer header).
   */
    protected List<JSONObject> getItems(Connection conn, String userID, String url, Map<String, String> headers)
        throws GraphException {

        int max = (limit==null) ? Integer.MAX_VALUE : limit;
        ArrayList<JSONObject> items = new ArrayList<>();
        try{
            for (JSONObject json : conn.getPage(url, headers)){
                items.add(json);
                if (items.size() >= max) break;
            }
        }
        catch(RuntimeException e){
            if (e.getCause() instanceof GraphException){
                throw (GraphException) e.getCause();
            }
            throw e;
        }


      //Graph ignores Prefer: IdType="ImmutableId" for $search; translate the
      //ids so search results use the same (immutable) ids as everything else
        if (search!=null) toImmutableIds(conn, userID, items);
        return items;
    }


  //**************************************************************************
  //** quoteSearch
  //**************************************************************************
  /** Returns a <code>$search</code> value: the given text in double quotes,
   *  with backslashes and double quotes escaped (Graph rejects unescaped
   *  ones with a syntax error).
   */
    private static String quoteSearch(String search){
        if (search==null) return null;
        return "\"" + search.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }


  //**************************************************************************
  //** toImmutableIds
  //**************************************************************************
  /** Replaces the regular REST ids in the given items (Graph JSON objects
   *  with an "id") with immutable ids, if the connection uses immutable ids.
   *  Items whose id can't be translated (e.g. the item was deleted after the
   *  search) are removed rather than returned with an id in the wrong
   *  format.
   */
    private static void toImmutableIds(Connection conn, String userID, List<JSONObject> items)
        throws GraphException {

        if (items==null || items.isEmpty() || !conn.usesImmutableIds()) return;

        List<String> ids = new ArrayList<>();
        for (JSONObject item : items){
            String id = item.get("id").toString();
            if (id!=null) ids.add(id);
        }

        Map<String, String> map;
        try{
            map = getImmutableIds(conn, userID, ids);
        }
        catch(GraphException e){
            throw new GraphException("Failed to convert search result ids to immutable ids " +
            "(requires the User.Read.All application permission): " + e.getMessage(), e);
        }

        Iterator<JSONObject> it = items.iterator();
        while (it.hasNext()){
            JSONObject item = it.next();
            String target = map.get(item.get("id").toString());
            if (target==null) it.remove();
            else item.set("id", target);
        }
    }


  //**************************************************************************
  //** getImmutableIds
  //**************************************************************************
  /** Translates regular REST ids of items in a mailbox (messages, events,
   *  contacts, tasks, attachments) to immutable ids, using Graph's
   *  translateExchangeIds function, up to 1,000 ids per request. See
   *  {@link User#getImmutableIds}.
   *  @return Map of each given id to its immutable id, in the order given.
   *  Ids that couldn't be translated are not in the map.
   */
    protected static Map<String, String> getImmutableIds(Connection conn, String userID, Collection<String> ids)
        throws GraphException {

        if (conn==null) throw new IllegalArgumentException("conn is required");
        if (userID==null) throw new IllegalArgumentException("userID is required");

        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (ids==null || ids.isEmpty()) return result;

      //Remove duplicates and nulls
        List<String> unique = new ArrayList<>(new LinkedHashSet<>(ids));
        unique.removeIf(Objects::isNull);

        String url = "/users/" + userID + "/translateExchangeIds";
        HashMap<String, String> translated = new HashMap<>();
        for (int i=0; i<unique.size(); i+=MAX_TRANSLATE_BATCH){
            List<String> batch = unique.subList(i, Math.min(unique.size(), i+MAX_TRANSLATE_BATCH));

            JSONArray inputIds = new JSONArray();
            for (String id : batch) inputIds.add(id);
            JSONObject payload = new JSONObject();
            payload.set("inputIds", inputIds);
            payload.set("sourceIdType", "restId");
            payload.set("targetIdType", "restImmutableEntryId");

            JSONObject response = conn.post(url, payload);
            JSONArray value = (response==null) ? null : response.get("value").toJSONArray();
            if (value==null) continue;

            for (JSONValue v : value){
                JSONObject r = v.toJSONObject();
                if (r==null) continue;
                String sourceId = r.get("sourceId").toString();
                String targetId = r.get("targetId").toString(); //absent if the translation failed
                if (sourceId!=null && targetId!=null) translated.put(sourceId, targetId);
            }
        }

        for (String id : unique){
            String target = translated.get(id);
            if (target!=null) result.put(id, target);
        }
        return result;
    }
}
