package com.younglin.partnerMatching.model.vo;

import lombok.Data;

/**
 * 用户数据视图类
 */
@Data
public class UserVO {
    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String username;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 性别
     */
    private Integer gender;
    /**
     * 电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 标签列表
     */
    private String tags;
    /**
     * 个人简介
     */
    private String profile;

    /**
     * 好友id
     */
    private String friendIds;
}
