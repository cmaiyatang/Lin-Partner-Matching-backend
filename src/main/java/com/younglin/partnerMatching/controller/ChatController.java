package com.younglin.partnerMatching.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.younglin.partnerMatching.common.BaseResponse;
import com.younglin.partnerMatching.common.ErrorCode;
import com.younglin.partnerMatching.common.ResultUtils;
import com.younglin.partnerMatching.exception.BusinessException;
import com.younglin.partnerMatching.model.domain.ChatMessage;
import com.younglin.partnerMatching.model.domain.ChatUserLink;
import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.model.request.ChatRequest.ChatConnectRequest;
import com.younglin.partnerMatching.model.request.ChatRequest.ChatDeleteRequest;
import com.younglin.partnerMatching.model.request.ChatRequest.MessageGetReqeust;
import com.younglin.partnerMatching.model.vo.ChatMessageVO;
import com.younglin.partnerMatching.model.vo.ChatTeamMessageVO;
import com.younglin.partnerMatching.service.ChatMessageService;
import com.younglin.partnerMatching.service.ChatUserLinkService;
import com.younglin.partnerMatching.service.UserService;
import org.apache.logging.log4j.message.Message;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
public class ChatController {
    @Resource
    private ChatUserLinkService chatUserLinkService;

    @Resource
    private UserService userService;

    @Resource
    private ChatMessageService chatMessageService;

    /**
     * 新增用户聊天关系
     *
     * @param chatConnectRequest
     * @return
     */
    @PostMapping("/connect")
    public BaseResponse<Boolean> connectUser(@RequestBody ChatConnectRequest chatConnectRequest) {

        if (chatConnectRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        ChatUserLink chatUserLink = new ChatUserLink();
        BeanUtils.copyProperties(chatConnectRequest, chatUserLink);

        boolean connectBuild = chatUserLinkService.save(chatUserLink);

        if (!connectBuild) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "建立聊天失败！");
        }

        return ResultUtils.success(connectBuild);
    }


    /**
     * 删除用户聊天关系
     *
     * @param chatDeleteRequest
     * @return
     */
    @PostMapping("/deleteConnection")
    public BaseResponse<Boolean> deleteConnectionUser(@RequestBody ChatDeleteRequest chatDeleteRequest) {

        if (chatDeleteRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        boolean deleted = chatUserLinkService.deleteConnection(chatDeleteRequest);

        if (!deleted) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败~");
        }

        return ResultUtils.success(deleted);
    }

    /**
     * 查询用户聊天关系
     *
     * @param request
     * @return
     */
    @GetMapping("/queryFriend")
    public BaseResponse<List<ChatUserLink>> queryConnectionUser(HttpServletRequest request) {

        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //当前登录用户
        User currentUser = userService.getCurrentUser(request);

        Long currentUserId = currentUser.getId();
        QueryWrapper<ChatUserLink> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", currentUserId).isNotNull("friendId");
        List<ChatUserLink> friendList = chatUserLinkService.list(queryWrapper);

        if (CollectionUtils.isEmpty(friendList)) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        return ResultUtils.success(friendList);
    }

    /**
     * 获取好友聊天记录
     * @param messageGetReqeust
     * @param request
     * @return
     */
    @PostMapping("/privateChatMessage")
    public BaseResponse<List<ChatMessageVO>> getFriendMessage(@RequestBody MessageGetReqeust messageGetReqeust, HttpServletRequest request) {
        if (messageGetReqeust == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Long userId = messageGetReqeust.getUserId();
        Long friendId = messageGetReqeust.getFriendId();

        User currentUser = userService.getCurrentUser(request);

        //查询聊天记录
        List<ChatMessageVO> chatRecord = chatMessageService.getChatRecord(userId, friendId, currentUser);

        return ResultUtils.success(chatRecord);
    }

    /**
     * 获取队伍聊天室聊天记录
     * @param messageGetReqeust
     * @return
     */
    @PostMapping("/teamChatMessage")
    public BaseResponse<List<ChatTeamMessageVO>> getTeamRoomMessage(@RequestBody MessageGetReqeust messageGetReqeust,HttpServletRequest request){
        if (messageGetReqeust == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = messageGetReqeust.getUserId();
        Long friendId = messageGetReqeust.getFriendId();
        User currentUser = userService.getCurrentUser(request);

        List<ChatTeamMessageVO> teamChatMessage = chatMessageService.getTeamChatMessage(userId, friendId, currentUser);

        return ResultUtils.success(teamChatMessage);
    }
 }















