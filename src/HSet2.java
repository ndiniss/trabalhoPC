import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class HSet2<E> implements IHSet<E> {
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final ReadLock readLock = lock.readLock();
  private final WriteLock writeLock = lock.writeLock();
  private final Condition addedEntryCondition = writeLock.newCondition();

  private Node<E>[] table;
  private int size;

  /**
   * Constructor.
   *
   * @param ht_size Initial size for internal hash table.
   */
  public HSet2(int ht_size) {
    table = Node.createTable(ht_size);
    size = 0;
  }

  /**
   * Get the position in the hash table where an element should be stored.
   *
   * @param elem
   * @return
   */
  private int position(E elem) {
    return Math.abs(elem.hashCode() % table.length);
  }

  @Override
  public int capacity() {
    readLock.lock();
    try {
      return table.length;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public int size() {
    readLock.lock();
    try {
      return size;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public boolean add(E elem) {
    if (elem == null) {
      throw new IllegalArgumentException();
    }
    writeLock.lock();
    try {
      int pos = position(elem);
      Node<E> node = table[pos];
      boolean notAlreadyIn = !contains(elem);
      if (notAlreadyIn) {
        node = new Node<E>(elem, table[pos]);
        table[pos] = node;
        size++;
        addedEntryCondition.signalAll();
      }
      return notAlreadyIn;
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public boolean remove(E elem) {
    if (elem == null) {
      throw new IllegalArgumentException();
    }
    writeLock.lock();
    try {
      int pos = position(elem);
      Node<E> prev = null;
      Node<E> curr = table[pos];

      while (curr != null) {
        if (elem.equals(curr.elem)) {
          if (prev != null) {
            prev.next = curr.next;
          } else {
            table[pos] = curr.next;
          }
          size--;
          addedEntryCondition.signalAll();
          return true;
        }
        prev = curr;
        curr = curr.next;
      }
      return false;
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public boolean contains(E elem) {
    if (elem == null) {
      throw new IllegalArgumentException();
    }
    readLock.lock();
    try {
      int pos = position(elem);
      Node<E> node = table[pos];

      while (node != null) {
        if (elem.equals(node.elem)) {
          return true;
        }
        node = node.next;
      }
      return false;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void waitFor(E elem) {
    if (elem == null) {
      throw new IllegalArgumentException();
    }
    writeLock.lock();
    try {
      while (!contains(elem)) {
        try {
          addedEntryCondition.await();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public void rehash() {
    writeLock.lock();
    try {
      Node<E>[] oldTable = table;
      table = Node.createTable(2 * oldTable.length);
      size = 0;
      for (Node<E> list : oldTable) {
        Node<E> node = list;
        while (node != null) {
          add(node.elem);
          node = node.next;
        }
      }
    } finally {
      writeLock.unlock();
    }
  }
}
