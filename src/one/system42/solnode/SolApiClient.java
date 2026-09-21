package one.system42.solnode;
import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.json.*;
public final class SolApiClient {
 public static final String BASE=EndpointConfig.BASE;
 private final android.content.Context context; public SolApiClient(android.content.Context c){context=c.getApplicationContext();}
 private volatile Call active; private volatile boolean cancelled;
 public interface Events {void event(JSONObject data);}
 public void cancel(){cancelled=true;Call c=active;if(c!=null)c.cancel();}
 public void chat(String message,String session,String profile,boolean persist,final Events events) throws Exception {
  if(BASE.length()==0)throw new IOException("Public SOL endpoint not configured in this build; local tools remain available");
  com.google.android.gms.security.ProviderInstaller.installIfNeeded(context);
  TrustManagerFactory tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());KeyStore anchors=KeyStore.getInstance(KeyStore.getDefaultType());anchors.load(null,null);
  for(String name:new String[]{"gts-root-r4.pem","isrg-root-x1.pem"}){InputStream root=context.getAssets().open(name);try{anchors.setCertificateEntry(name,java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(root));}finally{root.close();}}tm.init(anchors);
  X509TrustManager trust=null;for(TrustManager m:tm.getTrustManagers())if(m instanceof X509TrustManager)trust=(X509TrustManager)m;
  if(trust==null)throw new IOException("No trust manager");

  OkHttpClient client=new OkHttpClient.Builder().sslSocketFactory(new Tls12(trust),trust)
   .connectionSpecs(Arrays.asList(new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).tlsVersions(TlsVersion.TLS_1_2).build()))
   .protocols(Arrays.asList(Protocol.HTTP_1_1)).callTimeout(180,TimeUnit.SECONDS).connectTimeout(15,TimeUnit.SECONDS).readTimeout(90,TimeUnit.SECONDS).retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build();
  JSONObject j=new JSONObject();j.put("message",message);j.put("session",session);j.put("profile",profile);j.put("persist",persist);j.put("stream",true);j.put("allow_actions",false);j.put("allow_fallback_answer",false);
  Request request=new Request.Builder().url(BASE+"/api/chat").header("Accept","text/event-stream").header("User-Agent","SOLNode/0.3 Android18").post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"),j.toString())).build();
  Call call=client.newCall(request);active=call;if(cancelled)call.cancel();
  Response response=null;
  try{response=call.execute();if(response.code()!=200)throw new IOException("SOL HTTP "+response.code()+"; Retry-After: "+response.header("Retry-After","not specified")+". No automatic retry.");
   if(!response.header("Content-Type","").contains("text/event-stream"))throw new IOException("Expected SSE response");
   SseReader.read(response.body().charStream(),new SseReader.Consumer(){public boolean event(String raw)throws Exception{if(cancelled)throw new IOException("Cancelled");JSONObject e=new JSONObject(raw);events.event(e);return !e.optString("type").equals("done")&&!e.optString("type").equals("error");}});
  }finally{if(response!=null)response.close();active=null;client.connectionPool().evictAll();client.dispatcher().executorService().shutdown();}
 }
}
