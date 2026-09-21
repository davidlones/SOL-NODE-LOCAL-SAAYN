import one.system42.solnode.DialoguePolicy;
public class DialoguePolicyTest {
 public static void main(String[] args){
  if(!DialoguePolicy.PRIMER.endsWith("Assistant: Answer the user's message directly, then stop. "))throw new AssertionError("Primer changed");
  for(String marker:DialoguePolicy.STOPS)if(DialoguePolicy.stop("Hello"+marker+"ignored")!=5)throw new AssertionError(marker);
  if(DialoguePolicy.stop("A normal answer.")!=-1)throw new AssertionError("Unexpected stop");
  if(!DialoguePolicy.clean(" Assistant: Hello. User ").equals("Hello."))throw new AssertionError("Cleanup");
  if(!DialoguePolicy.content("Hello\nthere\u001b[31m!").equals("Hello there!"))throw new AssertionError("Sanitization");
  if(!DialoguePolicy.bad("Of mine, yes"))throw new AssertionError("Bad continuation");
  System.out.println("Existing SAAYN primer, role stops, cleanup and sanitization passed");
 }
}
