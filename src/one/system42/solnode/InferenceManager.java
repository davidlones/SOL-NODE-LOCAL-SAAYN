package one.system42.solnode;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
/** Fixed executable + argument vector, never a shell. Existing tensor files are read only. */
public final class InferenceManager {
 public static final String CACHE="/sdcard/qpython/scripts/saayn_s4/cache/gpt2-small-npy";
 private JSONObject cachedVocab,cachedScores; private JSONArray cachedTokens;
 private final Context context; private volatile Process active; private volatile int generation;
 public InferenceManager(Context c){context=c;}
 private String read(InputStream in,int limit)throws IOException{try{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("Data exceeds limit");out.write(b,0,n);}return out.toString("UTF-8");}finally{in.close();}}
 private String json(String name)throws Exception{return read(new FileInputStream(new File(CACHE,name)),8000000);}
 public boolean available(){return new File(CACHE,"model_wte.npy").canRead();}
 public int prepare(){return ++generation;}
 public void cancel(){++generation;Process p=active;if(p!=null)p.destroy();}
 private ArrayList<Integer> encode(String prompt)throws Exception{
  if(!prompt.matches("[\\x09\\x0a\\x0d\\x20-\\x7e]{1,8192}"))throw new IllegalArgumentException("Local tokenizer accepts ASCII text up to 8,192 characters");
  if(cachedVocab==null)cachedVocab=new JSONObject(json("token_to_id.json"));if(cachedScores==null)cachedScores=new JSONObject(json("bpe_scores.json"));JSONObject vocab=cachedVocab,scores=cachedScores;
  ArrayList<Integer> ids=new ArrayList<Integer>();Matcher words=Pattern.compile("'s|'t|'re|'ve|'m|'ll|'d| ?[A-Za-z_]+| ?[0-9]+| ?[^\\sA-Za-z_0-9]+|\\s+(?!\\S)|\\s+").matcher(prompt);
  while(words.find()){String word=words.group();ArrayList<String> pieces=new ArrayList<String>();for(int i=0;i<word.length();i++)pieces.add(word.substring(i,i+1));
   for(int step=0;step<word.length();step++){int best=-1,score=-1;for(int i=0;i<pieces.size()-1;i++){int s=scores.optInt(pieces.get(i)+"\u0000"+pieces.get(i+1),-1);if(s>score){score=s;best=i;}}if(best<0)break;pieces.set(best,pieces.get(best)+pieces.get(best+1));pieces.remove(best+1);}
   for(String piece:pieces){if(!vocab.has(piece))throw new IOException("Unsupported tokenizer character");ids.add(vocab.getInt(piece));}
  }
  if(ids.isEmpty())throw new IOException("No tokens");return ids;
 }
 private JSONObject next(ArrayList<Integer> ids,JSONArray tokens,int ticket,long limit)throws Exception{
  File executable=new File(context.getFilesDir(),"saayn-native-tiled-v1");
  if(!executable.exists()){InputStream in=context.getAssets().open("saayn_s4_native");FileOutputStream out=new FileOutputStream(executable);try{byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}finally{in.close();out.close();}if(!executable.setExecutable(true,true))throw new IOException("Unable to enable private helper");}
  ArrayList<String> args=new ArrayList<String>();args.add(executable.getAbsolutePath());args.add(CACHE);for(int i=Math.max(0,ids.size()-16);i<ids.size();i++)args.add(String.valueOf(ids.get(i)));
  if(ticket!=generation)throw new IOException("Cancelled");final Process process=new ProcessBuilder(args).start();active=process;if(ticket!=generation)process.destroy();final StringBuilder progress=new StringBuilder();
  Thread stderr=new Thread(new Runnable(){public void run(){try{progress.append(read(process.getErrorStream(),16384));}catch(IOException e){progress.append("Progress unavailable");}}});stderr.start();
  Timer timer=new Timer(true);timer.schedule(new TimerTask(){public void run(){process.destroy();}},Math.max(1,Math.min(120000,limit)));
  try{String output=read(process.getInputStream(),16384);int exit=process.waitFor();stderr.join(2000);if(exit!=0)throw new IOException("Native worker exit "+exit+" (cancelled, timeout or model error)");JSONObject result=new JSONObject(output);result.put("token",tokens.getString(result.getInt("token_id")));result.put("progress",progress.toString());result.put("context_tokens",Math.min(16,ids.size()));result.put("provenance","LOCAL / native ARM");return result;}finally{timer.cancel();process.destroy();active=null;}
 }
 private JSONArray tokens()throws Exception{if(cachedTokens==null)cachedTokens=new JSONArray(json("id_to_token.json"));return cachedTokens;}
 public synchronized JSONObject run(String prompt,int ticket)throws Exception{
  return next(encode(prompt),tokens(),ticket,120000);
 }
 public interface Listener {void update(String text,int count);}
 public synchronized JSONObject generate(String prompt,int ticket,Listener listener)throws Exception{
  ArrayList<Integer> ids=encode(prompt);JSONArray tokens=tokens();
  StringBuilder text=new StringBuilder();JSONArray generated=new JSONArray();long start=android.os.SystemClock.elapsedRealtime();String reason="token_limit";
  for(int i=0;i<24;i++){
   if(ticket!=generation)throw new IOException("Cancelled");
   long left=600000-(android.os.SystemClock.elapsedRealtime()-start);if(left<=0){reason="time_limit";break;}
   JSONObject step=next(encode(prompt+text.toString()),tokens,ticket,left);int id=step.getInt("token_id");if(id==50256){reason="end_of_text";break;}
   ids.add(id);generated.put(id);text.append(tokens.getString(id));int boundary=DialoguePolicy.stop(text.toString());if(boundary>=0){text.setLength(boundary);reason="role_stop";listener.update(text.toString(),generated.length());break;}listener.update(text.toString(),generated.length());
  }
  JSONObject result=new JSONObject();result.put("text",text.toString());result.put("token_ids",generated);result.put("generated_tokens",generated.length());result.put("stop_reason",reason);result.put("elapsed_seconds",(android.os.SystemClock.elapsedRealtime()-start)/1000.0);result.put("context_window",16);return result;
 }

}
