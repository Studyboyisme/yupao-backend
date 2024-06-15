package com.linyu.service;

import com.linyu.model.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
public class UserServiceTest {

    @Resource
    private UserService userService;

    @Test
    public void testAddUser(){
        User user = new User();
        user.setUsername("jack");
        user.setUserAccount("123");
        user.setAvatarUrl("xxx");
        user.setGender(0);
        user.setUserPassword("xxx");
        user.setPhone("123");
        user.setEmail("456");
        System.out.println(user.getId());
        boolean result = userService.save(user);
        Assertions.assertEquals(true, result);
    }

    @Test
    void userRegister(){
        String userAccount = "linyu";
        String userPassword = "";
        String checkPassword = "123456";
        String planetCode = "1";
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assertions.assertEquals(-1, result);

        userAccount = "lin";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assertions.assertEquals(-1, result);

        userAccount = "linyu";
        userPassword = "123456";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assertions.assertEquals(-1, result);

        userAccount = "lin yu";
        userPassword = "12345678";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assertions.assertEquals(-1, result);

        checkPassword = "123456789";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assertions.assertEquals(-1, result);

        userAccount = "StudyBoy";
        checkPassword = "12345678";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assertions.assertEquals(-1, result);

        userAccount = "marry";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assertions.assertTrue(result > 0);
    }

    /**
     * 测试根据id集合查询数据
     */
    @Test
    void testGetUsersByIds(){
        List<Long> ids = Arrays.asList(4L, 8L, 9L);
        List<User> users = userService.getUsersByIds(ids);
        assertFalse(users.isEmpty());
        users.forEach(user -> log.info(user.toString()));
    }
}