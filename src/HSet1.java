import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class HSet1<E> implements IHSet<E> {
  private Node<E>[] table;
  private int size = 0;
  private ReentrantLock lock;
  private Condition condition;

  public HSet1(int ht_size) {
    table = Node.createTable(ht_size);
    lock = new ReentrantLock();
    condition = lock.newCondition();
  }

  @Override
  public int capacity() {
    return table.length;
  }

  @Override
  public int size() {
    lock.lock();
    try {
      return size;
    } finally {
      lock.unlock();
    }
  }

  private int position(E elem) {
    return Math.abs(elem.hashCode() % table.length);
  }

  @Override
  public boolean add(E elem) {
    lock.lock();
    try {
      int pos = position(elem);
      Node<E> node = table[pos];
      while (node != null) {
        if (elem.equals(node.elem)) {
          return false;
        }
        node = node.next;
      }
      node = new Node<>(elem, table[pos]);
      table[pos] = node;
      size++;
      condition.signalAll();
      return true;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean remove(E elem) {
    lock.lock();
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
          return true;
        }
        prev = curr;
        curr = curr.next;
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean contains(E elem) {
    lock.lock();
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
      lock.unlock();
    }
  }

  @Override
  public void waitFor(E elem) {
    lock.lock();
    try {
      while (!contains(elem)) {
        try {
          condition.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void rehash() {
    lock.lock();
    try {
      Node<E>[] oldTable = table;
      table = Node.createTable(2 * oldTable.length);
      for (Node<E> list : oldTable) {
        Node<E> node = list;
        while (node != null) {
          add(node.elem);
          node = node.next;
        }
      }
    } finally {
      lock.unlock();
    }
  }
}
