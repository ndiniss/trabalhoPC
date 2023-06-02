import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashSet;

public class DeadlockDetector extends Thread {

  public static final DeadlockDetector INSTANCE = new DeadlockDetector();
  private final HashSet<Long> threads_to_ignore = new HashSet<>();
  private final ThreadMXBean mxbean;
  private static final int MAX_RETRIES = 10; // Max retries before concluding it's a deadlock!
  private static final long POLLING_INTERVAL_MS = 100L; // Polling interval (ms)

  public static void enable() {
    if (! INSTANCE.isAlive())
      INSTANCE.start();
  }

  private DeadlockDetector() {
    super("DeadlockDetector");
    setDaemon(true);
    mxbean = ManagementFactory.getThreadMXBean();
    for (ThreadInfo ti : mxbean.dumpAllThreads(false, false)) {
      if (!ti.getThreadName().equals("main")) {
        threads_to_ignore.add(ti.getThreadId());
      }
    }
  }

  @Override
  public void run() {
    threads_to_ignore.add(Thread.currentThread().getId());
    int deadlock_suspected = 0;
    while (true) {
      try {
        Thread.sleep(POLLING_INTERVAL_MS);
      }
      catch(InterruptedException e) {
        throw new RuntimeException("Unexpected interrupt");
      }
      boolean deadlock = true;
      ArrayList<ThreadInfo> stuck = new ArrayList<>();
      for (ThreadInfo ti : mxbean.dumpAllThreads(false, false)) {
        if (threads_to_ignore.contains(ti.getThreadId())) {
          continue;
        }
        // System.out.println(ti.getThreadName() + " --> " + ti.getThreadState());
        switch (ti.getThreadState()) {
          case BLOCKED:
          case WAITING:
            stuck.add(ti);
            break;
          default:
            deadlock = false;
        }
      }
      if (!deadlock) {
        deadlock_suspected = 0;
        continue;
      }
      deadlock_suspected ++;
      if (deadlock_suspected < MAX_RETRIES) {
        continue; // May be a false positive (unfortunately ThreadMXBean may not report the state accurately)
      }
      System.err.println("Deadlock detected!");
      for (ThreadInfo ti : stuck) {
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
