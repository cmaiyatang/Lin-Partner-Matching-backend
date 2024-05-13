package com.younglin.partnerMatching.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.younglin.partnerMatching.common.ErrorCode;
import com.younglin.partnerMatching.exception.BusinessException;
import com.younglin.partnerMatching.mapper.UserMapper;
import com.younglin.partnerMatching.model.domain.ChatUserLink;
import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.model.request.UserRequest.FriendQueryRequest;
import com.younglin.partnerMatching.model.vo.UserVo;
import com.younglin.partnerMatching.service.ChatUserLinkService;
import com.younglin.partnerMatching.service.FriendService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 *
 * @author
 * @from
 */
@Service
@Slf4j
public class FriendServiceImpl extends ServiceImpl<UserMapper, User>
        implements FriendService {
    @Resource
    private ChatUserLinkService chatUserLinkService;

    /**
     * 查询好友
     *
     * @param friendQueryRequest
     * @return
     */
    @Override
    public List<UserVo> searchFriends(FriendQueryRequest friendQueryRequest) {
        if (friendQueryRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        Long userId = friendQueryRequest.getId();
        String friendName = friendQueryRequest.getFriendName();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        QueryWrapper<ChatUserLink> linkQueryWrapper = new QueryWrapper<>();
        linkQueryWrapper.eq("userId", userId);

        List<ChatUserLink> links = chatUserLinkService.list(linkQueryWrapper);
        if (CollectionUtils.isEmpty(links)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "还未有小伙伴哦");
        }
        //拿到所有伙伴的id
        List<Long> friendIdList = links.stream().map(ChatUserLink::getFriendId).collect(Collectors.toList());

        //查询伙伴
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", friendIdList);
        if (StringUtils.isNotBlank(friendName)){
            userQueryWrapper.like("username",friendName);
        }
        List<User> friends = this.list(userQueryWrapper);
        if (CollectionUtils.isEmpty(friends)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "还未有小伙伴哦");
        }

        // 遍历 friends 集合，并转换为 UserVo 对象
        List<UserVo> friendUserVoList = new ArrayList<>();
        for (User friend : friends) {
            // 创建新的 UserVo 对象
            UserVo friendUserVo = new UserVo();

            // 复制 friend 对象的属性到 friendUserVo 对象
            BeanUtils.copyProperties(friend, friendUserVo);

            // 将转换后的 UserVo 对象添加到列表中
            friendUserVoList.add(friendUserVo);
        }

        return friendUserVoList;
    }


    /**
     * 查询所有好友的id
     *
     * @param userId
     * @return
     */
    @Override
    public String searchFriendIds(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        QueryWrapper<ChatUserLink> linkQueryWrapper = new QueryWrapper<>();
        linkQueryWrapper.eq("userId", userId);
        List<ChatUserLink> links = chatUserLinkService.list(linkQueryWrapper);
        if (CollectionUtils.isEmpty(links)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "还未有小伙伴哦");
        }

        List<Long> friendIds = links.stream().map(ChatUserLink::getFriendId).collect(Collectors.toList());

        //拿到所有伙伴的id
        return JSON.toJSONString(friendIds);
    }

    @Override
    public boolean deleteFriend(Long userId, Long id) {
        if (id == null || userId == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        //查询好友存不存在
        User friend = this.getById(id);
        if (friend == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "好友不存在");
        }

        //删除好友
        QueryWrapper<ChatUserLink> chatUserLinkQueryWrapper = new QueryWrapper<>();
        chatUserLinkQueryWrapper.eq("userId", userId);
        chatUserLinkQueryWrapper.eq("friendId", id);
        boolean remove = chatUserLinkService.remove(chatUserLinkQueryWrapper);

        if (!remove) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }

        return remove;
    }

}

