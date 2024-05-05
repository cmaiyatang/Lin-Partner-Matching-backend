package com.younglin.partnerMatching;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.younglin.partnerMatching.common.ErrorCode;
import com.younglin.partnerMatching.contant.TeamStatusEnum;
import com.younglin.partnerMatching.exception.BusinessException;
import com.younglin.partnerMatching.model.domain.Team;
import com.younglin.partnerMatching.model.domain.UserTeam;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@Slf4j
public class RedissionLockTest {

    @Resource
    private RedissonClient redissonClient;

    @Test
    public void lockTest() {
        RLock lock = redissonClient.getLock("my:join_team");
        try {
            // 抢到锁并执行
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                System.out.println("获取到锁，开始执行业务");
                // 在这里执行你的业务逻辑
                // 例如：处理加入队伍的业务

                System.out.println("业务执行完毕");
            } else {
                System.out.println("未获取到锁，放弃执行业务");
            }
        } catch (Exception e) {
            log.error("业务执行出错", e);
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("释放锁");
                lock.unlock();
            }
        }
    }
}
