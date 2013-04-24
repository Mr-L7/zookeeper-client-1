package org.robotninjas.zookeeper;

import com.google.common.util.concurrent.AbstractFuture;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.lang.Object;import java.lang.Override;import java.lang.String;import java.util.List;

class CompletableAsyncAclCallback extends AbstractFuture<List<ACL>> implements AsyncCallback.ACLCallback {
  @Override
  public void processResult(int rc, String path, Object ctx, List<ACL> acl, Stat stat) {
    final KeeperException.Code code = KeeperException.Code.get(rc);
    if (code.equals(KeeperException.Code.OK)) {
      set(acl);
    } else {
      setException(KeeperException.create(code));
    }
  }

}
