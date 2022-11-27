package com.lxc.distrubutedlock.demo.service;

import com.lxc.distrubutedlock.demo.lock.DistributedLockClient;
import com.lxc.distrubutedlock.demo.lock.DistributedRedisLock;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author Frank_lin
 * @date 2022/11/18
 */
@Component
public class StockService {
    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    private DistributedLockClient distributedLockClient;

    public void deduct() {
        DistributedRedisLock redisLock = distributedLockClient.getRedisLock("lock");
        redisLock.lock();

        try {

            String stock = redisTemplate.opsForValue().get("stock");

            if (Strings.isNotBlank(stock)) {
                Integer integer = Integer.valueOf(stock);
                if (integer > 0) {
                    redisTemplate.opsForValue().set("stock", String.valueOf(--integer));
                }
            }
            try {
                TimeUnit.SECONDS.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            test();
        } finally {
            redisLock.unlock();
        }
    }

    public void test(){
        DistributedRedisLock lock = distributedLockClient.getRedisLock("lock");
        lock.lock();
        lock.unlock();
    }
}
