package com.younglin.partnerMatching.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.younglin.partnerMatching.common.ErrorCode;
import com.younglin.partnerMatching.exception.BusinessException;
import com.younglin.partnerMatching.mapper.ChatUserLinkMapper;
import com.younglin.partnerMatching.model.domain.ChatUserLink;
import com.younglin.partnerMatching.model.request.ChatRequest.ChatDeleteRequest;
import com.younglin.partnerMatching.service.ChatUserLinkService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
* @author chenyanglin
* @description 针对表【chat_user_link(聊天用户关系表)】的数据库操作Service实现
* @createDate 2024-05-05 19:22:13
*/
@Service
public class ChatUserLinkServiceImpl extends ServiceImpl<ChatUserLinkMapper, ChatUserLink>
    implements ChatUserLinkService {

    @Resource
    private ChatUserLinkMapper chatUserLinkMapper;

    /**
     * 删除用户聊天关系
     * @param chatDeleteRequest
     * @return
     */
    @Override
    public boolean deleteConnection(ChatDeleteRequest chatDeleteRequest) {
        if (chatDeleteRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        Long connectionId = chatDeleteRequest.getId();

        if (connectionId == null || connectionId < 0){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        boolean idDelete = this.removeById(connectionId);

        if (!idDelete){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除失败！");
        }

        return idDelete;
    }
}




