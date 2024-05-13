package com.younglin.partnerMatching.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.younglin.partnerMatching.common.BaseResponse;
import com.younglin.partnerMatching.common.ErrorCode;
import com.younglin.partnerMatching.common.ResultUtils;
import com.younglin.partnerMatching.exception.BusinessException;
import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.model.request.UserRequest.FriendQueryRequest;
import com.younglin.partnerMatching.model.request.UserRequest.UserLoginRequest;
import com.younglin.partnerMatching.model.request.UserRequest.UserRegisterRequest;
import com.younglin.partnerMatching.model.request.UserRequest.UserUpdateRequest;
import com.younglin.partnerMatching.model.vo.UserVo;
import com.younglin.partnerMatching.service.ChatUserLinkService;
import com.younglin.partnerMatching.service.FriendService;
import com.younglin.partnerMatching.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.PathParam;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户接口
 *
 * @author
 * @from
 */
@RestController
@RequestMapping("/friend")
@Slf4j
public class FriendController {

    @Resource
    private UserService userService;

    @Resource
    private FriendService friendService;

    @Resource
    private ChatUserLinkService chatUserLinkService;


    /**
     * 查询伙伴
     *
     * @param request
     * @return
     */
    @PostMapping("/searchFriends")
    public BaseResponse<List<UserVo>> searchFriends(@RequestBody FriendQueryRequest friendQueryRequest, HttpServletRequest request) {
        if (friendQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);
        Long userId = currentUser.getId();

        friendQueryRequest.setId(userId);

        //todo 设计伙伴缓存
        List<UserVo> friends = friendService.searchFriends(friendQueryRequest);

        return ResultUtils.success(friends);
    }

    /**
     * 删除好友
     *
     * @param id
     * @return
     */
    @PostMapping("/deleteFriend/{id}")
    public BaseResponse<Boolean> deleteFriend(@PathVariable(value = "id") Long id, HttpServletRequest request) {
        if (id == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        User currentUser = userService.getCurrentUser(request);

        boolean deleteResult = friendService.deleteFriend(currentUser.getId(), id);

        return ResultUtils.success(deleteResult);
    }


}






