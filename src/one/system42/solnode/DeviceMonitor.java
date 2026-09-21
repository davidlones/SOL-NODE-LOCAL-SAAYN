package one.system42.solnode;
import android.content.*;
import android.os.*;
import android.app.ActivityManager;
import android.net.*;
import org.json.*;
public final class DeviceMonitor {
 private final Context context;
 public DeviceMonitor(Context c){context=c;}
 public JSONObject sample() throws JSONException {
  JSONObject j=new JSONObject(); Intent b=context.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
  j.put("model",Build.MODEL); j.put("android",Build.VERSION.RELEASE); j.put("api",Build.VERSION.SDK_INT);
  int level=b==null?-1:b.getIntExtra(BatteryManager.EXTRA_LEVEL,-1), scale=b==null?0:b.getIntExtra(BatteryManager.EXTRA_SCALE,0);
  j.put("battery_percent",level>=0&&scale>0?(level*100/scale):JSONObject.NULL);
  j.put("charging",b!=null&&b.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0);
  StatFs fs=new StatFs(context.getFilesDir().getPath()); j.put("storage_available_bytes",fs.getAvailableBytes());
  ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo(); ((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(m);
  j.put("ram_available_bytes",m.availMem); j.put("uptime_ms",SystemClock.elapsedRealtime());
  ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE); NetworkInfo n=cm.getActiveNetworkInfo();
  j.put("network_connected",n!=null&&n.isConnected()); j.put("network_type",n==null?"none":n.getTypeName());
  j.put("camera","not owned / no permission"); j.put("inference",new InferenceManager(context).available()?"model cache readable / native bridge":"model cache unavailable");
  j.put("sampled_at_ms",System.currentTimeMillis()); return j;
 }
}
