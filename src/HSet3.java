import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;


public class HSet3<E> implements IHSet<E> {
    /** Hash table. */
    private Node<E>[] table;
    private AtomicInteger size = new AtomicInteger(0);

    private ReentrantReadWriteLock[] locks;
    private Condition[] conditions;

    /**
     * Constructor.
     * 
     * @param ht_size Initial size for internal hash table.
     */
    public HSet3(int ht_size) {
        table = Node.createTable(ht_size);
        locks = createLocks(ht_size);
        conditions = createConditions(locks);
    }

    // Creation of initial variables
    private ReentrantReadWriteLock[] createLocks(int ht_size) {
        ReentrantReadWriteLock[] locks = new ReentrantReadWriteLock[ht_size];
        for (int i = 0; i < ht_size; i++) {
            locks[i] = new ReentrantReadWriteLock();
        }
        return locks;
    }
    private Condition[] createConditions(ReentrantReadWriteLock[] locks) {
        Condition[] conditions = new Condition[locks.length];
        for (int i = 0; i < locks.length; i++) {
            conditions[i] = locks[i].writeLock().newCondition();
        }
        return conditions;
    }

    // Getters
    private ReadLock getReadLock(E elem) {
        return locks[Math.abs(elem.hashCode() % locks.length)].readLock();
    }
    private WriteLock getWriteLock(E elem) {
        return locks[Math.abs(elem.hashCode() % locks.length)].writeLock();
    }
    private Condition getCondition(E elem) {
        return conditions[Math.abs(elem.hashCode() % locks.length)];
    }

    // General locking functions
    private void lockAllLocks(){
        lockAllWriteLocks();
        lockAllReadLocks();
    }
    private void unlockAllLocks() {
        unlockAllReadLocks();
        unlockAllWriteLocks();
    }
    private void lockAllReadLocks(){
        for (int i = 0; i < locks.length; i++) {
            locks[i].readLock().lock();
        }
    }
    private void unlockAllReadLocks() {
        for (int i = 0; i < locks.length; i++) {
            locks[i].readLock().unlock();
        }
    }
    private void lockAllWriteLocks(){
        for (int i = 0; i < locks.length; i++) {
            locks[i].writeLock().lock();
        }
    }
    private void unlockAllWriteLocks() {
        for (int i = 0; i < locks.length; i++) {
            locks[i].writeLock().unlock();
        }
    }

    // Auxiliary method to return the list where
    // an element should be stored.
    private Node<E> getEntry(E elem) {
        return table[getEntryIndex(elem)];
    }

    private int getEntryIndex(E elem) {
        return Math.abs(elem.hashCode() % table.length);
    }

    private Node<E> getNodeElement(Node<E> node, E value) {
        if(node == null) { return null; }
        if(node.elem.equals(value)){ return node; }
        if(node.next == null){ return null; }

        return getNodeElement(node.next, value);
    }

    private boolean nodeContainsElement(Node<E> node, E value) {
        if(node == null) { return false; }
        if(node.elem.equals(value)){ return true; }
        if(node.next == null){ return false; }

        return nodeContainsElement(node.next, value);
    }

    @Override
    public int capacity() {
        return table.length;
    }

    @Override
    public int size() {
        return this.size.get();
    }

    // Check if first node is null for all methods
    @Override
    public boolean add(E elem) {
        if (elem == null) {
            throw new IllegalArgumentException();
        }
        getWriteLock(elem).lock();
        try {
            Node<E> firstNode = getEntry(elem);
            boolean nodeContainsElement = contains(elem);
            if (!nodeContainsElement) {
                if (firstNode == null) {
                    table[getEntryIndex(elem)] = new Node<E>(elem, null);
                } else {
                    table[getEntryIndex(elem)] = new Node<E>(elem, firstNode);
                }
                this.size.incrementAndGet();
                getCondition(elem).signalAll(); // there may be threads waiting in waitEleme
            }
            return !nodeContainsElement;
        } finally {
            getWriteLock(elem).unlock();
        }
    }

    @Override
    public boolean remove(E elem) {
        if (elem == null) {
            throw new IllegalArgumentException();
        }
        getWriteLock(elem).lock();
        try {
            Node<E> firstNode = getEntry(elem);
            Node<E> nodeElement = getNodeElement(firstNode, elem);
            boolean nodeContainsElement = nodeElement != null;
            if (nodeContainsElement) {
                if (nodeElement.equals(firstNode)) {
                    table[getEntryIndex(elem)] = nodeElement.next;
                } else {
                    Node<E> nodeBefore = firstNode;
                    while(!nodeBefore.next.equals(nodeElement)){nodeBefore = nodeBefore.next;}
                    nodeBefore.next = nodeElement.next;
                }
                this.size.decrementAndGet();
                getCondition(elem).signalAll(); // there may be threads waiting in waitEleme
            }
            return nodeContainsElement;
        } finally {
            getWriteLock(elem).unlock();
        }
    }

    @Override
    public boolean contains(E elem) {
        if (elem == null) {
            throw new IllegalArgumentException();
        }
        getReadLock(elem).lock();
        try {
            Node<E> firstNode = getEntry(elem);
            return nodeContainsElement(firstNode, elem);
        } finally {
            getReadLock(elem).unlock();
        }
    }

    @Override
    public void waitFor(E elem) {
        if (elem == null) {
            throw new IllegalArgumentException();
        }
        getWriteLock(elem).lock();
        try {
            Node<E> firstNode = getEntry(elem);
            while (!nodeContainsElement(firstNode, elem)) {
                try {
                    getCondition(elem).await();
                }
                catch(InterruptedException e) { 
                    // Ignore interrupts
                }
            }  
        } finally {
            getWriteLock(elem).unlock();
        }
    }
    
    @Override
    public void rehash() {
        lockAllLocks();
        try {
            Node<E>[] oldTable = table;
            table = Node.createTable(2 * oldTable.length);

            for (Node<E> node : oldTable) {
                if (node == null) { continue; }
                
                Node<E> searchNode = node;
                while(searchNode.next != null) {
                    addWithoutLocks(searchNode.elem);
                    searchNode = searchNode.next;
                }
                addWithoutLocks(searchNode.elem);
            }
        } finally {
            unlockAllLocks();
        }
    }

    private boolean addWithoutLocks(E elem) {
        if (elem == null) {
            throw new IllegalArgumentException();
        }
        // getWriteLock(elem).lock();
        try {
            Node<E> firstNode = getEntry(elem);
            boolean nodeContainsElement = nodeContainsElement(firstNode, elem);
            if (!nodeContainsElement) {
                if (firstNode == null) {
                    table[getEntryIndex(elem)] = new Node<E>(elem, null);
                } else {
                    table[getEntryIndex(elem)] = new Node<E>(elem, firstNode);
                }
                // this.size.incrementAndGet();
                // getCondition(elem).signalAll(); // there may be threads waiting in waitEleme
            }
            return !nodeContainsElement;
        } finally {
            // getWriteLock(elem).unlock();
        }
    }
}

