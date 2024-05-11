package com.younglin.partnerMatching.model.request.ChatRequest;

import lombok.Data;

@Data
public class ChatConnectRequest {

    /**
     * 消息发送用户id
     */
    private Long userId;

    /**
     * 消息接收用户id
     */
    private Long friendId;

}
