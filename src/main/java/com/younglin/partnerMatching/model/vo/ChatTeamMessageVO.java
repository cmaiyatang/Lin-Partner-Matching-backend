package com.younglin.partnerMatching.model.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

@Data
public class ChatTeamMessageVO {

    /**
     * 是否为登录用户发送的消息
     */
    private Boolean isMy;

    /**
     * 消息发送者对象
     */
    private WebsocketVO sendUser;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息类型
     */
    private Integer chatType;

    /**
     * 消息发送时间
     */
    private Date sendTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
