package slimeknights.tconstruct.library.utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/** Runs callbacks for a number of listeners which are weakly referenced */
public class WeakListenerList implements Runnable {
  private final List<Entry<?>> entries = new ArrayList<>();

  /** Adds a listener for the given parent */
  public <T> void addListener(T parent, Consumer<T> listener) {
    entries.add(new Entry<>(new WeakReference<>(parent), listener));
  }

  /** Removes all listeners for the given parent */
  public void removeListeners(Object parent) {
    Iterator<Entry<?>> iterator = entries.iterator();
    while (iterator.hasNext()) {
      Entry<?> entry = iterator.next();
      Object entryParent = entry.parent.get();
      if (entryParent == null || entryParent == parent) {
        iterator.remove();
      }
    }
  }

  @Override
  public void run() {
    entries.removeIf(entry -> entry.run());
  }

  private record Entry<T>(WeakReference<T> parent, Consumer<T> listener) {
    /**
     * Runs the listener.
     * @return true if the listener is no longer valid.
     */
    public boolean run() {
      T value = parent.get();
      if (value != null) {
        listener.accept(value);
        return false;
      }
      return true;
    }
  }
}