package com.younglin.partnerMatching.ws;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.younglin.partnerMatching.common.ErrorCode;
import com.younglin.partnerMatching.exception.BusinessException;
import com.younglin.partnerMatching.model.domain.ChatMessage;
import com.younglin.partnerMatching.model.domain.ChatUserLink;
import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.model.request.ChatRequest.MessageSendRequest;
import com.younglin.partnerMatching.model.vo.ChatMessageVO;
import com.younglin.partnerMatching.service.ChatMessageService;
import com.younglin.partnerMatching.service.ChatUserLinkService;
import com.younglin.partnerMatching.utils.GetHttpSessionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.younglin.partnerMatching.contant.UserConstant.USER_LOGIN_STATE;


@Component
@Slf4j
@ServerEndpoint(value = "/websocket/{friendId}", configurator = GetHttpSessionUtil.class)
public class WebSocket implements ApplicationContextAware {
    @Resource
    private ChatUserLinkService chatUserLinkService;

    @Resource
    private ChatMessageService chatMessageService;

    // 全局静态变量，保存 ApplicationContext
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        WebSocket.applicationContext = applicationContext;
    }

    //用来记录当前登录用户id和该session进行绑定
    private static Map<Long, Session> map = new HashMap<Long, Session>();

    private HttpSession httpSession;

    private User currentUser;

    /**
     * 连接建立成功调用的方法，初始化昵称、session
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config, @PathParam(value = "friendId") Long friendId) {
        // 连接创建的时候，从 ApplicationContext 获取到 Bean 进行初始化
        chatUserLinkService = WebSocket.applicationContext.getBean(ChatUserLinkService.class);
        chatMessageService = WebSocket.applicationContext.getBean(ChatMessageService.class);
        //拿到登录用户的id 获取当前登录用户
        httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());

        currentUser = (User) httpSession.getAttribute(USER_LOGIN_STATE);
        Long curUserId = currentUser.getId();

        //将用户id和当前websocket session存储起来
        map.put(curUserId, session);

        //保存用户-friend表连接数据
        this.setConnectFriends(curUserId, friendId);

    }


    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息封装对象
     */
    @OnMessage
    public void onMessage(String message) {
        if (message == null || message == "") {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Long userId = currentUser.getId();
        //将message转换为ChatMessage对象
        log.info("服务端收到用户username={}的消息:{}", userId, message);
        //将前端发送的json数据解析成对象
        MessageSendRequest messageRequest = new Gson().fromJson(message, MessageSendRequest.class);
        String sendMessage = messageRequest.getMessage();
        Long friendId = messageRequest.getFriendId();
        Session session = map.get(friendId);
        if (session != null) {
            try {
                //通过方法获取到WebSocketVO对象
                ChatMessageVO messageResult = chatMessageService.getMessageResult(userId, friendId, sendMessage);

                //转为json
                String messageResultToJson = new Gson().toJson(messageResult);

                session.getBasicRemote().sendText(messageResultToJson);

            } catch (Exception e) {
                e.printStackTrace();
                log.info("发送消息出错啦！！");
            }
        }

        //将消息保存到数据库
        ChatMessage chatMessage = new ChatMessage();
        BeanUtils.copyProperties(messageRequest, chatMessage);
        chatMessage.setFriendId(friendId);
        chatMessage.setSendTime(new Date());

        chatMessageService.save(chatMessage);

    }


    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {


        map.remove(this);  //从set中删除
    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "出现异常~~~");
    }


    public void setConnectFriends(Long curUserId, Long friendId) {

        // 创建当前用户到朋友用户的连接
        ChatUserLink chatUserLink = new ChatUserLink();
        chatUserLink.setUserId(curUserId);
        chatUserLink.setFriendId(friendId);

        // 创建朋友用户到当前用户的连接
        ChatUserLink chatUserLinkChange = new ChatUserLink();
        chatUserLinkChange.setUserId(friendId);
        chatUserLinkChange.setFriendId(curUserId);

        // 检查是否已经存在连接
        QueryWrapper<ChatUserLink> linkQueryWrapper = new QueryWrapper<>();
        linkQueryWrapper.eq("userId", curUserId);
        linkQueryWrapper.eq("friendId", friendId);

        ChatUserLink one = chatUserLinkService.getOne(linkQueryWrapper);
        if (one == null) {
            //保存连接信息到chat-user-link表
            chatUserLinkService.save(chatUserLink);

            linkQueryWrapper.eq("userId", friendId);
            linkQueryWrapper.eq("friendId", curUserId);
            ChatUserLink anotherOne = chatUserLinkService.getOne(linkQueryWrapper);
            if (anotherOne == null) {
                chatUserLinkService.save(chatUserLinkChange);
            }

        }

    }


}
