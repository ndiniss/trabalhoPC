
public class SetTest2 extends Test {

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void main(String[] args) throws Exception {
    Class<? extends IHSet> clazz = args.length < 1 ? HSet0.class : (Class<? extends IHSet>) Class.forName(args[0]);
    int repetitions = args.length < 2 ? 100 : Integer.parseInt(args[1]);
    new SetTest2(clazz, repetitions).run();
  }

  SetTest2(Class<? extends IHSet> clazz, int repetitions) {
    super(clazz, repetitions);
  }

  @Override
  public void onRun() throws Exception {
    testWaitFor1();
    testWaitFor2();
    testWaitFor3();
    testRehash1();
    testRehash2();
  }

  private void testWaitFor1() throws Exception {
    IHSet<String> set = createSet(4);
    execute(
        () -> {
          set.add("a");
        },
        () -> {
          set.waitFor("a");
          assertEqual("t1", true, set.contains("a"));
        },
        () -> {
          set.waitFor("a");
          assertEqual("t2", true, set.contains("a"));
        },
        () -> {
          set.waitFor("a");
          assertEqual("t3", true, set.contains("a"));
        });
  }

  private void testWaitFor2() throws Exception {
    IHSet<String> set = createSet(4);
    execute(
        () -> {
          set.add("a");
          set.waitFor("b");
          assertEqual("t1w1", true, set.contains("b"));
          set.waitFor("c");
          assertEqual("t1w2", true, set.contains("c"));
          set.waitFor("d");
          assertEqual("t1w3", true, set.contains("d"));
        },
        () -> {
          set.waitFor("a");
          assertEqual("t2w1", true, set.contains("a"));
          set.add("b");
        },
        () -> {
          set.waitFor("a");
          assertEqual("t3w1", true, set.contains("a"));
          set.add("c");
        },
        () -> {
          set.waitFor("a");
          assertEqual("t4w1", true, set.contains("a"));
          set.add("d");
        });
  }

  private void testWaitFor3() throws Exception {
    IHSet<String> set = createSet(4);
    StringBuffer sb = new StringBuffer();
    execute(
        () -> {
          sb.append('a');
          set.add("a");
          set.waitFor("d");
          ;
          assertEqual("t1", true, set.contains("d"));
          set.waitFor("a");
        },
        () -> {
          set.waitFor("a");
          assertEqual("t2", true, set.contains("a"));
          sb.append('b');
          set.add("b");
          set.waitFor("b");
          set.waitFor("a");
        },
        () -> {
          set.waitFor("b");
          assertEqual("t3", true, set.contains("b"));
          sb.append('c');
          set.add("c");
          set.waitFor("c");
          set.waitFor("b");
        },
        () -> {
          set.waitFor("c");
          assertEqual("t4", true, set.contains("c"));
          sb.append('d');
          set.add("d");
          set.waitFor("d");
          set.waitFor("c");
        });
    assertEqual("final check", "abcd", sb.toString());
  }

  static final int INITIAL_CAPACITY = 2;

  private void testRehash1() throws Exception {
    IHSet<String> set = createSet(INITIAL_CAPACITY);
    execute(
        () -> {
          set.add("a", "b", "c", "d");
          Thread.yield();
          set.rehash();
          assertEqual("t1", 4, set.contains("a", "b", "c", "d"));
        },
        () -> {
          set.add("e", "f", "g");
          Thread.yield();
          set.rehash();
          assertEqual("t2", 3, set.contains("e", "f", "g"));
          Thread.yield();
          set.add("h");
        });
    assertEqual("main", 8, set.contains("a", "b", "c", "d", "e", "f", "g", "h"));
    assertEqual("main", 4 * INITIAL_CAPACITY, set.capacity());
  }

  private void testRehash2() throws Exception {
    IHSet<String> set = createSet(INITIAL_CAPACITY);
    execute(
       () -> {
        Thread.yield();
        assertEqual("t1 [1]", 4, set.add("a", "b", "c", "d"));
         Thread.yield();
         set.rehash();
         Thread.yield();
         assertEqual("t1 [2]", 4, set.contains("a", "b", "c", "d"));
         Thread.yield();
         assertEqual("t1 [3]", 4, set.remove("a", "b", "c", "d"));
         Thread.yield();
         assertEqual("t1 [4]", 0, set.contains("a", "b", "c", "d"));
       },
       () -> {
         Thread.yield();
         assertEqual("t1 [1]", 3, set.add("e", "f", "g"));
         Thread.yield();
         set.rehash();
         Thread.yield();
         assertEqual("t2 [1]", 3, set.contains("e", "f", "g"));
         Thread.yield();
         assertEqual("t2 [2]", 3, set.remove("e", "f", "g"));
         Thread.yield();
         assertEqual("t2 [3]", 0, set.contains("e", "f", "g"));
         Thread.yield();
         set.add("h");
         Thread.yield();
         set.rehash();
         assertEqual("t2 [4]", true, set.contains("h"));
       },
       () -> {
          Thread.yield();
          assertEqual("t3 [1]", 4, set.add("i", "j", "k", "l"));
          set.rehash();
       }
    );
    assertEqual("main [1]", 5, set.contains("h","i","j","k","l"));
    assertEqual("main [2]", 0, set.contains("a", "b", "c", "d", "e", "f", "g"));
    assertEqual("main [3]", 16 * INITIAL_CAPACITY, set.capacity());
  }
}
