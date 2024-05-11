package com.younglin.partnerMatching.model.request.ChatRequest;


import lombok.Data;

import java.io.Serializable;

@Data
public class MessageGetReqeust implements Serializable {

    /**
     * 消息发送者id
     */
    private Long userId;

    /**
     * 消息接收者id
     */
    private Long friendId;

}
