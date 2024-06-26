 # 介绍
 
 **实现好友/队伍聊天功能,用户可以寻找小伙伴,建立小队.**

**为了解决分布式定时任务重复执行的问题,使用锁分布式锁来限制多台服务器重复执行方法**

## 数据库表设计

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

/**
  用户-队伍表
 */

    id         bigint auto_increment comment 'id' primary key,
    userId     bigint                              null comment '用户id',
    teamId     bigint                              null comment '队伍id',
    joinTime   datetime                            null comment '用户加入时间',

/**
  聊天内容详情表
 */

    id         bigint auto_increment comment 'id' primary key,
    userId   bigint                              not null comment '消息发送用户id',
    friendId bigint                              not null comment '消息接收用户id',
    message    varchar(512)                        null comment '消息接收用户id',
    chatType   tinyint                             null comment '消息类型 0-好友 1-队伍',
    sendTime   TIMESTAMP                           null comment '消息发送时间',

/**
  聊天用户关系表
 */
 
    id             bigint auto_increment comment 'id' primary key,
    userId       bigint                              not null comment '消息发送用户id',
    friendId     bigint                              not null comment '消息接收用户id',



 ## 分布式锁

锁：有多个线程执行任务且都要访问共享资源时（如取同一张卡里的钱），为了解决数据同步等一系列问题，需要让所有线程去竞争，这里就是抢锁，只有抢到锁的线程才能访问共享资源。

**伙伴匹配项目里分布式锁的运用**

## 定时任务

定时执行某个任务（添加数据，查询数据等），

如果服务只部署在一台服务器上，没有使用定时任务没有问题，但如果服务部署在多台服务器上时，每台服务器都会去执行定时任务，如果是查询数据还好，但如果要增加，修改数据，那后果非常严重，所以我们要控制只有一台服务器能执行定时任务

**如何解决多台服务器执行定时任务的问题，**

**方案：**

​	1.只将定时任务部署在一台服务器上，麻烦，不好维护

​	2.在定时任务里判断此台服务器是否为我们指定的服务器，可以通过ip来判断，可以将服务器ip存在数据库，Redis，注册中心（nacos，zookeeper）中

​	3.分布式锁：让抢到锁的用户，在数据库，Redis，注册中心中保存自己的标识，当其他用户执行定时任务时先查询然后比较，此时已经有用户在执行定时任务了，其他用户等待。

**注意：**

​	1.**抢完锁后，必须要释放锁，必须设置过期时间！！**

​	2.且执行完定时任务后，要释放锁，才能让其他用户继续抢锁

​	3.如果执行方法的时候锁过期了

  // 每天执行，预热推荐用户 //自己设置时间测试
  
    @Scheduled(cron = "0 5 23 * * *")   
    public void doCacheRecommendUser() {
        RLock lock = redissonClient.getLock("younglin:precachejob:docache:lock");

        try {
            // 只有一个线程能获取到锁
            
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                System.out.println("getLock: " + Thread.currentThread().getId());
                for (Long userId : mainUserList) {
                    //查数据库
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    Page<User> userPage = userService.page(new Page<>(1, 20), queryWrapper);
                    String redisKey = String.format("younglin:user:recommend:%s", mainUserList);
                    ValueOperations valueOperations = redisTemplate.opsForValue();
                    //写缓存,30s过期
                    try {
                        valueOperations.set(redisKey, userPage, 30000, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("redis set key error", e);
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
        } finally {
            // 只能释放自己的锁
            
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }

## 前端跨域问题

**浏览器的同源策略，安全** **只会在前端发生！！！，如果后端使用HttpClient调用不会发生异常**

协议，域名（ip地址），端口号

这三个任意一个不同都是造成跨域

浏览器已经请求的后端，但后端没有返回 access-cros的响应头，浏览器不接受

**后端解决方式**

1.@cros注解

2.定义配置类

3.@Bean filter

前端解决：

请求拦截器，加上allow-cros- *



**还可以配置nginx将请求转发到后端**

配置了nginx反向代理后监听的是80端口   http://ip地址:80   不写80默认的是80端口，

前端的baseUrl不需要加:8080   

```js
'http://8.134.203.235/api',
```

```nginx
#PROXY-START/

location ^~ /api/
{
    proxy_pass http://8.134.203.235:8080;
    proxy_set_header Host 8.134.203.235;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header REMOTE-HOST $remote_addr;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection $connection_upgrade;
    proxy_http_version 1.1;
    # proxy_hide_header Upgrade;

    add_header X-Cache $upstream_cache_status;

    #Set Nginx Cache
    
    
    set $static_fileL6YroQSB 0;
    if ( $uri ~* "\.(gif|png|jpg|css|js|woff|woff2)$" )
    {
    	set $static_fileL6YroQSB 1;
    	expires 1m;
        }
    if ( $static_fileL6YroQSB = 0 )
    {
    add_header Cache-Control no-cache;
    }
}

#PROXY-END/
```

地址有api时nginx会帮我们调用http://8.134.203.235:8080/api/ 接口

**问题:**
  
  **1.多服务器session共享问题**
​	当服务部署在多台服务器中时，用户请求通过Nginx服务器反向代理加负载均衡的方式分发到不同的服务器，但如果用户先访问的服务器A，此时，session保存在服务器A中，下次请求客户端携带cookie访问另外一台服务器的时候服务器B并不认识用户，也就拿不到保存在session中的信息

​	如何解决：

​	1.中间件，可以将session存储在中间件中，如Redis，MySQL，每台服务器都从Redis中取出session，Spring-data-session    

​	缺点：依赖性太强，如果这个中间件挂掉了，大家都无法工作，当然也可以部署多台中间件

​	2.服务器之间通过网络通信共享sessino，会增加每台服务器的网络开销，不推荐

​	3.通过memcache同步session，

分布式可能遇到的问题

  





 
