package com.younglin.partnerMatching.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.younglin.partnerMatching.model.domain.ChatMessage;
import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.model.vo.ChatMessageVO;
import com.younglin.partnerMatching.model.vo.ChatTeamMessageVO;
import com.younglin.partnerMatching.model.vo.WebsocketVO;
import io.swagger.models.auth.In;

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
     * @param chatType
     * @return
     */
    ChatMessageVO getPrivateChatResult(Long userId, Long friendId, String message, Integer chatType);

    /**
     * 封装队伍聊天信息
     * @param sendUserId
     * @param friendId
     * @param message
     * @param chatType
     * @return
     */
    ChatTeamMessageVO getTeamChatResult(Long sendUserId, Long friendId, String message,Integer chatType);

    /**
     * 查询聊天记录
     * @param userId
     * @param friendId
     * @param currentUser
     * @return
     */
    List<ChatMessageVO> getChatRecord(Long userId, Long friendId, User currentUser);

    /**
     * 查询队伍聊天记录
     * @param userId
     * @param friendId
     * @param currentUser
     * @return
     */
    List<ChatTeamMessageVO> getTeamChatMessage(Long userId, Long friendId, User currentUser);
}
