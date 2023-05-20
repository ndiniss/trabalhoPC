import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashSet;

public class DeadlockDetector extends Thread {

  public static final DeadlockDetector INSTANCE = new DeadlockDetector();

  public static void enable() {
    INSTANCE.start();
  }

  private DeadlockDetector() {
    super("DeadlockDetector");
    setDaemon(true);
  }
  
  @Override
  public void run() {
    // Initialize.
    // Note: this procedure is fragile and will not work
    // in all JVMs and all contexts of use.
    ThreadMXBean mxbean = ManagementFactory.getThreadMXBean();
    HashSet<String> ignore = new HashSet<>();
    for (ThreadInfo ti : mxbean.dumpAllThreads(false, false)) {
      ignore.add(ti.getThreadName());
    }
    ignore.remove("main");
    ignore.add("DestroyJavaVM");
    
    // Loop until a deadlock is detected
    boolean deadlock = false;
    while (! deadlock) {
      try {
        Thread.sleep(1000);
      }
      catch(InterruptedException e) {
        throw new RuntimeException("Unexpected interrupt");
      }
      deadlock = true;
      ArrayList<ThreadInfo> stucked = new ArrayList<>();
      for (ThreadInfo ti : mxbean.dumpAllThreads(false, false)) {
        if (ignore.contains(ti.getThreadName())) {
          continue; 
        }
        // D.print("%s -> %s",
        //         ti.getThreadName() + " " + ti.getThreadState());
        switch (ti.getThreadState()) {
          case BLOCKED:
          case WAITING:
            stucked.add(ti);
            break;
          default: 
            deadlock = false;
        }
      }
      if (deadlock) {
        System.err.println("Deadlock detected!");
        for (ThreadInfo ti : stucked) {
          System.err.printf("Stack trace for %s => %n", ti.getThreadName());
          StackTraceElement[] st = ti.getStackTrace();
          for (int i = st.length - 1; i >= 0; i--) {
            StringBuilder sb = new StringBuilder();
            for (int j = st.length - i; j >= 0; j--) {
              sb.append("  ");
            }
            sb.append(st[i].toString());
            System.err.println(sb.toString());
          }
        }
        System.exit(1);
      }
    }
  }
}
