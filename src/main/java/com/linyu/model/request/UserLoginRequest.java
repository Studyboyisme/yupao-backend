package com.linyu.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {
    private static final long serialVersionUID = 123456789L;

    private String userAccount;
    private String userPassword;
}
