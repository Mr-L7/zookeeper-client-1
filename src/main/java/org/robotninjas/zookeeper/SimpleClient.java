package org.robotninjas.zookeeper;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.ACL;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.zookeeper.ZooDefs.Ids;
import static org.apache.zookeeper.ZooDefs.Perms;

public class SimpleClient {

  Executor executor;
  ZooKeeperClient client;

  private void doElection(final String path) {
    final ListenableFuture<List<String>> childrenFuture = client.getChildren(path);
    final Function<List<String>, String> leaderFunc = new Function<List<String>, String>() {
      @Override
      public String apply(List<String> input) {
        Collections.sort(input);
        return input.get(0);
      }
    };
    final ListenableFuture<String> leaderFuture = Futures.transform(childrenFuture, leaderFunc, executor);
    Futures.addCallback(leaderFuture, new FutureCallback<String>() {
      @Override
      public void onSuccess(String result) {
        client.stat(result, new Watcher() {
          @Override
          public void process(WatchedEvent event) {
            doElection(path);
          }
        });
      }

      @Override
      public void onFailure(Throwable t) {}

    }, executor);
  }




  public static void main(String[] args) throws ExecutionException, InterruptedException {

    final ZooKeeperClientService clientService = new ZooKeeperClientService("localhost:2181");
    final ExecutorService executor = Executors.newCachedThreadPool();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        clientService.stopAndWait();
        executor.shutdown();
      }
    });
    clientService.startAndWait();

    executor.submit(new Runnable() {
      @Override
      public void run() {
        try {

          final List<ACL> acl = Lists.newArrayList(new ACL(Perms.ALL, Ids.ANYONE_ID_UNSAFE));
          final CreateMode mode = CreateMode.PERSISTENT;
          final String path = "/membership";
          final byte[] data = new byte[]{};

          try {
            clientService.create(path, data, mode, acl).get();
          } catch (Exception e) {
            if (!(e instanceof KeeperException.NodeExistsException)) {
              Throwables.propagate(e);
            }
          }

          clientService.watchChildren(path, new PersistentWatcher<List<String>>() {
            @Override
            public void watchTriggered(List<String> result) {
              System.out.print("Watch triggered ");
              for (final String member : result) {
                System.out.print(member + " ");
              }
              System.out.println();
            }

            @Override
            public void watchFailed(Throwable t) {
              System.out.println("Failed");
            }
          });

          for (int i = 0; i < 10; ++i) {
            System.out.println("Creating, " + i);
            final String child = clientService.create(path + "/child", data, CreateMode.EPHEMERAL_SEQUENTIAL, acl).get();
            System.out.println("child creaded: " + child);
          }

        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }
      }
    });

  }

}
