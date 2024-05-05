package com.younglin.partnerMatching.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.younglin.partnerMatching.service.ChatMessageService;
import com.younglin.partnerMatching.model.domain.ChatMessage;
import com.younglin.partnerMatching.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;

/**
* @author chenyanglin
* @description 针对表【chat_message(聊天内容详情表)】的数据库操作Service实现
* @createDate 2024-05-05 19:22:02
*/
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
    implements ChatMessageService {

}




