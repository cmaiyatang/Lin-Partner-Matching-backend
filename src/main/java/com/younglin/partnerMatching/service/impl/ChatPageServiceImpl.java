package com.younglin.partnerMatching.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.younglin.partnerMatching.service.ChatPageService;
import com.younglin.partnerMatching.model.domain.ChatPage;
import com.younglin.partnerMatching.mapper.ChatPageMapper;
import org.springframework.stereotype.Service;

/**
* @author chenyanglin
* @description 针对表【chat_page(聊天界面表)】的数据库操作Service实现
* @createDate 2024-05-05 19:22:07
*/
@Service
public class ChatPageServiceImpl extends ServiceImpl<ChatPageMapper, ChatPage>
    implements ChatPageService {

}




