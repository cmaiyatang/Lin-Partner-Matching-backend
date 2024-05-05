package com.younglin.partnerMatching.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.younglin.partnerMatching.common.BaseResponse;
import com.younglin.partnerMatching.common.ErrorCode;
import com.younglin.partnerMatching.common.ResultUtils;
import com.younglin.partnerMatching.exception.BusinessException;
import com.younglin.partnerMatching.mapper.UserTeamMapper;
import com.younglin.partnerMatching.model.domain.Team;
import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.model.domain.UserTeam;
import com.younglin.partnerMatching.model.dto.SearchTeamDto;
import com.younglin.partnerMatching.model.request.*;
import com.younglin.partnerMatching.model.vo.TeamUserVo;
import com.younglin.partnerMatching.service.TeamService;
import com.younglin.partnerMatching.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户接口
 *
 * @author
 * @from
 */
@RestController
@RequestMapping("/team")
@CrossOrigin(origins = {"http://localhost:5173"})
@Slf4j
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamMapper userTeamMapper;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 新增队伍
     *
     * @param teamAddRequest
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        //获取当前登录用户
        User currentUser = userService.getCurrentUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);

        Long teamId = teamService.addTeam(team, currentUser);

        return ResultUtils.success(teamId);
    }

    /**
     * 更新队伍数据
     *
     * @param teamUpdateRequest
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);

        boolean isUpdate = teamService.updateTeam(teamUpdateRequest, currentUser);

        if (!isUpdate) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 根据id查找单个队伍
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(long id) {
        if (id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求id为空");
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查找队伍失败");
        }

        return ResultUtils.success(team);
    }

    /**
     * 分页查询teams
     *
     * @param searchTeamDto
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<IPage<TeamUserVo>> getTeamListPage(SearchTeamDto searchTeamDto, HttpServletRequest request) {

        if (searchTeamDto == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean isAdmin = userService.isAdmin(request);
        User currentUser = userService.getCurrentUser(request);
        IPage<TeamUserVo> teamList = teamService.searchTeams(searchTeamDto, isAdmin);

        List<TeamUserVo> teamRecords = teamList.getRecords();

        List<TeamUserVo> teamUserVoList = setHasJoinAndJoinNum(teamRecords, currentUser);

        return ResultUtils.success(teamList.setRecords(teamUserVoList));
    }

//    /**
//     * 查询队伍list
//     *
//     * @param searchTeamDto
//     * @return
//     */
//    @GetMapping("/list")
//    public BaseResponse<Page<Team>> getTeamList(SearchTeamDto searchTeamDto,HttpServletRequest request) {
//        if (searchTeamDto == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//
//        if (searchTeamDto == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        boolean isAdmin = userService.isAdmin(request);
//        User currentUser = userService.getCurrentUser(request);
//
//        return ResultUtils.success(teamPage);
//    }

    /**
     * 加入队伍
     *
     * @param teamJoinRequest
     * @param request
     * @return
     */
    @Transactional//事务注解
    @PostMapping("join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //得到当前登录用户
        User currentUser = userService.getCurrentUser(request);

        boolean isJoin = teamService.joinTeam(teamJoinRequest, currentUser);

        if (!isJoin) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入队伍失败");
        }

        return ResultUtils.success(true);
    }

    /**
     * 用户退出队伍
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);
        boolean isQuit = teamService.quitTeam(deleteRequest, currentUser);

        return ResultUtils.success(isQuit);
    }

    /**
     * 删除队伍/解散队伍
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        Long teamId = deleteRequest.getId();
        if (deleteRequest == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //获取当前登录用户
        User currentUser = userService.getCurrentUser(request);

        boolean isDelete = teamService.deleteTeam(teamId, currentUser);

        return ResultUtils.success(isDelete);
    }

    /**
     * 我的队伍
     *
     * @param request
     * @return
     */
    @GetMapping("/myTeam")
    public BaseResponse<List<TeamUserVo>> getMyTeam(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        //获取当前登录用户
        User currentUser = userService.getCurrentUser(request);
        SearchTeamDto searchTeamDto = new SearchTeamDto();
        searchTeamDto.setUserId(currentUser.getId());
        searchTeamDto.setPageNumber(1);
        searchTeamDto.setPageSize(8);

//        List<TeamUserVo> orimyTeamList = teamService.searchTeams(searchTeamDto, true);
        IPage<TeamUserVo> teamUserVoIPage = teamService.searchTeams(searchTeamDto, true);
        List<TeamUserVo> orimyTeamList = teamUserVoIPage.getRecords();

        List<TeamUserVo> myTeamList = setHasJoinAndJoinNum(orimyTeamList, currentUser);

        return ResultUtils.success(myTeamList);
    }

    /**
     * 我加入的队伍
     *
     * @param request
     * @return
     */
    @GetMapping("/myJoinTeam")
    public BaseResponse<List<TeamUserVo>> getMyJoinTeam(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        //获取当前登录用户
        User currentUser = userService.getCurrentUser(request);

        //获取用户加入队伍的id
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", currentUser.getId());
        List<UserTeam> userTeams = userTeamMapper.selectList(queryWrapper);

        //遍历数据得到队伍idList 确保没有重复的数据 根据队伍id分组
        Map<Long, List<UserTeam>> collect =
                userTeams.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));

        ArrayList<Long> idList = new ArrayList<>(collect.keySet());
        //判断用户有没有加入队伍
        if (CollectionUtils.isEmpty(idList)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "还未加入队伍哦");
        }

        SearchTeamDto searchTeamDto = new SearchTeamDto();
        searchTeamDto.setIdList(idList);
        searchTeamDto.setPageNumber(1);
        searchTeamDto.setPageSize(8);

        //复用查询队伍列表的方法
//        List<TeamUserVo> curmyJoinTeamList = teamService.searchTeams(searchTeamDto, true);
        IPage<TeamUserVo> teamUserVoIPage = teamService.searchTeams(searchTeamDto, true);
        List<TeamUserVo> curmyJoinTeamList = teamUserVoIPage.getRecords();


        List<TeamUserVo> myJoinTeamList = setHasJoinAndJoinNum(curmyJoinTeamList, currentUser);

        return ResultUtils.success(myJoinTeamList);

    }


    /**
     * 查询每个队伍加入的人数
     *
     * @param teamList
     * @return
     */
    @GetMapping("/userNumHasJoinTeam")
    public Map<Long, Integer> setHasJoinNum(List<TeamUserVo> teamList) {
        if (teamList == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        List<TeamUserVo> teamUserVoList = teamService.setHasJoinNum(teamList);

        Map<Long, Integer> teamHasJoinNum = new HashMap<>();
        for (TeamUserVo team : teamUserVoList) {
            Integer hasJoinNum = team.getHasJoinNum();
            Long teamId = team.getId();
            teamHasJoinNum.put(teamId, hasJoinNum);
        }

        return teamHasJoinNum;

    }

    //设置队伍TeamUserVo的加入人数hasJoinNum和当前登录用户是否已加入hasJoin
    public List<TeamUserVo> setHasJoinAndJoinNum(List<TeamUserVo> teamUserVoList, User currentUser) {

        //判断登录用户是否已加入队伍，hasJoin 方便前端队伍按钮展示
        List<Long> teamIdList = teamUserVoList.stream().map(TeamUserVo::getId).collect(Collectors.toList());

        //查询当前用户已加入的队伍id
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", currentUser.getId());
        userTeamQueryWrapper.in("teamId", teamIdList);

        try {
            List<UserTeam> userHasJoinTeamList = userTeamMapper.selectList(userTeamQueryWrapper);

            //用户已加入的队伍id集合
            List<Long> userHasJoinTeamIdList = userHasJoinTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toList());

            //遍历所有队伍，判断用户已加入的队伍id是否包含队伍的id
            teamUserVoList.forEach(team -> {
                boolean contains = userHasJoinTeamIdList.contains(team.getId());
                team.setHasJoin(contains);
            });
        } catch (Exception e) {
        }

        //设置每个队伍已加入的人数
        List<TeamUserVo> teamList = teamService.setHasJoinNum(teamUserVoList);

        return teamList;
    }

}




