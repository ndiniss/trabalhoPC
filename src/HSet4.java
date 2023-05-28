import scala.concurrent.stm.Ref;
import scala.concurrent.stm.TArray;
import scala.concurrent.stm.japi.STM;

// TODO: uncomment the implements clause, and implement necessary methods.
public class HSet4<E> implements IHSet<E>{

  private static class STMNode<T> {
    T elem;
    Ref.View<STMNode<T>> next = STM.newRef(null);
  }

  private final Ref.View<TArray.View<STMNode<E>>> table;
  private final Ref.View<Integer> size;

  public HSet4(int h_size) {
    table = STM.newRef(STM.newTArray(h_size));
    size = STM.newRef(0); 
  }

  @Override
  public int size() {
    return size.get();
  }

  @Override
  public int capacity() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'capacity'");
  }

  @Override
  public boolean add(E elem) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'add'");
  }

  @Override
  public boolean remove(E elem) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'remove'");
  }

  @Override
  public boolean contains(E elem) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'contains'");
  }

  @Override
  public void waitFor(E elem) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'waitFor'");
  }

  @Override
  public void rehash() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'rehash'");
  }
}
