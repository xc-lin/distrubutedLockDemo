package com.lxc.distrubutedlock.demo.zk;

import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.*;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

public class ZkDistributedLock implements Lock {
    private String lockName;

    private ZooKeeper zooKeeper;

    private String currentNodePath;

    private static final String ROOT_PATH = "/locks";

    private static final ThreadLocal<Integer> THREAD_LOCAL = new ThreadLocal<>();

    public ZkDistributedLock(ZooKeeper zooKeeper, String lockName) {
        this.zooKeeper = zooKeeper;
        this.lockName = lockName;
        try {
            if (zooKeeper.exists(ROOT_PATH, false) == null) {
                zooKeeper.create(ROOT_PATH, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void lock() {
        this.tryLock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        try {
            // 先判断threadLocal中是否应有锁了
            Integer flag = THREAD_LOCAL.get();
            if (flag != null && flag > 0) {
                THREAD_LOCAL.set(flag + 1);
                return true;
            }

            // 创建znode节点
            currentNodePath = this.zooKeeper.create(ROOT_PATH + "/" + lockName + "-", null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            // 获取前置节点，如果前置节点为空，则获取锁成功，否则监听前置节点
            String prevNode = this.getPrevNode();
            if (prevNode != null) {
                // 利用闭锁思想，实现阻塞功能
                CountDownLatch countDownLatch = new CountDownLatch(1);
                // 再次判断zk中前置节点是否存在
                // 因为不具备原子性操作

                if (this.zooKeeper.exists(ROOT_PATH + "/" + prevNode, new Watcher() {
                    @Override
                    public void process(WatchedEvent watchedEvent) {
                        countDownLatch.countDown();
                    }
                }) == null) {
                    THREAD_LOCAL.set(1);
                    return true;
                }
                countDownLatch.await();
            }
            THREAD_LOCAL.set(1);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
//            try {
//                Thread.sleep(10);
//                tryLock();
//            } catch (InterruptedException ex) {
//                ex.printStackTrace();
//            }

        }
        return false;

    }

    private String getPrevNode() {
        // 获取根节点下的所有节点
        try {
            List<String> children = zooKeeper.getChildren(ROOT_PATH, false);
            if (CollectionUtils.isEmpty(children)) {
                throw new IllegalMonitorStateException("非法操作");
            }
            // 获取当前节点同一资源的锁
            List<String> nodes = children.stream().filter(node -> StringUtils.startsWith(node, lockName + "-")).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(nodes)) {
                throw new IllegalMonitorStateException("非法操作");
            }
            // 排好序
            Collections.sort(nodes);
            // 获取当前node的下标
            String currentNode = StringUtils.substringAfterLast(currentNodePath, "/");
            int index = Collections.binarySearch(nodes, currentNode);
            if (index < 0) {
                throw new IllegalMonitorStateException("非法操作");
            } else if (index > 0) {
                // 返回前置节点
                return nodes.get(index - 1);
            }
            // 如果当前节点就是第一个节点，则返回null；
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalMonitorStateException("非法操作" + e.getMessage());
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {
        try {
            THREAD_LOCAL.set(THREAD_LOCAL.get() - 1);
            if (THREAD_LOCAL.get() == 0) {
                this.zooKeeper.delete(currentNodePath, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
