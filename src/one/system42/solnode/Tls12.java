package one.system42.solnode;
import javax.net.ssl.*;
import java.net.*;
import java.io.*;
public final class Tls12 extends SSLSocketFactory {
 private final SSLSocketFactory base;
 public Tls12(X509TrustManager trust) throws Exception {SSLContext c=SSLContext.getInstance("TLS");c.init(null,new TrustManager[]{trust},null);base=c.getSocketFactory();}
 private Socket enable(Socket socket,String host){if(socket instanceof SSLSocket){SSLSocket s=(SSLSocket)socket;try{s.getClass().getMethod("setHostname",String.class).invoke(s,host);}catch(Exception ignored){}for(String p:s.getSupportedProtocols())if(p.equals("TLSv1.2")){s.setEnabledProtocols(new String[]{"TLSv1.2"});break;}}return socket;}
 public String[] getDefaultCipherSuites(){return base.getDefaultCipherSuites();}public String[] getSupportedCipherSuites(){return base.getSupportedCipherSuites();}
 public Socket createSocket(Socket s,String h,int p,boolean close)throws IOException{return enable(base.createSocket(s,h,p,close),h);}
 public Socket createSocket(String h,int p)throws IOException{return enable(base.createSocket(h,p),String.valueOf(h));}
 public Socket createSocket(String h,int p,InetAddress l,int lp)throws IOException{return enable(base.createSocket(h,p,l,lp),String.valueOf(h));}
 public Socket createSocket(InetAddress h,int p)throws IOException{return enable(base.createSocket(h,p),String.valueOf(h));}
 public Socket createSocket(InetAddress h,int p,InetAddress l,int lp)throws IOException{return enable(base.createSocket(h,p,l,lp),String.valueOf(h));}
}
