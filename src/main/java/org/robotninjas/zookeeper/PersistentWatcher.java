package org.robotninjas.zookeeper;

import java.lang.Throwable;public interface PersistentWatcher<T> {

  void watchTriggered(T result);

  void watchFailed(Throwable t);

  // TODO: void watchCancelled();

}
