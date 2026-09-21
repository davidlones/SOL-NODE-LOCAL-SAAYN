package one.system42.solnode;
import android.content.*;
import org.json.*;
import java.util.*;
/** App-private bounded local history. Session IDs are identifiers, not credentials. */
public final class ConversationStore {
 private final SharedPreferences prefs;
 public ConversationStore(Context c){prefs=c.getSharedPreferences("conversations",0);}
 public String session(){String s=prefs.getString("current",null);return s==null?create():s;}
 public String create(){String s="solnode-"+UUID.randomUUID().toString();prefs.edit().putString("current",s).putString(s,"[]").commit();return s;}
 public void select(String s){prefs.edit().putString("current",s).commit();}
 public String[] sessions(){ArrayList<String> out=new ArrayList<String>();for(String k:prefs.getAll().keySet())if(k.startsWith("solnode-"))out.add(k);Collections.sort(out);return out.toArray(new String[out.size()]);}
 public JSONArray messages(String session){try{return new JSONArray(prefs.getString(session,"[]"));}catch(Exception e){return new JSONArray();}}
 public void append(String session,String role,String text){try{JSONArray old=messages(session),n=new JSONArray();for(int i=Math.max(0,old.length()-99);i<old.length();i++)n.put(old.get(i));JSONObject m=new JSONObject();m.put("role",role);m.put("text",text.length()>65536?text.substring(0,65536):text);n.put(m);prefs.edit().putString(session,n.toString()).commit();}catch(JSONException ignored){}}
 public String display(){JSONArray a=messages(session());StringBuilder b=new StringBuilder();for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);b.append(m.optString("role")).append("\n").append(m.optString("text")).append("\n\n");}return b.toString();}
}
