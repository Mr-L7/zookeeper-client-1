package org.robotninjas.zookeeper;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.lang.String;import java.lang.Void;import java.util.List;

public interface ZooKeeperClient {

  ListenableFuture<String> create(String path, byte[] data, CreateMode mode, List<ACL> acl);

  ListenableFuture<Void> delete(String path, int version);

  ListenableFuture<Stat> stat(String path);

  ListenableFuture<Stat> statAndWatch(String path, Watcher watcher);

  ListenableFuture<List<ACL>> getAcl(String path);

  ListenableFuture<Stat> setAcl(String path, int version, List<ACL> acl);

  ListenableFuture<byte[]> getAndWatchData(String path, Watcher watcher);

  ListenableFuture<Stat> setData(String path, int version, byte[] data);

  ListenableFuture<List<String>> getChildren(String path);

  ListenableFuture<List<String>> getAndWatchChildren(String path, Watcher watcher);

  PersistentWatch watchData(final String path, final PersistentWatcher<byte[]> watcher);

  PersistentWatch watchChildren(final String path, final PersistentWatcher<List<String>> watcher);

  PersistentWatch watchNode(final String path, final PersistentWatcher<Stat> watcher);

  }
