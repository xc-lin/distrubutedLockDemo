package com.lxc.distrubutedlock.demo;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@SpringBootTest
class DistrubutedLockDemoApplicationTests {

    @Test
    void contextLoads() throws Exception {
        ZooKeeper zooKeeper = null;
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            zooKeeper =  new ZooKeeper("127.0.0.1:2181", 30000, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    Event.KeeperState state = watchedEvent.getState();
                    if (Event.KeeperState.SyncConnected.equals(state) && Event.EventType.None.equals(watchedEvent.getType())) {
                        System.out.println("获取链接" + watchedEvent);
                        countDownLatch.countDown();
                    }else if (Event.KeeperState.Closed.equals(state)){
                        System.out.println("关闭链接了");
                    }
                }

            });
            countDownLatch.await();
            System.out.println("一顿操作");
            // 节点新增
            // zooKeeper.create("/lxc/test","hello zookeeper".getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

            // 查询
            // 判断节点是否存在
            Stat stat = zooKeeper.exists("/lxc", false);
            System.out.println(stat);
            // 获取当前节点中的数据内容
            byte[] data = zooKeeper.getData("/lxc", true, stat);
            System.out.println(new String(data));
            // 获取当前节点的子节点

            List<String> children = zooKeeper.getChildren("/lxc", new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    System.out.println("节点的子节点发生变化。。。。");

                }
            });
            System.out.println(children);

            // 更新 版本号必须和当前节点的版本号一致否则更新失败，也可以指定为-1。代表不关心版本号
            zooKeeper.setData("/lxc","wwwww".getBytes(StandardCharsets.UTF_8),stat.getVersion());
            // 删除
            // zooKeeper.delete("/lxc/test1",-1);

            System.in.read();

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            zooKeeper.close();
        }

    }

}
