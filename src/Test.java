import java.util.ArrayList;

/**
 * Base class for test execution.
 */
public abstract class Test {
  /**
   * Set class to test.
   */
  protected final Class<? extends IHSet> SET_CLASS;
  /*
   * Number of test repetitions.
   */
  private final int TEST_REPETITIONS; // test repetitions

  @SuppressWarnings(("rawtypes"))
  Test(Class<? extends IHSet> SET_CLASS, int TEST_REPETITIONS) {
    this.SET_CLASS = SET_CLASS;
    this.TEST_REPETITIONS = TEST_REPETITIONS;
  }

  // Utility method for assertions.
  static <E> void assertEqual(String desc, E expected, E actual) {
    if (!expected.equals(actual)) {
      throw new AssertionError(desc + ": expected " + expected + " but got " + actual);
    }
  }

  /**
   * Create a set. This method is used by the test driver to create a set. 
   * @param ht_size Hash table size.
   */
  @SuppressWarnings(("unchecked"))
  public final <T> IHSet<T> createSet(int ht_size) throws Exception {
    return (IHSet<T>) SET_CLASS.getDeclaredConstructor(Integer.TYPE).newInstance((Integer) ht_size);
  }

  /**
   * Global test method - must be overriden by subclasses.
   */
  public abstract void onRun() throws Exception;

  /**
   * Executed once before all test repetitions - may be overriden by subclasses.
   */
  public void globalSetup() {
    // Do nothing by default.
  }

  /**
   * Executed once before all test repetitions - may be overriden by subclasses.
   */
  public void globalTeardown() throws Exception {
    // Do nothing by default.
  }

  /**
   * Executed before each test repetition - may be overriden by subclasses.
   */
  public void setup() throws Exception {
    // Do nothing by default.
  }

  /**
   * Executed after each test repetition - may be overriden by subclasses.
   */
  public void teardown() throws Exception {
    // Do nothing by default.
  }

  /**
   * Executes the test.
   */
  public final void run() throws Exception {
    System.out.printf("%s : testing %s%n", getClass().getSimpleName(), SET_CLASS.getSimpleName());
    DeadlockDetector.enable();
    globalSetup();
    for (int t = 0; t < TEST_REPETITIONS; t++) {
      setup();
      onRun();
      teardown();
    }
    globalTeardown();
    System.out.println("OK!");
  }

  /**
   * Execute a number of threads till completion.
   * 
   * @param toRun Runnable objects to execute.
   * @throws Exception if any of the threads throws an exception.
   */
  public final void execute(Runnable... toRun) throws Exception {
    Thread[] threads = new Thread[toRun.length];
    for (int i = 0; i < toRun.length; i++) {
      Thread th = new Thread(toRun[i]);
      th.setName("t" + i);
      th.start();
      threads[i] = th;
    }
    for (Thread th : threads) {
      th.join();
    }
  }
}
