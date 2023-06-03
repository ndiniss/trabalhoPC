import scala.concurrent.stm.Ref;
import scala.concurrent.stm.TArray;
import scala.concurrent.stm.japi.STM;

// TODO: uncomment the implements clause, and implement necessary methods.
public class HSet4<E> implements IHSet<E>{

  private static class STMNode<T> {
    T elem;
    Ref.View<STMNode<T>> prev = STM.newRef(null);
    Ref.View<STMNode<T>> next = STM.newRef(null);
  }

  private final Ref.View<TArray.View<STMNode<E>>> table;
  private final Ref.View<Integer> size;

  public HSet4(int h_size) {
    table = STM.newRef(STM.newTArray(h_size));
    size = STM.newRef(0); 
  }

  /**
   * Função para descobrir a entrada (lista) na tabela associada a um elemento x no conjunto
  */
  private int getEntry(E elem){
    return Math.abs(elem.hashCode() % table.get().length());
  }

  /**
   * Função para obter a posição da lista ligada em que um elemento deve entrar
  */
  private STMNode<E> getPos(E elem){
    return table.get().apply(getEntry(elem)); // Lê a posição do indíce da lista em que elem deveria estar
  }

  @Override
  public int size() {
    return size.get();
  }

  @Override
  public int capacity() {
    return table.get().length();
  }

  @Override
  public boolean add(E elem) {
    if (elem == null) {
      throw new IllegalArgumentException(); //abort
    }
    return STM.atomic(()->{
      if(contains(elem)){
        return false; //Um Hashset não poderá ter elementos repetidos
      }
      /*Se porventura o elemento ainda não existe no set então procedemos a adicioná-lo
       * Para tal, descobrimos a posição em que deverá entrar
       * Colocamo-lo na frente da lista, ligando-o ao nó seguinte e o seguinte a este
      */
      STMNode<E> first = getPos(elem);
      STMNode<E> prox = new STMNode<>();
      prox.elem = elem;

      if(first == null)
        prox.next.set(null);
      else{
        prox.next.set(first);
        first.prev.set(prox);
      }
      //Após ligar o novo nó aos outros na cadeia atualizamos a tabela com o novo nó e novo tamanho
      table.get().update(getEntry(elem),prox);
      STM.increment(size,1);
      return true;
    });
  }

  @Override
  public boolean remove(E elem) {
    if (elem == null) {
      throw new IllegalArgumentException();
    }
    return STM.atomic(() -> {
      if (!contains(elem))
        return false;

      STMNode<E> toRemove = getPos(elem);

      while (toRemove != null){
        if((toRemove.elem).equals(elem)){
          //#1: Tem um nó a seguir
          if(toRemove.next.get() != null)
            toRemove.next.get().prev.set(toRemove.prev.get());
          //#2 Está no início da lista
          if(toRemove.prev.get() == null)
            table.get().update(getEntry(elem), toRemove.next.get());
          //#3 Tem 2 nós ligados (está no meio da lista)
          if(toRemove.prev.get() != null)
            toRemove.prev.get().next.set(toRemove.next.get());

          STM.increment(size,-1);
          return true;
        }
        toRemove = toRemove.next.get();
      }
      return false;
    });
  }

  @Override
  public boolean contains(E elem) {
    if (elem.equals(null)) {
      throw new IllegalArgumentException();
    }
    return STM.atomic(() -> {
      /* Pegamos num nó iterador que vai iterar pela lista
       * Comparando sempre o valor atual com o pretendido
       * Se encontrar retorna true, caso contrário continua
       * Ao chegar ao fim da lista retorna false o que significa que o valor não existe*/
      STMNode<E> it = getPos(elem);
      while(it != null){
        if((it.elem).equals(elem)) return true;
        it = it.next.get();
      }
      return false;
    });
  }

  @Override
  public void waitFor(E elem) {
    if (elem == null) {
      throw new IllegalArgumentException();
    }
    STM.atomic(() -> {
      if(contains(elem)) return;
      STM.retry();
    });
  }

  @Override
  public void rehash() {
    STM.atomic(() -> {
      int newCapacity = table.get().length() * 2; // Double the capacity

      TArray.View<STMNode<E>> oldTable = table.get();
      table.set(STM.newTArray(newCapacity));

      // Rehash all the elements into the new table
      for (int i = 0; i < oldTable.length(); i++) {
        STMNode<E> node = oldTable.apply(i);
        while (node != null) {
          STMNode<E> next = node.next.get();

          int newEntry = Math.abs(node.elem.hashCode() % newCapacity);
          STMNode<E> firstNode = table.get().apply(newEntry);
          if (firstNode == null) {
            node.next.set(null);
          } else {
            node.next.set(firstNode);
            firstNode.prev.set(node);
          }

          table.get().update(newEntry, node);
          node = next;
        }
      }
    });
  }
}
