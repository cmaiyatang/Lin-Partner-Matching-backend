package com.younglin.partnerMatching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.younglin.partnerMatching.model.domain.Team;
import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.model.dto.SearchTeamDto;
import com.younglin.partnerMatching.model.request.DeleteRequest;
import com.younglin.partnerMatching.model.request.TeamRequest.TeamJoinRequest;
import com.younglin.partnerMatching.model.request.TeamRequest.TeamUpdateRequest;
import com.younglin.partnerMatching.model.vo.TeamUserVo;

import java.util.List;

/**
* @author chenyanglin
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-04-15 13:09:59
*/
public interface TeamService extends IService<Team> {

    /**
     * 新建队伍
     * @param team
     * @param loginUser
     * @return
     */
    public Long addTeam(Team team, User loginUser);

    /**
     * 查询队伍
     * @param searchTeamDto
     * @return
     */
    IPage<TeamUserVo> searchTeams(SearchTeamDto searchTeamDto, Boolean isAdmin);

    /**
     * 修改队伍信息
     *
     * @param teamUpdateRequest
     * @param currentUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User currentUser);

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @param currentUser
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User currentUser);

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param currentUser
     */
    boolean quitTeam(DeleteRequest teamQuitRequest, User currentUser);

    /**
     * 删除队伍/解散队伍
     * @param id
     * @param currentUser
     * @return
     */
    boolean deleteTeam(long id, User currentUser);

    /**
     * 获取 我的小队
     * @param currentUser
     * @return
     */
    List<Team> getMyTeamList(User currentUser);

    /**
     * 获取我加入的小队
     * @param currentUser
     * @return
     */
    List<Team> getMyJoinTeamList(User currentUser);

    /**
     * 获取队伍加入的人数
     * @param myJoinTeamList
     * @return
     */
    List<TeamUserVo> setHasJoinNum(List<TeamUserVo> myJoinTeamList);
}

