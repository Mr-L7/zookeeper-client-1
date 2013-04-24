package org.robotninjas.zookeeper;

import com.google.common.util.concurrent.AbstractFuture;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;import java.lang.Object;import java.lang.Override;import java.lang.String;import java.lang.Void;

class CompletableAsyncVoidCallback extends AbstractFuture<Void> implements AsyncCallback.VoidCallback {
  @Override
  public void processResult(int rc, String path, Object ctx) {
    final KeeperException.Code code = KeeperException.Code.get(rc);
    if (code.equals(KeeperException.Code.OK)) {
      set(null);
    } else {
      setException(KeeperException.create(code));
    }
  }
}
