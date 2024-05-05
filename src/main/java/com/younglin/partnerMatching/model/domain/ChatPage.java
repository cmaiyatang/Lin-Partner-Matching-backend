package com.younglin.partnerMatching.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天界面表
 * @TableName chat_page
 */
@TableName(value ="chat_page")
@Data
public class ChatPage implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息发送用户id
     */
    private Long senderId;

    /**
     * 消息接收用户id
     */
    private Long recevierId;

    /**
     * 发送者是否在线
     */
    private Integer senderOnline;

    /**
     * 接收者是否在线
     */
    private Integer recevierOnline;

    /**
     * 消息是否已读
     */
    private Integer isRead;

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