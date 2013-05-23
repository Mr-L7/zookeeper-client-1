package org.robotninjas.zookeeper;

import java.lang.Throwable;

public interface PersistentWatcher<T> {

  void watchTriggered(PersistentWatch watch, T result);

  void watchFailed(PersistentWatch watch, Throwable reason);

}
