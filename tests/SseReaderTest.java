import one.system42.solnode.SseReader;
import java.io.*;
import java.util.*;
public final class SseReaderTest {
 public static void main(String[] args)throws Exception{
  final List<String> got=new ArrayList<String>();
  SseReader.Consumer c=new SseReader.Consumer(){public boolean event(String s){got.add(s);return !s.equals("done");}};
  SseReader.read(new StringReader(": heartbeat\r\ndata: first\r\ndata: second\r\n\r\ndata: done\r\n\r\n"),c);
  if(got.size()!=2||!got.get(0).equals("first\nsecond"))throw new AssertionError(got);
  try{SseReader.read(new StringReader("data: unfinished\n\n"),c);throw new AssertionError("Silent truncation");}catch(EOFException expected){}
  char[] huge=new char[65537];Arrays.fill(huge,'a');try{SseReader.read(new StringReader("data:"+new String(huge)),c);throw new AssertionError("No bound");}catch(IOException expected){}
  System.out.println("SSE framing, multiline, CRLF, incomplete stream and size limits passed");
 }
}
