package slimeknights.tconstruct.library.utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Runs callbacks for a number of listeners which are weakly referenced */
public class WeakListenerList implements Runnable {
  private final List<Entry<?>> entries = new ArrayList<>();

  /**
   * Adds a weak listener that will automatically be cleared when the parent goes away
   * @param parent    Parent that owns the callback. When this goes out of scope, the callback stops
   * @param listener  Callback when the event fires
   * @param <T>  Parent type
   */
  public <T> void addListener(T parent, Consumer<T> listener) {
    // try to find an existing entry to reuse
    for (Entry<?> entry : entries) {
      if (entry.parent.get() == parent) {
        @SuppressWarnings("unchecked")
        Entry<T> casted = (Entry<T>) entry;
        casted.listener = listener;
        return;
      }
    }
    entries.add(new Entry<>(new WeakReference<>(parent), listener));
  }

  /**
   * Removes any listener associated with the given object
   * @param parent  Target object
   */
  public void removeListener(Object parent) {
    for (Iterator<Entry<?>> iterator = entries.iterator(); iterator.hasNext(); ) {
      Entry<?> entry = iterator.next();
      if (entry.parent.get() == parent) {
        iterator.remove();
        return;
      }
    }
  }

  @Override
  public void run() {
    entries.removeIf(Entry.REMOVE_IF);
  }

  private static class Entry<T> {
    private final WeakReference<T> parent;
    private Consumer<T> listener;

    private Entry(WeakReference<T> parent, Consumer<T> listener) {
      this.parent = parent;
      this.listener = listener;
    }

    private static final Predicate<Entry<?>> REMOVE_IF = Entry::run;

    /**
     * Runs the listener
     * @return true if the listener is no longer valid.
     */
    public boolean run() {
      T te = parent.get();
      if (te != null) {
        listener.accept(te);
        return false;
      }
      return true;
    }
  }
}
