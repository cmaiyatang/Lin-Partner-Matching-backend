package com.younglin.partnerMatching.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.younglin.partnerMatching.model.domain.ChatMessage;
import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.model.vo.ChatMessageVO;
import com.younglin.partnerMatching.model.vo.WebsocketVO;

import java.util.List;

/**
* @author chenyanglin
* @description 针对表【chat_message(聊天内容详情表)】的数据库操作Service
* @createDate 2024-05-05 19:22:02
*/
public interface ChatMessageService extends IService<ChatMessage> {

    /**
     * 封装聊天信息
     * @param userId
     * @param friendId
     * @param message
     * @return
     */
    ChatMessageVO getMessageResult(Long userId,Long friendId,String message);

    /**
     * 查询聊天记录
     * @param userId
     * @param friendId
     * @param currentUser
     * @return
     */
    List<ChatMessageVO> getChatRecord(Long userId, Long friendId, User currentUser);
}
