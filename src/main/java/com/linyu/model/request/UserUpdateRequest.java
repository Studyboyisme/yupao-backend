package com.linyu.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员更新用户请求体
 */
@Data
public class UserUpdateRequest implements Serializable {
    private static final long serialVersionUID = 123456789L;

    private Long id;
    private String userAccount;
    private String userPassword;
    private String planetCode;
    private String username;
    private String avatarUrl;
    private Integer gender;
    private String phone;
    private String email;
    private Integer userRole;
    private Integer userStatus;
}
