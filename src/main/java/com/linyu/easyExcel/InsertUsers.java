package com.linyu.easyExcel;

import com.linyu.mapper.UserMapper;
import com.linyu.model.User;

import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;

@Component
public class InsertUsers {

    @Resource
    private UserMapper userMapper;

    /**
     * 循环插入用户
     */
//    @Scheduled(initialDelay = 5000,fixedRate = Long.MAX_VALUE )
    public void doInsertUser() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 1000;
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
            user.setUsername("小狗");
            user.setUserAccount("luckyDog");
            user.setAvatarUrl("https://web-study-123.oss-cn-hangzhou.aliyuncs.com/%E5%B0%8F%E7%8B%97.jpg");
            user.setProfile("幸运狗");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("123456789108");
            user.setEmail("luckyDog@qq.com");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("369");
            user.setTags("[]");
            userMapper.insert(user);
        }
        stopWatch.stop();
        System.out.println( stopWatch.getLastTaskTimeMillis());

    }
}
