package com.younglin.partnerMatching.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.model.request.UserRequest.FriendQueryRequest;
import com.younglin.partnerMatching.model.vo.UserVo;

import java.util.List;

/**
 * 好友服务接口
 *
 * @author
 * @from
 */
public interface FriendService extends IService<User> {

    /**
     * 查询伙伴
     * @return
     */
    List<UserVo> searchFriends(FriendQueryRequest friendQueryRequest);

    /**
     * 查询伙伴的id
     * @param userId
     * @return
     */
    String searchFriendIds(Long userId);

    /**
     * 删除好友
     * @param userId
     * @param id
     * @return
     */
    boolean deleteFriend(Long userId,Long id);
}
