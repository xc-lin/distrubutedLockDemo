package com.lxc.distrubutedlock.demo.zk;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.CountDownLatch;

@Component
public class ZkClient {

    private ZooKeeper zooKeeper;

    @PostConstruct
    public void init() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            zooKeeper = new ZooKeeper("127.0.0.1:2181", 3000, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    Event.KeeperState state = watchedEvent.getState();
                    if (Event.KeeperState.SyncConnected.equals(state) && Event.EventType.None.equals(watchedEvent.getType())) {
                        System.out.println("获取到链接了:" + watchedEvent);
                        countDownLatch.countDown();
                    } else if (Event.KeeperState.Closed.equals(state)) {
                        System.out.println("关闭链接：" + watchedEvent);
                    }
                }
            });
            countDownLatch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @PreDestroy
    public void destroy() {
        // 释放zk的链接
        try {
            if (zooKeeper != null) {
                zooKeeper.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public ZkDistributedLock getLock(String lockName){
        return new ZkDistributedLock(zooKeeper, lockName);
    }
}
