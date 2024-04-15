package com.younglin.partnerMatching.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.younglin.partnerMatching.service.UserTeamService;
import com.younglin.partnerMatching.model.domain.UserTeam;
import com.younglin.partnerMatching.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author chenyanglin
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2024-04-15 13:10:05
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService {

}




