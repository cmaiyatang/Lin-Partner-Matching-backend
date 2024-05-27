package com.younglin.partnerMatching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.younglin.partnerMatching.common.ErrorCode;
import com.younglin.partnerMatching.common.ResultUtils;
import com.younglin.partnerMatching.contant.ChatTypeEnum;
import com.younglin.partnerMatching.contant.TeamStatusEnum;
import com.younglin.partnerMatching.exception.BusinessException;
import com.younglin.partnerMatching.mapper.UserMapper;
import com.younglin.partnerMatching.model.domain.Team;
import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.model.vo.ChatMessageVO;
import com.younglin.partnerMatching.model.vo.ChatTeamMessageVO;
import com.younglin.partnerMatching.model.vo.WebsocketVO;
import com.younglin.partnerMatching.service.ChatMessageService;
import com.younglin.partnerMatching.model.domain.ChatMessage;
import com.younglin.partnerMatching.mapper.ChatMessageMapper;
import com.younglin.partnerMatching.service.TeamService;
import com.younglin.partnerMatching.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author chenyanglin
 * @description 针对表【chat_message(聊天内容详情表)】的数据库操作Service实现
 * @createDate 2024-05-05 19:22:02
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
        implements ChatMessageService {
    @Resource
    private UserService userService;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Resource
    private TeamService teamService;


    /**
     * 查询聊天记录
     *
     * @param userId
     * @param friendId
     * @param currentUser
     * @return
     */
    @Override
    public List<ChatMessageVO> getChatRecord(Long userId, Long friendId, User currentUser) {
        if (userId == null || friendId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        User nowUser = userService.getById(userId);
        User friendUser = userService.getById(friendId);
        if (nowUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        if (friendUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "好友不存在呦~");
        }
        WebsocketVO nowWebsocketVO = new WebsocketVO();
        WebsocketVO friendWebsocketVO = new WebsocketVO();
        BeanUtils.copyProperties(nowUser, nowWebsocketVO);
        BeanUtils.copyProperties(friendUser, friendWebsocketVO);


        //查询聊天记录
        QueryWrapper<ChatMessage> chatMessageQueryWrapper = new QueryWrapper<>();
        // 设置查询条件，使用 OR 逻辑捕获发送和接收的消息
        // 使用 in 方法简化条件构造
        chatMessageQueryWrapper.and(wrapper ->
                wrapper.in("userId", userId, friendId)
                        .in("friendId", userId, friendId)
                        .eq("chatType", ChatTypeEnum.PRIVATE_CHAT.getValue())
        );

        List<ChatMessage> chatMessages = chatMessageMapper.selectList(chatMessageQueryWrapper);
        if (CollectionUtils.isEmpty(chatMessages)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "还未与该小伙伴聊天哦~ 打个招呼吧！");
        }
        List<ChatMessageVO> chatMessageVOList = new ArrayList<>();

        for (ChatMessage chatMessage : chatMessages) {
            // 创建 ChatMessageVO 对象
            ChatMessageVO chatMessageVO = new ChatMessageVO();
            chatMessageVO.setMessage(chatMessage.getMessage());
            chatMessageVO.setSendTime(chatMessage.getSendTime());

            // 设置发送者和接收者用户信息
            chatMessageVO.setNowUser(nowWebsocketVO);
            chatMessageVO.setFriendUser(friendWebsocketVO);

            // 判断消息是否为当前登录用户发送
            if (Objects.equals(chatMessage.getUserId(), userId)) {
                chatMessageVO.setIsMy(true);
            } else {
                chatMessageVO.setIsMy(false);
            }

            // 将封装好的 ChatMessageVO 添加到列表中
            chatMessageVOList.add(chatMessageVO);
        }

        return chatMessageVOList;
    }

    /**
     * 查询队伍聊天记录
     *
     * @param userId
     * @param teamId
     * @param currentUser
     * @return
     */
    @Override
    public List<ChatTeamMessageVO> getTeamChatMessage(Long userId, Long teamId, User currentUser) {
        if (userId == null || teamId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        User nowUser = userService.getById(userId);
        Team team = teamService.getById(teamId);

        if (nowUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在诶~");
        }
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在呢~");
        }
        //根据teamId查询聊天记录
        QueryWrapper<ChatMessage> chatMessageQueryWrapper = new QueryWrapper<>();
        chatMessageQueryWrapper.eq("friendId", team.getId());
        chatMessageQueryWrapper.eq("chatType", ChatTypeEnum.TEAM_CHAT.getValue());
        List<ChatMessage> chatMessageList = this.list(chatMessageQueryWrapper);

        if (CollectionUtils.isEmpty(chatMessageList)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "暂时没有队伍聊天信息哦");
        }
        //转换为messageVO
        List<ChatTeamMessageVO> chatMessageVOList = new ArrayList<>();
        for (ChatMessage message : chatMessageList) {
            ChatTeamMessageVO teamChatMessageVO = new ChatTeamMessageVO();
            //根据每个队伍消息的发送者id查询用户信息
            Long sendUserId = message.getUserId();
            User sendUser = userService.getById(sendUserId);
            WebsocketVO sendUserWebsocket = new WebsocketVO();
            BeanUtils.copyProperties(sendUser, sendUserWebsocket);

            if (Objects.equals(sendUserId, nowUser.getId())) {
                teamChatMessageVO.setIsMy(true);
            } else {
                teamChatMessageVO.setIsMy(false);
            }
            teamChatMessageVO.setSendUser(sendUserWebsocket);
            teamChatMessageVO.setMessage(message.getMessage());
            teamChatMessageVO.setSendTime(message.getSendTime());
            chatMessageVOList.add(teamChatMessageVO);
        }

        return chatMessageVOList;
    }


    /**
     * 封装聊天信息
     *
     * @param sendUserId
     * @param friendId
     * @param message
     * @return
     */
    @Override
    public ChatMessageVO getPrivateChatResult(Long sendUserId, Long friendId, String message,Integer chatType) {

        //查询用户信息
        User sendUser = userService.getById(sendUserId);
        if (sendUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        }
        WebsocketVO sendUserWebsocketVO = new WebsocketVO();
        BeanUtils.copyProperties(sendUser, sendUserWebsocketVO);

        User friendUser = userService.getById(friendId);
        if (friendUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "好友不存在");
        }
        WebsocketVO friendWebsocketVO = new WebsocketVO();
        BeanUtils.copyProperties(friendUser, friendWebsocketVO);

        //封装聊天消息对象
        ChatMessageVO chatMessageVO = new ChatMessageVO();
        //todo 检查逻辑 前后端交互
        //对于消息接收者来说
        chatMessageVO.setNowUser(friendWebsocketVO);
        chatMessageVO.setFriendUser(sendUserWebsocketVO);
        chatMessageVO.setIsMy(false);
        chatMessageVO.setMessage(message);
        chatMessageVO.setChatType(chatType);
        chatMessageVO.setSendTime(new Date());

        return chatMessageVO;
    }

    @Override
    public ChatTeamMessageVO getTeamChatResult(Long sendUserId, Long friendId, String message,Integer chatType) {

        //查询用户信息
        User sendUser = userService.getById(sendUserId);
        if (sendUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        }
        WebsocketVO sendUserWebsocketVO = new WebsocketVO();
        BeanUtils.copyProperties(sendUser, sendUserWebsocketVO);

        User friendUser = userService.getById(friendId);
        if (friendUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "好友不存在");
        }
        WebsocketVO friendWebsocketVO = new WebsocketVO();
        BeanUtils.copyProperties(friendUser, friendWebsocketVO);

        //封装聊天消息对象
        ChatTeamMessageVO chatTeamMessageVO = new ChatTeamMessageVO();
        chatTeamMessageVO.setIsMy(false);
        chatTeamMessageVO.setSendUser(sendUserWebsocketVO);
        chatTeamMessageVO.setMessage(message);
        chatTeamMessageVO.setChatType(chatType);
        chatTeamMessageVO.setSendTime(new Date());

        return chatTeamMessageVO;
    }


}




