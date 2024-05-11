package com.younglin.partnerMatching.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.younglin.partnerMatching.model.domain.ChatUserLink;
import com.younglin.partnerMatching.model.request.ChatRequest.ChatDeleteRequest;

/**
* @author chenyanglin
* @description 针对表【chat_user_link(聊天用户关系表)】的数据库操作Service
* @createDate 2024-05-05 19:22:13
*/
public interface ChatUserLinkService extends IService<ChatUserLink> {

    /**
     * 删除用户聊天关系
     * @param chatDeleteRequest
     * @return
     */
    boolean deleteConnection(ChatDeleteRequest chatDeleteRequest);
}
