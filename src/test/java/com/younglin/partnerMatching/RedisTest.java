package com.younglin.partnerMatching;
import java.util.Date;

import com.younglin.partnerMatching.model.domain.User;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate redisTemplate;

    @Test
    public void redisTest(){
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //增
        valueOperations.set("younglinString","chen");
        valueOperations.set("younglinInt",1);
        valueOperations.set("younglinDouble",2.0);
        User user = new User();
        user.setId(1L);
        user.setUsername("YoungLin");
        valueOperations.set("user",user);
        //查
        Object younglinString = valueOperations.get("younglinString");
        Assertions.assertEquals("chen", younglinString);
        Object younglinInt = valueOperations.get("younglinInt");
        Assertions.assertEquals(1, younglinInt);
        Object younglinDouble = valueOperations.get("younglinDouble");
        Assertions.assertEquals(2.0, younglinDouble);
        User user1 = (User)valueOperations.get("user");
        Assertions.assertEquals(1,user1.getId());
        Assertions.assertEquals("YoungLin",user1.getUsername());
        System.out.println(user1.getId());
        System.out.println(user1.getUsername());

        //改 就是重新set
        //删
        redisTemplate.delete("user");






    }

}
