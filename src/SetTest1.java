import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unchecked")
public class SetTest1 extends Test {
  public static void main(String[] args) throws Exception {
    Class<? extends IHSet> clazz = args.length < 1 ? HSet0.class : (Class<? extends IHSet>) Class.forName(args[0]);
    int T = args.length < 2 ? 8 : Integer.parseInt(args[1]);
    new SetTest1(clazz, T).run();
  }

  final int T, OPS, N;
  IHSet<Integer> set;
  final AtomicInteger expectedSetSize = new AtomicInteger();

  SetTest1(Class<? extends IHSet> clazz, int threads) {
    super(clazz, 10);
    this.T = threads;
    N = 100;
    // Number of operations per thread
    OPS = 10000;
  }

  @Override
  public void setup() throws Exception {
    expectedSetSize.set(0);
    set = createSet(T);
  }

  @Override
  public void teardown() {
    assertEqual("[final check]", expectedSetSize.get(), set.size());
  }

  @Override
  public void onRun() throws Exception {
    Runnable[] toRun = new Runnable[T];
    for (int i = 0; i < T; i++) {
      final int id = i;
      toRun[i] = () -> run(id);
    }
    execute(toRun);
  }

  private void run(int id) {
    try {
      // "Oracle set" for this thread in terms of thread modification s
      java.util.Set<Integer> mySet = new java.util.TreeSet<>();
      int min = id * N;
      int max = min + N;
      Random rng = new Random(id);

      for (int i = 0; i < OPS; i++) {
        Thread.yield();
        int v = min + rng.nextInt(max - min);
        switch (rng.nextInt(5)) {
          case 0:
            assertEqual("add()", mySet.add(v), set.add(v));
            assertEqual("contains() after add()", true, set.contains(v));
            break;
          case 1:
            assertEqual("remove", mySet.remove(v), set.remove(v));
            assertEqual("contains() after remove()", false, set.contains(v));
            break;
          default:
            assertEqual("contains()", mySet.contains(v), set.contains(v));
            break;
        }
      }
      for (int v = min; v < max; v++) {
        Thread.yield();
        assertEqual("contains() [final check]", mySet.contains(v), set.contains(v));
      }
      expectedSetSize.getAndAdd(mySet.size());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
