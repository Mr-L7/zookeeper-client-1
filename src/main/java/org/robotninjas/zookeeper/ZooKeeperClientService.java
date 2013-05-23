package org.robotninjas.zookeeper;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.util.concurrent.Futures.addCallback;

public class ZooKeeperClientService extends AbstractService implements ZooKeeperClient, Watcher {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final String connectString;
  private Optional<ZooKeeper> client;

  public ZooKeeperClientService(String connectString) {
    this.client = Optional.absent();
    this.connectString = connectString;
  }

  @Override
  public void process(WatchedEvent watchedEvent) {
    log.debug("{}", watchedEvent);
    switch (watchedEvent.getState()) {
      case AuthFailed:
      case Expired:
        notifyFailed(new Exception(watchedEvent.getState().toString()));
        break;
      case SyncConnected:
        notifyStarted();
        break;
      default:
        //log it
        break;
    }
  }

  @Override
  protected void doStart() {
    try {
      client = Optional.of(new ZooKeeper(connectString, 10000, this));
    } catch (Exception e) {
      notifyFailed(e);
    }
  }

  @Override
  protected void doStop() {
    if (client.isPresent()) {
      try {
        client.get().close();
        notifyStopped();
      } catch (InterruptedException e) {
        notifyFailed(e);
      }
    }
  }

  @Override
  public ListenableFuture<String> create(String path, byte[] data, CreateMode mode, List<ACL> acl) {
    Preconditions.checkState(isRunning());
    final CompletableAsyncStringCallback callback = new CompletableAsyncStringCallback();
    client.get().create(path, data, acl, mode, callback, null);
    return callback;
  }

  @Override
  public ListenableFuture<java.lang.Void> delete(String path, int version) {
    Preconditions.checkState(isRunning());
    final CompletableAsyncVoidCallback callback = new CompletableAsyncVoidCallback();
    client.get().delete(path, version, callback, null);
    return callback;
  }

  @Override
  public ListenableFuture<Stat> stat(String path) {
    Preconditions.checkState(isRunning());
    final CompletableAsyncStatCallback callback = new CompletableAsyncStatCallback();
    client.get().exists(path, false, callback, null);
    return callback;
  }

  @Override
  public ListenableFuture<Stat> statAndWatch(String path, Watcher watcher) {
    Preconditions.checkState(isRunning());
    final CompletableAsyncStatCallback callback = new CompletableAsyncStatCallback();
    client.get().exists(path, watcher, callback, null);
    return callback;
  }

  @Override
  public ListenableFuture<List<ACL>> getAcl(String path) {
    Preconditions.checkState(isRunning());
    final CompletableAsyncAclCallback callback = new CompletableAsyncAclCallback();
    final Stat stat = new Stat();
    client.get().getACL(path, stat, callback, null);
    return callback;
  }

  @Override
  public ListenableFuture<Stat> setAcl(String path, int version, List<ACL> acl) {
    Preconditions.checkState(isRunning());
    final CompletableAsyncStatCallback callback = new CompletableAsyncStatCallback();
    client.get().setACL(path, acl, version, callback, null);
    return callback;
  }

  @Override
  public ListenableFuture<byte[]> getAndWatchData(String path, Watcher watcher) {
    Preconditions.checkState(isRunning());
    final CompletableAsyncDataCallback callback = new CompletableAsyncDataCallback();
    client.get().getData(path, watcher, callback, null);
    return callback;
  }

  @Override
  public ListenableFuture<Stat> setData(String path, int version, byte[] data) {
    Preconditions.checkState(isRunning());
    final CompletableAsyncStatCallback callback = new CompletableAsyncStatCallback();
    client.get().setData(path, data, version, callback, null);
    return callback;
  }

  @Override
  public ListenableFuture<List<String>> getAndWatchChildren(String path, Watcher watcher) {
    Preconditions.checkState(isRunning());
    final CompletableAsyncChildrenCallback callback = new CompletableAsyncChildrenCallback();
    client.get().getChildren(path, watcher, callback, null);
    return callback;
  }

  @Override
  public ListenableFuture<List<String>> getChildren(String path) {
    Preconditions.checkState(isRunning());
    final CompletableAsyncChildrenCallback callback = new CompletableAsyncChildrenCallback();
    client.get().getChildren(path, false, callback, null);
    return callback;
  }

  private void watchData(final PersistentWatch watch, final String path, final PersistentWatcher<byte[]> watcher) {

    if (watch.isCancelled()) {
      return;
    }

    addCallback(getAndWatchData(path, new Watcher() {
      @Override
      public void process(WatchedEvent event) {
        watchData(path, watcher);
      }
    }), new PersistentWatchCallback<byte[]>(watcher, watch));
  }

  public PersistentWatch watchData(final String path, final PersistentWatcher<byte[]> watcher) {
    Preconditions.checkState(isRunning());
    final PersistentWatch watch = new PersistentWatch();
    watchData(watch, path, watcher);
    return watch;
  }

  private void watchChildren(final PersistentWatch watch, final String path, final PersistentWatcher<List<String>> watcher) {

    if (watch.isCancelled()) {
      return;
    }

    addCallback(getAndWatchChildren(path, new Watcher() {
      @Override
      public void process(WatchedEvent event) {
        watchChildren(path, watcher);
      }
    }), new PersistentWatchCallback<List<String>>(watcher, watch));

  }

  public PersistentWatch watchChildren(final String path, final PersistentWatcher<List<String>> watcher) {
    Preconditions.checkState(isRunning());
    final PersistentWatch watch = new PersistentWatch();
    watchChildren(watch, path, watcher);
    return watch;
  }

  private void watchNode(final PersistentWatch watch, final String path, final PersistentWatcher<Stat> watcher) {

    if (watch.isCancelled()) {
      return;
    }

    addCallback(statAndWatch(path, new Watcher() {
      @Override
      public void process(WatchedEvent event) {
        watchNode(watch, path, watcher);
      }
    }), new PersistentWatchCallback<Stat>(watcher, watch));

  }

  public PersistentWatch watchNode(final String path, final PersistentWatcher<Stat> watcher) {
    Preconditions.checkState(isRunning());
    final PersistentWatch watch = new PersistentWatch();
    watchNode(watch, path, watcher);
    return watch;
  }

  private static class PersistentWatchCallback<T> implements FutureCallback<T> {

    private final PersistentWatcher<T> watcher;
    private final PersistentWatch watch;

    private PersistentWatchCallback(PersistentWatcher<T> watcher, PersistentWatch watch) {
      this.watcher = watcher;
      this.watch = watch;
    }

    @Override
    public void onSuccess(T value) {
      watcher.watchTriggered(watch, value);
    }

    @Override
    public void onFailure(Throwable throwable) {
      watcher.watchFailed(watch, throwable);
    }

  }

}
