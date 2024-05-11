package com.younglin.partnerMatching.model.vo;

import lombok.Data;
@Data
public class ChatUserLinkVO {


    /**
     * 消息发送用户id
     */
    private Long userId;

    /**
     * 消息接收用户id
     */
    private Long friendId;


}
