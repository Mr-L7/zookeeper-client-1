package org.robotninjas.zookeeper;

import com.google.common.util.concurrent.AbstractFuture;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;import java.lang.Override;

public class FutureWatcher extends AbstractFuture<WatchedEvent> implements Watcher {
  @Override
  public void process(WatchedEvent event) {
    set(event);
  }
}
