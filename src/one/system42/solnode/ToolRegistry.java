package one.system42.solnode;
import org.json.*;
public final class ToolRegistry {
 private final DeviceMonitor monitor;
 public static final String[] IDS={"device.status","device.battery","device.storage","device.network"};
 public ToolRegistry(DeviceMonitor m){monitor=m;}
 public JSONObject execute(String id) throws JSONException {
  boolean permitted=false; for(String known:IDS) if(known.equals(id)) permitted=true;
  if(!permitted) throw new SecurityException("Capability unavailable: "+id);
  JSONObject all=monitor.sample(), out=new JSONObject();
  if(id.equals("device.status")) return all;
  String[] keys=id.equals("device.battery")?new String[]{"battery_percent","charging"}:id.equals("device.storage")?new String[]{"storage_available_bytes","ram_available_bytes"}:new String[]{"network_connected","network_type"};
  for(String k:keys)out.put(k,all.get(k));return out;
 }
}
