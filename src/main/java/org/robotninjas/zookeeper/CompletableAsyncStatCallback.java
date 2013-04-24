package org.robotninjas.zookeeper;

import com.google.common.util.concurrent.AbstractFuture;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;import java.lang.Object;import java.lang.Override;import java.lang.String;

class CompletableAsyncStatCallback extends AbstractFuture<Stat> implements AsyncCallback.StatCallback {
  @Override
  public void processResult(int rc, String path, Object ctx, Stat stat) {
    final KeeperException.Code code = KeeperException.Code.get(rc);
    if (code.equals(KeeperException.Code.OK)) {
      set(stat);
    } else {
      setException(KeeperException.create(code));
    }
  }
}
