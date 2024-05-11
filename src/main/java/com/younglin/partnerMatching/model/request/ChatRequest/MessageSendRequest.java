package com.younglin.partnerMatching.model.request.ChatRequest;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@Data
public class MessageSendRequest {

    /**
     * 消息发送用户id
     */
    private Long userId;

    /**
     * 消息接收用户id
     */
    private Long friendId;

    /**
     * 消息内容
     */
    private String message;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
