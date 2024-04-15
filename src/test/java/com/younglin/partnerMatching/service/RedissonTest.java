package com.younglin.partnerMatching.service;

import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class RedissonTest {

    @Resource
    private RedissonClient redissonClient;

    @Test
    void test() {
        // list，数据存在本地 JVM 内存中
        List<String> list = new ArrayList<>();
        list.add("yupi");
        System.out.println("list:" + list.get(0));

        list.remove(0);

        // 数据存在 redis 的内存中
        RList<String> rList = redissonClient.getList("test-list");
        rList.add("yupi");
        System.out.println("rlist:" + rList.get(0));
        rList.remove(0);

        // map
        Map<String, Integer> map = new HashMap<>();
        map.put("yupi", 10);
        map.get("yupi");

        RMap<Object, Object> map1 = redissonClient.getMap("test-map");

        // set

        // stack


    }
}