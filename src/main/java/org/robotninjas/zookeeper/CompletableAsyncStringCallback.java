package org.robotninjas.zookeeper;

import com.google.common.util.concurrent.AbstractFuture;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;import java.lang.Object;import java.lang.Override;import java.lang.String;

class CompletableAsyncStringCallback extends AbstractFuture<String> implements AsyncCallback.StringCallback {

  @Override
  public void processResult(int rc, String path, Object ctx, String name) {
    final KeeperException.Code code = KeeperException.Code.get(rc);
    if (code.equals(KeeperException.Code.OK)) {
      set(name);
    } else {
      setException(KeeperException.create(code));
    }
  }

}
