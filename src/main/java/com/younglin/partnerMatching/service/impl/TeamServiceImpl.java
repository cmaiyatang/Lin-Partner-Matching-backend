package com.younglin.partnerMatching.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.younglin.partnerMatching.service.TeamService;
import com.younglin.partnerMatching.model.domain.Team;
import com.younglin.partnerMatching.mapper.TeamMapper;
import org.springframework.stereotype.Service;

/**
* @author chenyanglin
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2024-04-15 13:09:59
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService {

}




