package org.robotninjas.zookeeper;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.lang.Exception;import java.lang.InterruptedException;import java.lang.Override;import java.lang.String;import java.lang.Throwable;import java.util.List;

public class ZooKeeperClientService extends AbstractService implements ZooKeeperClient, Watcher {

  private final String connectString;
  private Optional<ZooKeeper> client;

  public ZooKeeperClientService(String connectString) {
    this.client = Optional.absent();
    this.connectString = connectString;
  }

  @Override
  public void process(WatchedEvent watchedEvent) {
    switch (watchedEvent.getState()) {
      case AuthFailed:
      case Expired:
        notifyFailed(new Exception(watchedEvent.getState().toString()));
        break;
      default:
        //log it
        break;
    }
  }

  @Override
  protected void doStart() {
    try {
      client = Optional.of(new ZooKeeper(connectString, 1000, this));
      notifyStarted();
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
  public ListenableFuture<Stat> stat(String path, Watcher watcher) {
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
  public ListenableFuture<byte[]> getData(String path, Watcher watcher) {
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
  public ListenableFuture<List<String>> getChildren(String path, Watcher watcher) {
    Preconditions.checkState(isRunning());
    final CompletableAsyncChildrenCallback callback = new CompletableAsyncChildrenCallback();
    client.get().getChildren(path, watcher, callback, null);
    return callback;
  }

  public void watchData(final PersistentWatch watch, final String path, final PersistentWatcher<byte[]> watcher) {

    Preconditions.checkState(isRunning());

    final ListenableFuture<byte[]> f = getData(path, new Watcher() {
      @Override
      public void process(WatchedEvent event) {
        watchData(path, watcher);
      }
    });

    Futures.addCallback(f, new FutureCallback<byte[]>() {
      @Override
      public void onSuccess(byte[] bytes) {
        watcher.watchTriggered(bytes);
      }

      @Override
      public void onFailure(Throwable throwable) {
        watcher.watchFailed(throwable);
      }
    });
  }

  public PersistentWatch watchData(final String path, final PersistentWatcher<byte[]> watcher) {
    Preconditions.checkState(isRunning());
    final PersistentWatch watch = new PersistentWatch();
    watchData(watch, path, watcher);
    return watch;
  }

  public void watchChildren(final PersistentWatch watch, final String path, final PersistentWatcher<List<String>> watcher) {

    Preconditions.checkState(isRunning());

    if (watch.isCancelled()) {
      return;
    }

    final ListenableFuture<List<String>> f = getChildren(path, new Watcher() {
      @Override
      public void process(WatchedEvent event) {
        watchChildren(path, watcher);
      }
    });

    Futures.addCallback(f, new FutureCallback<List<String>>() {
      @Override
      public void onSuccess(List<String> children) {
        watcher.watchTriggered(children);
      }

      @Override
      public void onFailure(Throwable throwable) {
        watcher.watchFailed(throwable);
      }
    });

  }

  public PersistentWatch watchChildren(final String path, final PersistentWatcher<List<String>> watcher) {
    Preconditions.checkState(isRunning());
    final PersistentWatch watch = new PersistentWatch();
    watchChildren(watch, path, watcher);
    return watch;
  }

  private void watchNode(final PersistentWatch watch, final String path, final PersistentWatcher<Stat> watcher) {

    Preconditions.checkState(isRunning());

    if (watch.isCancelled()) {
      return;
    }

    final ListenableFuture<Stat> f = stat(path, new Watcher() {
      @Override
      public void process(WatchedEvent event) {
        watchNode(watch, path, watcher);
      }
    });

    Futures.addCallback(f, new FutureCallback<Stat>() {
      @Override
      public void onSuccess(Stat stat) {
        watcher.watchTriggered(stat);
      }

      @Override
      public void onFailure(Throwable throwable) {
        watcher.watchFailed(throwable);
      }
    });

  }

  public PersistentWatch watchNode(final String path, final PersistentWatcher<Stat> watcher) {
    Preconditions.checkState(isRunning());
    final PersistentWatch watch = new PersistentWatch();
    watchNode(watch, path, watcher);
    return watch;
  }

}
