package com.younglin.partnerMatching.model.request;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

/**
 * 队伍数据更新请求体
 */
@Data
public class TeamUpdateRequest {
    /**
     * 队伍id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String teamName;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 队伍最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 密码
     */
    private String password;


    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0-公开，1-私有，2-加密
     */
    private Integer status;


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
