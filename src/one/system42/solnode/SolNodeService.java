package one.system42.solnode;
import android.app.Service;
import android.content.Intent;
import android.os.*;
public final class SolNodeService extends Service {
 public final class LocalBinder extends Binder { public ToolRegistry tools(){return registry;} public InferenceManager inference(){return worker;} }
 private ToolRegistry registry; private InferenceManager worker; private final LocalBinder binder=new LocalBinder();
 public void onCreate(){super.onCreate();registry=new ToolRegistry(new DeviceMonitor(this));worker=new InferenceManager(this);}
 public IBinder onBind(Intent i){return binder;}
 public void onDestroy(){worker.cancel();super.onDestroy();}
 // Bound lifetime only in v0.1. No polling, boot receiver, camera or wakelock.
}
