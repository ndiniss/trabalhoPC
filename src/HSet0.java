public class HSet0<E> implements IHSet<E> {
  /** Hash table. */
  private Node<E>[] table;
  /** Number of elements. */
  private int size = 0;

  /**
   * Constructor.
   *
   * @param ht_size Initial size for internal hash table.
   */
  public HSet0(int ht_size) {
    table = Node.createTable(ht_size);
  }

  @Override
  public synchronized int capacity() {
    return table.length;
  }

  @Override
  public synchronized int size() {
    return size;
  }

  /**
   * Get the position in the hash table where
   * an element should be stored.
   *
   * @param elem The element at stake)
   * @return the position in the hash table.
   */
  private int position(E elem) {
    return Math.abs(elem.hashCode() % table.length);
  }

  @Override
  public synchronized boolean add(E elem) {
    int pos = position(elem);
    Node<E> node = table[pos];
    while (node != null) {
      if (elem.equals(node.elem)) {
        return false;
      }
      node = node.next;
    }
    // New element, add it to the (head of the) list
    node = new Node<E>(elem, table[pos]);
    table[pos] = node;
    size++;
    notifyAll();
    return true;
  }

  @Override
  public synchronized boolean remove(E elem) {
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
  }

  @Override
  public synchronized boolean contains(E elem) {
    int pos = position(elem);
    Node<E> node = table[pos];

    while (node != null) {
      if (elem.equals(node.elem)) {
        return true;
      }
      node = node.next;
    }
    return false;
  }

  @Override
  public synchronized void waitFor(E elem) {
    while (!contains(elem)) {
      try {
        wait();
      } catch (InterruptedException e) {
        // HandleException
        e.printStackTrace();
      }
    }
  }

  @Override
  public synchronized void rehash() {
    Node<E>[] oldTable = table;
    table = Node.createTable(2 * oldTable.length);
    for (Node<E> list : oldTable) {
      Node<E> node = list;
      while (node != null) {
        add(node.elem);
        node = node.next;
      }
    }
  }
}
