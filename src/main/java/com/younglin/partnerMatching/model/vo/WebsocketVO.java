package com.younglin.partnerMatching.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class WebsocketVO implements Serializable {
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
}
