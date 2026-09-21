package one.system42.solnode;
import java.io.*;
/** Bounded incremental SSE framing; JSON interpretation belongs to the contract client. */
public final class SseReader {
 public interface Consumer { boolean event(String data) throws Exception; }
 public static void read(Reader input,Consumer consumer) throws Exception {
  BufferedReader r=new BufferedReader(input);StringBuilder event=new StringBuilder();String line;
  while((line=r.readLine())!=null){
   if(line.length()>65536)throw new IOException("SSE line limit exceeded");
   if(line.length()==0){if(event.length()>0){event.setLength(event.length()-1);if(!consumer.event(event.toString()))return;event.setLength(0);}continue;}
   if(line.startsWith("data:")){String data=line.substring(5);if(data.startsWith(" "))data=data.substring(1);event.append(data).append('\n');if(event.length()>262144)throw new IOException("SSE event limit exceeded");}
  }
  throw new EOFException("Stream ended before a completion event");
 }
}
