package com.younglin.partnerMatching.model.request.UserRequest;

import lombok.Data;

import java.io.Serializable;


/**
 * 更新用户信息请求体
 */
@Data
public class UserUpdateRequest implements Serializable {
    private String username;
    private String phone;
    private String email;
    private String profile;
    private Integer gender;
    private String avatarUrl;
}
