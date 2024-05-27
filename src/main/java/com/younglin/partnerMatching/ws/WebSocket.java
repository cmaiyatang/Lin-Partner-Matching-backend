package com.younglin.partnerMatching.ws;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.younglin.partnerMatching.common.ErrorCode;
import com.younglin.partnerMatching.contant.ChatTypeEnum;
import com.younglin.partnerMatching.exception.BusinessException;
import com.younglin.partnerMatching.model.domain.ChatMessage;
import com.younglin.partnerMatching.model.domain.ChatUserLink;
import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.model.request.ChatRequest.MessageSendRequest;
import com.younglin.partnerMatching.model.vo.ChatMessageVO;
import com.younglin.partnerMatching.model.vo.ChatTeamMessageVO;
import com.younglin.partnerMatching.service.ChatMessageService;
import com.younglin.partnerMatching.service.ChatUserLinkService;
import com.younglin.partnerMatching.utils.GetHttpSessionUtil;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.younglin.partnerMatching.contant.RedisKeyConstant.USER_LOGIN_STATE;


@Component
@Slf4j
@ServerEndpoint(value = "/websocket/{friendId}/{teamId}", configurator = GetHttpSessionUtil.class)
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

    /**
     * 用来记录当前登录用户id和该session进行绑定
     */
    private static final Map<String, Session> SESSIONS = new HashMap<String, Session>();

    /**
     * 记录队伍的连接信息
     * 服务器维护队伍的 WebSocket 连接：
     * 服务器创建一个 WebSocket 连接来代表整个队伍或房间，该连接可以被所有成员共享和使用。
     */
    private static final Map<String, ConcurrentHashMap<String, WebSocket>> TEAMROOMS = new HashMap<>();

    //todo 房间在线人数

    private HttpSession httpSession;

    private Session session;

    private User currentUser;

    private static Integer onlineCount = 0;

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocket.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocket.onlineCount--;
    }


    /**
     * 连接建立成功调用的方法，初始化昵称、session
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config, @PathParam(value = "friendId") String friendId, @PathParam(value = "teamId") String teamId) {
        // 连接创建的时候，从 ApplicationContext 获取到 Bean 进行初始化
        chatUserLinkService = WebSocket.applicationContext.getBean(ChatUserLinkService.class);
        chatMessageService = WebSocket.applicationContext.getBean(ChatMessageService.class);
        //拿到登录用户的id 获取当前登录用户
        httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());

        currentUser = (User) httpSession.getAttribute(USER_LOGIN_STATE);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long curUserId = currentUser.getId();
        //将session作为websocket对象的一个属性，方便队伍聊天功能调用
        this.session = session;

        //判断是队伍聊天还是好友聊天
        if (!"NaN".equals(teamId)) {
            //如果该队伍聊天室已开启
            if (TEAMROOMS.containsKey(teamId)) {
                ConcurrentHashMap<String, WebSocket> teamRoom = TEAMROOMS.get(teamId);
                teamRoom.put(String.valueOf(curUserId), this);

            } else {
                //队伍聊天 this指的是当前类的对象
                ConcurrentHashMap<String, WebSocket> teamRoom = new ConcurrentHashMap<>();
                teamRoom.put(String.valueOf(curUserId), this);
                TEAMROOMS.put(String.valueOf(teamId), teamRoom);
            }
            //队伍人数加一
            addOnlineCount();
            log.info("用户{}进入队伍聊天室{},队伍在线人数", curUserId, teamId, onlineCount);
        } else {
            //将用户id和当前websocket session存储起来
            SESSIONS.put(String.valueOf(curUserId), session);
            //保存用户-friend表连接数据
            this.setConnectFriends(curUserId, Long.valueOf(friendId));
        }


    }


    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息封装对象
     */
    @OnMessage
    public void onMessage(String message) {
        if (Objects.equals(message, "ping")) {
            return;
        }
        if (StringUtils.isBlank(message)) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Long userId = currentUser.getId();
        //将message转换为ChatMessage对象
        log.info("服务端收到用户username={}的消息:{}", userId, message);
        //将前端发送的json数据解析成对象
        MessageSendRequest messageRequest = new Gson().fromJson(message, MessageSendRequest.class);
        String sendMessage = messageRequest.getMessage();
        Long friendId = messageRequest.getFriendId();
        Integer chatType = messageRequest.getChatType();

        if (chatType == 0) {
            //好友聊天
            friendChat(userId, friendId, sendMessage);
        } else if (chatType == 1) {
            //队伍聊天
            teamChat(userId, friendId, sendMessage);

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
    public void onClose(@PathParam(value = "teamId") Long teamId) {
        String teamIdString = String.valueOf(teamId);
        String curUserIdString = String.valueOf(currentUser.getId());
        if (!"NaN".equals(teamIdString)) {
            TEAMROOMS.get(teamIdString).remove(curUserIdString);
            if (getOnlineCount() >= 1) {
                //队伍成员退出聊天时在线人数减一
                subOnlineCount();
                log.info("用户{}退出队伍聊天室", curUserIdString);
            }
        } else {
            //好友聊天
            SESSIONS.remove(curUserIdString);
        }
    }

    /**
     * 私聊
     *
     * @param userId
     * @param friendId
     * @param sendMessage
     */
    private void friendChat(Long userId, Long friendId, String sendMessage) {
        //私聊
        Session session = SESSIONS.get(String.valueOf(friendId));
        if (session != null) {
            try {
                //通过方法获取到WebSocketVO对象
                ChatMessageVO messageResult = chatMessageService.getPrivateChatResult(userId, friendId, sendMessage, ChatTypeEnum.PRIVATE_CHAT.getValue());

                //转为json
                String messageResultToJson = new Gson().toJson(messageResult);

                session.getBasicRemote().sendText(messageResultToJson);
                log.info("发送给用户username={}，消息：{}", friendId, messageResult.getMessage());

            } catch (Exception e) {
                e.printStackTrace();
                log.info("发送消息出错啦！！");
            }
        } else {
            log.info("用户id：{}不在线或系统异常", friendId);
        }
    }


    //队伍聊天
    private void teamChat(Long userId, Long friendId, String sendMessage) {
        //队伍聊天   string为队员id
        ConcurrentHashMap<String, WebSocket> concurrentHashMap = TEAMROOMS.get(String.valueOf(friendId));
        //给队伍中每个成员发送消息
        for (String key : concurrentHashMap.keySet()) {
            //如果当前登录用户的id等于key，不发送消息，就是不自己给自己发消息
            if (Objects.equals(key, String.valueOf(userId))) {
                continue;
            }
            WebSocket webSocket = concurrentHashMap.get(key);
            if (webSocket != null) {
                //发送消息
                try {
                    //通过方法获取到WebSocketVO对象
                    ChatTeamMessageVO messageResult = chatMessageService.getTeamChatResult(userId, friendId, sendMessage, ChatTypeEnum.TEAM_CHAT.getValue());

                    //转为json
                    String messageResultToJson = new Gson().toJson(messageResult);

                    webSocket.session.getBasicRemote().sendText(messageResultToJson);
                    log.info("用户{}在队伍{}发送消息：{}", userId, friendId, messageResult.getMessage());

                } catch (IOException e) {
                    e.printStackTrace();
                    log.info("发送消息出错啦！！");
                }
            } else {
                log.info("队伍id：{}不在线或系统异常,ket:{}", friendId, key);
            }
        }
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

        QueryWrapper<ChatUserLink> linkQueryWrapper1 = new QueryWrapper<>();
        linkQueryWrapper1.eq("userId", friendId);
        linkQueryWrapper1.eq("friendId", curUserId);

        ChatUserLink oneChange = chatUserLinkService.getOne(linkQueryWrapper);

        if (one == null) {
            chatUserLinkService.save(chatUserLink);
        }
        if (oneChange == null) {
            chatUserLinkService.save(chatUserLinkChange);
        }


    }


}
