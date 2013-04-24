package org.robotninjas.zookeeper;

public class PersistentWatch {

  private volatile boolean isCancelled = false;

  public void cancel() {
    isCancelled = true;
  }

  public boolean isCancelled() {
    return isCancelled;
  }

}
