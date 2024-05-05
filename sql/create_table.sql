-- auto-generated definition
create table tag
(
    id         bigint auto_increment comment '标签id'
        primary key,
    tagName    varchar(256)                        null comment '标签名称',
    userId     bigint                              null comment '用户id',
    parentId   bigint                              null comment '父标签 id',
    isParent   tinyint                             null comment '0-不是，1-父标签',
    createTime timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete   int       default 0                 not null comment '是否删除 0 ',
    constraint tag__tagName_uniIdex
        unique (tagName)
)
    comment '标签';

create index tag_userId_uniIdx
    on tag (userId);

/**
  队伍表
 */
create table team
(
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

)
    comment '队伍';

/**
  用户-队伍表
 */
create table user_team
(
    id         bigint auto_increment comment 'id' primary key,
    userId     bigint                              null comment '用户id',
    teamId     bigint                              null comment '队伍id',
    joinTime   datetime                            null comment '用户加入时间',

    createTime timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete   tinyint   default 0                 not null comment '是否删除'

)
    comment '用户队伍关系';

/**
  聊天界面表
 */
create table chat_page
(
    id             bigint auto_increment comment 'id' primary key,
    senderId       bigint                              not null comment '消息发送用户id',
    recevierId     bigint                              not null comment '消息接收用户id',
    senderOnline   tinyint                             null comment '发送者是否在线 1-在线 0-下线',
    recevierOnline tinyint                             null comment '接收者是否在线 1-在线 0-下线',
    isRead         tinyint                             null comment '消息是否已读 1-已读 0-未读',

    createTime     timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime     timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete       tinyint   default 0                 not null comment '是否删除'

)
    comment '聊天界面表';
/**
  聊天内容详情表
 */
create table chat_message
(
    id         bigint auto_increment comment 'id' primary key,
    senderId   bigint                              not null comment '消息发送用户id',
    recevierId bigint                              not null comment '消息接收用户id',
    message    varchar(512)                        null comment '消息接收用户id',
    sendTime   TIMESTAMP                           null comment '消息发送时间',
    type       int                                 null comment '消息类型 ',
    isLatest   tinyint                             null comment '是否是最后一条 1-是 0-不是',

    createTime timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete   tinyint   default 0                 not null comment '是否删除'

)
    comment '聊天内容详情表';


/**
  聊天用户关系表
 */
create table chat_user_link
(
    id             bigint auto_increment comment 'id' primary key,
    senderId       bigint                              not null comment '消息发送用户id',
    recevierId     bigint                              not null comment '消息接收用户id',

    createTime     timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime     timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete       tinyint   default 0                 not null comment '是否删除'

)
    comment '聊天用户关系表';
