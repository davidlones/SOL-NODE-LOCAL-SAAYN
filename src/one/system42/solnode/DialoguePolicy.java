package one.system42.solnode;
/** Ported from saayn_chat.py and saayn_openai/engines.py; generated constants preserve the existing wrapper. */
public final class DialoguePolicy {
 public static final String PRIMER = "User: hello there Assistant: Hello. How can I help? User: hello friend Assistant: Hello friend. Good to hear from you. User: What are spreadsheets good for? Assistant: Spreadsheets are useful for organizing data, calculating formulas, and building models. User: Who are you in this conversation? Assistant: I am SOL, your AI assistant. User: Who is the user? Assistant: You are the human user. I am SOL. User: Are you the user? Assistant: No. I am SOL, your AI assistant. You are the user. User: Answer briefly: what should the assistant do next? Assistant: Answer the user's message directly, then stop. ";
 public static final String[] STOPS = new String[]{"User:"," User:","\nUser:"," User","\nUser"," Assistant:"," Assistant","\nAssistant:","System:"," System:","\nSystem:"," System","\nSystem"};
 public static final String[] BAD_PREFIXES = new String[]{"of mine,","of mine ","a very good friend"};
 public static String content(String text){return text.replaceAll("\\x1b\\[[0-?]*[ -/]*[@-~]", "").replaceAll("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f\\x7f-\\x9f]", "").replace('\r',' ').replace('\n',' ');}
 public static boolean bad(String text){String s=text.trim().toLowerCase(java.util.Locale.US);for(String p:BAD_PREFIXES)if(s.startsWith(p))return true;return false;}
 public static int stop(String text){int best=-1;for(String marker:STOPS){int i=text.indexOf(marker);if(i>=0&&(best<0||i<best))best=i;}return best;}
 public static String clean(String text){String s=text.trim();for(String p:new String[]{"Assistant:","assistant:"})if(s.startsWith(p))s=s.substring(p.length()).trim();for(String suffix:new String[]{" Assistant"," User"," System"})if(s.endsWith(suffix))s=s.substring(0,s.length()-suffix.length()).trim();return s;}
}
