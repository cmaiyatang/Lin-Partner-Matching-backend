数据库表设计

/**
  用户标签表
 */
 
    id         bigint auto_increment comment '标签id'
        primary key,
    tagName    varchar(256)                        null comment '标签名称',
    userId     bigint                              null comment '用户id',
    parentId   bigint                              null comment '父标签 id',
    isParent   tinyint                             null comment '0-不是，1-父标签',
    createTime timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete   int       default 0                 not null comment '是否删除 0 ',
        

/**
  队伍表
 */

    id          bigint auto_increment comment '队伍id'
        primary key,
    teamName    varchar(256)                        not null comment '队伍名称',
    description varchar(1024)                       null comment '队伍描述',
    maxNum      int                                 null comment '队伍最大人数',
    expireTime  timestamp                           null comment '过期时间',
    userId      bigint                              null comment '用户id',
    status      int       default 0                 not null comment '0-公开，1-私有，2-加密',
    password    varchar(512) comment '用户id'       null comment '密码',

    createTime  timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime  timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete    tinyint   default 0                 not null comment '是否删除'

/**
  用户-队伍表
 */

    id         bigint auto_increment comment 'id' primary key,
    userId     bigint                              null comment '用户id',
    teamId     bigint                              null comment '队伍id',
    joinTime   datetime                            null comment '用户加入时间',

    createTime timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete   tinyint   default 0                 not null comment '是否删除'


/**
  聊天内容详情表
 */

    id         bigint auto_increment comment 'id' primary key,
    userId   bigint                              not null comment '消息发送用户id',
    friendId bigint                              not null comment '消息接收用户id',
    message    varchar(512)                        null comment '消息接收用户id',
    chatType   tinyint                             null comment '消息类型 0-好友 1-队伍',
    sendTime   TIMESTAMP                           null comment '消息发送时间',
    createTime timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete   tinyint   default 0                 not null comment '是否删除'


/**
  聊天用户关系表
 */
 
    id             bigint auto_increment comment 'id' primary key,
    userId       bigint                              not null comment '消息发送用户id',
    friendId     bigint                              not null comment '消息接收用户id',

    createTime     timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime     timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete       tinyint   default 0                 not null comment '是否删除'

