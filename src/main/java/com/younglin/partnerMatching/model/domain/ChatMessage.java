package com.younglin.partnerMatching.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天内容详情表
 * @TableName chat_message
 */
@TableName(value ="chat_message")
@Data
public class ChatMessage implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息发送用户id
     */
    private Long userId;

    /**
     * 消息接收用户id
     */
    private Long friendId;

    /**
     * 消息接收用户id
     */
    private String message;

    /**
     * 消息发送时间
     */
    private Date sendTime;


    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}