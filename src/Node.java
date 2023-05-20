/**
 * A node in a singly-linked list.
 */
public final class Node<E> {
  /** Element. */
  E elem;
  /** Next node. */
  Node<E> next;

  /**
   * Constructs a new node.
   * 
   * @param elem Element to store.
   * @param next Reference to next node.
   */
  Node(E elem, Node<E> next) {
    this.elem = elem;
    this.next = next;
  }

  /**
   * Create array of singly-linked lists to define a hash table.
   * 
   * @param ht_size Hash table size.
   */
  @SuppressWarnings("unchecked")
  static <T> Node<T>[] createTable(int ht_size) {
    return (Node<T>[]) new Node[ht_size];
  }
}
