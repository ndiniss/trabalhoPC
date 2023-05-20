import scala.concurrent.stm.Ref;
import scala.concurrent.stm.TArray;
import scala.concurrent.stm.japi.STM;

// TODO: uncomment the implements clause, and implement necessary methods.
public class HSet4<E> { // implements IHSet<E>{

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
}
