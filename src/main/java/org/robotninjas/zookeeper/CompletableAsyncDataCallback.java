package org.robotninjas.zookeeper;

import com.google.common.util.concurrent.AbstractFuture;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;import java.lang.Object;import java.lang.Override;import java.lang.String;

class CompletableAsyncDataCallback extends AbstractFuture<byte[]> implements AsyncCallback.DataCallback {
  @Override
  public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
    final KeeperException.Code code = KeeperException.Code.get(rc);
    if (code.equals(KeeperException.Code.OK)) {
      set(data);
    } else {
      setException(KeeperException.create(code));
    }
  }
}
