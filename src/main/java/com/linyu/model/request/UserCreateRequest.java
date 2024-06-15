package com.linyu.model.request;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 管理员创建新用户请求体
 */
@Data
public class UserCreateRequest implements Serializable {
    private static final long serialVersionUID = 123456789L;

    private String userAccount;
    private String userPassword;
    private String checkPassword;
    private String planetCode;
    private String username;
    private String avatarUrl;
    private Integer gender;
    private String phone;
    private String email;
    private Integer userRole;
}
