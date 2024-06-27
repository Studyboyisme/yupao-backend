package com.linyu.service;

import com.linyu.model.User;

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
    void test(){
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //增
        valueOperations.set("rainString", "rain");
        valueOperations.set("rainInt", 1);
        valueOperations.set("rainDouble", 2.0);
        User user = new User();
        user.setId(1L);
        user.setUsername("rain");
        valueOperations.set("rainUser", user);

        //查
        Object rain = valueOperations.get("rainString");
        Assertions.assertTrue("rain".equals((String) rain));
        rain = valueOperations.get("rainInt");
        Assertions.assertTrue(1 == (Integer) rain);
        rain = valueOperations.get("rainDouble");
        Assertions.assertTrue(2.0 == (Double) rain);
        System.out.println(valueOperations.get("rainUser"));
//        valueOperations.set("rainString", "rainbow");
        redisTemplate.delete("rainString");
    }
}
