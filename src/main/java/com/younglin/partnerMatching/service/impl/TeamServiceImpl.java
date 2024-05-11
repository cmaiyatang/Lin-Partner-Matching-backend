package com.younglin.partnerMatching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.younglin.partnerMatching.common.ErrorCode;
import com.younglin.partnerMatching.contant.TeamStatusEnum;
import com.younglin.partnerMatching.contant.UserConstant;
import com.younglin.partnerMatching.exception.BusinessException;
import com.younglin.partnerMatching.mapper.TeamMapper;
import com.younglin.partnerMatching.mapper.UserMapper;
import com.younglin.partnerMatching.mapper.UserTeamMapper;
import com.younglin.partnerMatching.model.domain.Team;
import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.model.domain.UserTeam;
import com.younglin.partnerMatching.model.dto.SearchTeamDto;
import com.younglin.partnerMatching.model.request.DeleteRequest;
import com.younglin.partnerMatching.model.request.TeamRequest.TeamJoinRequest;
import com.younglin.partnerMatching.model.request.TeamRequest.TeamUpdateRequest;
import com.younglin.partnerMatching.model.vo.TeamUserVo;
import com.younglin.partnerMatching.model.vo.UserVo;
import com.younglin.partnerMatching.service.TeamService;
import com.younglin.partnerMatching.service.UserService;
import com.younglin.partnerMatching.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author chenyanglin
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2024-04-15 13:09:59
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Resource
    private TeamMapper teamMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserTeamMapper userTeamMapper;

    @Resource
    private RedissonClient redissonClient;

    //判断队伍是否存在
    public Team getExistTeam(long id) {
        if (id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamMapper.selectById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }

        return team;
    }

    //判断队伍是否过期
    public void judgeTeamOutOfTime(Team team) {

        Date expireTime = team.getExpireTime();
        if (expireTime == null) {
            return;
        }

        if (expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍已过期~");
        }
    }

    /**
     * 新建队伍
     *
     * @param team
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTeam(Team team, User loginUser) {

        //1.请求参数是否为空
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍信息为空");
        }

        //2.是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        //3.校验信息
        //队伍人数 >1 且 <= 20
        Integer maxNum = team.getMaxNum();
        if (maxNum < 1 || maxNum > 20 || maxNum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不符和要求");
        }

        //队伍名称 <=20
        String teamName = team.getTeamName();
        if (StringUtils.isBlank(teamName) || teamName.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍名称符合要求");
        }

        //描述 <=512
        String description = team.getDescription();
        if (StringUtils.isBlank(description) || description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述不符合要求");
        }

        //status是否公开（int）不传默认为0 （公开）
        Integer status = team.getStatus();
        if (status == null) {
            status = TeamStatusEnum.getTeamStatusEnum(0).getValue();
        }

        //如果status是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if (status == TeamStatusEnum.getTeamStatusEnum(2).getValue()) {
            if (StringUtils.isBlank(password)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入密码");
            }
            if (password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不符合要求");
            }
        }

        //超时时间 大于 当前时间
        Date expireTime = team.getExpireTime();
        if (expireTime != null) {
            if (!expireTime.after(new Date())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间 < 当前时间");
            }
        }
        //检验用户最多创建5个队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
//        long count = teamService.count(queryWrapper);
        Long count = teamMapper.selectCount(queryWrapper);
        if (count >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "单个用户最多创建5个队伍");
        }

        //4.插入队伍信息到队伍表
        team.setUserId(loginUser.getId());
//        boolean isAddTeam = teamService.save(team);
        int insert = teamMapper.insert(team);
        if (insert < 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        }

        //5.插入数据到 用户-队伍表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(loginUser.getId());
        userTeam.setTeamId(team.getId());
        userTeam.setJoinTime(new Date());
        boolean isAddUser_Team = userTeamService.save(userTeam);
        if (!isAddUser_Team) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        }

        return team.getId();
    }

    /**
     * 查询队伍
     *
     * @param searchTeamDto
     * @param isAdmin
     * @return
     */
    @Override
    public IPage<TeamUserVo> searchTeams(SearchTeamDto searchTeamDto, Boolean isAdmin) {

        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // 组合查询条件
        if (searchTeamDto != null) {

            Long id = searchTeamDto.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }

            List<Long> idList = searchTeamDto.getIdList();
            if (CollectionUtils.isNotEmpty(idList)) {
                queryWrapper.in("id", idList);
            }

            //根据关键字查询
            String searchText = searchTeamDto.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("teamName", searchText).or().like("description", searchText));
            }

            //根据队伍名称查询
            String name = searchTeamDto.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("teamName", name);
            }

            //根据队伍描述查询
            String description = searchTeamDto.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }

            // 查询最大人数相等的
            Integer maxNum = searchTeamDto.getMaxNum();
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }

            // 根据创建人来查询
            Long userId = searchTeamDto.getUserId();
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }


            // 显示所有状态的队伍
            Integer status = searchTeamDto.getStatus();
            if (status != null) { // 如果状态筛选条件不为空，则根据条件筛选
                TeamStatusEnum teamStatusEnum = TeamStatusEnum.getTeamStatusEnum(status);
                if (teamStatusEnum != null) {
                    queryWrapper.eq("status", teamStatusEnum.getValue());
                }
            }

            //不展示已过期的队伍
        }

//        List<Team> teamList = this.list(queryWrapper);
//
//        if (!isAdmin) {
//            teamList = teamList.stream().filter(team ->
//                            TeamStatusEnum.PRIVATE.getValue() != team.getStatus())
//                    .collect(Collectors.toList());
//        }
//
//        List<TeamUserVo> teamUserVoList = new ArrayList<>();
//
//        //关联查询创建人信息
//        for (Team team : teamList) {
//
//            //拿到SearchTeamDto里面的userId
//            Long creater = team.getUserId();
//            //根据userId查询用户表
//            QueryWrapper userWrapper = new QueryWrapper();
//            userWrapper.eq("id", creater);
//
//            //创建UserVo对象
//            User user = userMapper.selectOne(userWrapper);
//            UserVo userVo = new UserVo();
//            BeanUtils.copyProperties(user, userVo);
//
//            //将查询到的数据封装到TeamUserVo中
//            TeamUserVo teamUserVo = new TeamUserVo();
//            BeanUtils.copyProperties(team, teamUserVo);
//            teamUserVo.setCreateUser(userVo);
//
//            teamUserVoList.add(teamUserVo);
//        }

        int pageNum = searchTeamDto.getPageNumber();
        int pageSize = searchTeamDto.getPageSize();

        // 分页查询
        Page<Team> page = new Page<>(pageNum, pageSize);
        IPage<Team> teamPage = this.page(page, queryWrapper);

        // 将查询到的 Team 转换为 TeamUserVo，并关联创建人信息
        List<TeamUserVo> teamUserVoList = teamPage.getRecords().stream().map(team -> {
            Long creater = team.getUserId();
            QueryWrapper<User> userWrapper = new QueryWrapper<>();
            userWrapper.eq("id", creater);
            User user = userMapper.selectOne(userWrapper);
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(user, userVo);
            TeamUserVo teamUserVo = new TeamUserVo();
            BeanUtils.copyProperties(team, teamUserVo);
            teamUserVo.setCreateUser(userVo);
            return teamUserVo;
        }).collect(Collectors.toList());

        // 将查询结果封装成 IPage<TeamUserVo> 对象并返回
        IPage<TeamUserVo> resultPage = new Page<>();
        BeanUtils.copyProperties(teamPage, resultPage);
        resultPage.setRecords(teamUserVoList);

        return resultPage;

    }

    /**
     * 更新队伍信息
     *
     * @param teamUpdateRequest
     * @param currentUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User currentUser) {
        //判断请求参数是否为空
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean hasChanges = false;

        //只有管理员或创建者才能修改队伍信息
        Integer userRole = currentUser.getUserRole();
        if (userRole != UserConstant.ADMIN_ROLE) {
            if (!Objects.equals(currentUser.getId(), teamUpdateRequest.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
        }

        Long teamId = teamUpdateRequest.getId();
        //队伍是否存在
        Team currentTeam = getExistTeam(teamId);

        //队伍是否过期
        judgeTeamOutOfTime(currentTeam);

        String curTeamName = currentTeam.getTeamName();
        String curTeamDescription = currentTeam.getDescription();
        Integer curTeamStatus = currentTeam.getStatus();
        Date curTeamExpireTime = currentTeam.getExpireTime();
        Integer curTeamMaxNum = currentTeam.getMaxNum();
        String curTeamPassword = currentTeam.getPassword();

        //设置队伍名称
        String teamName = teamUpdateRequest.getTeamName();
        if (!teamName.equals(curTeamName) && StringUtils.isNotBlank(teamName)) {
            //如果修改数据中队伍名称不等于原本的名称，并且不为空,null,空格
            currentTeam.setTeamName(teamName);
            hasChanges = true;
        }

        //设置队伍描述
        String description = teamUpdateRequest.getDescription();
        if (!description.equals(curTeamDescription) && StringUtils.isNotBlank(description)) {
            currentTeam.setDescription(description);
            hasChanges = true;
        }

        //设置队伍状态
        Integer status = teamUpdateRequest.getStatus();
        String password = teamUpdateRequest.getPassword();
        if (status != null && !Objects.equals(status, curTeamStatus)) {
            //如果要将状态修改为加密
            if (status == TeamStatusEnum.SECRET.getValue()) {
                if (password == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密队伍必须设置密码");
                }
                if (!password.equals(curTeamPassword)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
                }
                currentTeam.setStatus(status);
            }
            currentTeam.setStatus(status);
            hasChanges = true;
        }

        //修改密码 用户输入的密码是否正确的校验交给前端
        if (password != null && status == TeamStatusEnum.SECRET.getValue()) {
            currentTeam.setPassword(password);
            hasChanges = true;
        }

        //设置最大人数
        Integer maxNum = teamUpdateRequest.getMaxNum();
        //当最大队伍人数改变时
        if (maxNum != null && !maxNum.equals(curTeamMaxNum)) {
            // 查询当前队伍的人数
            //队伍最大人数不能小于当前队伍的人数
            QueryWrapper<UserTeam> queryWrapper1 = new QueryWrapper();
            queryWrapper1.eq("teamId", teamId);
            Long curUserNumber = userTeamMapper.selectCount(new QueryWrapper<UserTeam>().eq("teamId", teamId));

            if (maxNum < curUserNumber) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "最大人数不能小于当前队伍的人数");
            }

            // 更新最大人数
            currentTeam.setMaxNum(maxNum);
            hasChanges = true;
        }


        //设置过期时间
        Date expireTime = teamUpdateRequest.getExpireTime();
        //过期时间要大于当前时间
        if (expireTime != null && expireTime.after(new Date())) {
            //如果当前队伍过期时间不存在  或者当前过期时间大于当前过期时间
            if (curTeamExpireTime == null || expireTime.after(curTeamExpireTime)) {
                currentTeam.setExpireTime(expireTime);
                hasChanges = true;
            }
        }

        // 如果没有发生任何改变，则直接返回true
        if (hasChanges == false) {
            return true;
        }

        return this.updateById(currentTeam);
    }

    /**
     * 加入队伍
     *
     * @param teamJoinRequest
     * @param currentUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User currentUser) {
        //判断请求参数是否为空
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = currentUser.getId();

        //队伍是否存在
        Long teamId = teamJoinRequest.getTeamId();
        Team team = getExistTeam(teamId);

        //队伍是否过期
        judgeTeamOutOfTime(team);

        RLock lock = redissonClient.getLock("younglin:join_team");
        try {
            while (true) {
                // 抢到锁并执行
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    System.out.println("获取到锁，开始执行业务");
                    // 在这里执行你的业务逻辑
                    // 例如：处理加入队伍的业务
                    //用户不能加入自己的队伍
                    if (Objects.equals(team.getUserId(), currentUser.getId())) {
                        //登录用户的id和队伍创建者id相等
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能加入自己的队伍哟~");
                    }

                    //用户不能重复加入某个队伍
                    QueryWrapper<UserTeam> hasJoinWrapper = new QueryWrapper<>();
                    hasJoinWrapper.eq("userId", userId).eq("teamId", teamId);
                    UserTeam hasJoinUser = userTeamMapper.selectOne(hasJoinWrapper);
                    if (hasJoinUser != null) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "已在队伍中呦~");
                    }

                    //队伍人数是否已满
                    //关联查询user_team表
                    //select count(ut.userId) from team t left join user_team ut on t.id = ut.teamId
                    QueryWrapper<UserTeam> queryWrapper = new QueryWrapper();
                    queryWrapper.eq("teamId", teamId).isNotNull("userId");
                    Long userCount = userTeamMapper.selectCount(queryWrapper);
                    if (userCount >= 5) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍人数已满呢~");
                    }

                    //一个用户最多加入五个队伍
                    QueryWrapper<UserTeam> userTeamqueryWrapper = new QueryWrapper<>();
                    userTeamqueryWrapper.eq("userId", userId).isNotNull("teamId");
                    Long hasJoinNumber = userTeamMapper.selectCount(userTeamqueryWrapper);
                    if (hasJoinNumber >= 5) {
                        throw new BusinessException(ErrorCode.NULL_ERROR, "最多加入五个队伍~");
                    }

                    //加入加密队伍时判断密码是否正确
                    Integer status = team.getStatus();
                    if (status == TeamStatusEnum.SECRET.getValue()) {
                        String password = teamJoinRequest.getPassword();
                        if (password == null || !password.equals(team.getPassword())) {
                            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误~");
                        }
                    }

                    // 查询是否有已逻辑删除的记录(当用户退出后又重新加入队伍）
//        QueryWrapper<UserTeam> isDeletequeryWrapper = new QueryWrapper<>();
//        queryWrapper.apply("isDelete = 1",userId,teamId);
//        UserTeam deletedRecord = userTeamMapper.selectHasQuitTeam(queryWrapper);
//        if (deletedRecord != null) {
//            // 如果存在已逻辑删除的记录，更新isDelete字段为0，并更新joinTime字段
//            deletedRecord.setIsDelete(0); // 设置为未删除
//            deletedRecord.setJoinTime(new Date()); // 更新加入时间
//            userTeamMapper.updateById(deletedRecord);
//        } else {
//            // 如果不存在已逻辑删除的记录，插入新的记录
//            UserTeam userTeam = new UserTeam();
//            userTeam.setUserId(userId);
//            userTeam.setTeamId(teamId);
//            userTeam.setJoinTime(new Date());
//            userTeamMapper.insert(userTeam);
//        }

                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (Exception e) {
            log.error("业务执行出错", e);
            return false;
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("释放锁");
                lock.unlock();
            }
        }


    }

    /**
     * 退出队伍
     *
     * @param teamQuitRequest
     * @param currentUser
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(DeleteRequest teamQuitRequest, User currentUser) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //登录用户的id
        Long userId = currentUser.getId();

        Long teamId = teamQuitRequest.getId();
        Team team = getExistTeam(teamId);

        //队伍是否过期
        judgeTeamOutOfTime(team);

        //在队伍中才能退出
        QueryWrapper<UserTeam> hasJoinWrapper = new QueryWrapper<>();
        hasJoinWrapper.eq("userId", userId).eq("teamId", teamId);
        UserTeam hasJoinUser = userTeamMapper.selectOne(hasJoinWrapper);
        if (hasJoinUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您不在队伍中哦");
        }

        // 查询队伍人数
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId).isNotNull("userId");
        Long hasJoinUserNumber = userTeamMapper.selectCount(userTeamQueryWrapper);

        if (hasJoinUserNumber == 1) {
            //解散队伍
            teamMapper.deleteById(teamId);
        } else {
            // 队伍成员退出队伍或解散队伍
            if (Objects.equals(userId, team.getUserId())) {
                //队长退出后，将第二早加入队伍的成员设置为队长（队伍的创建者）
                //1.查询user_team表，用户的加入时间 joinTime
                // 查询加入队伍时间最早且不是队长的成员
                QueryWrapper<UserTeam> earliestMemberWrapper = new QueryWrapper<>();
                earliestMemberWrapper.eq("teamId", teamId).ne("userId", userId).orderByAsc("joinTime").last("LIMIT 1");
                UserTeam earliestMember = userTeamMapper.selectOne(earliestMemberWrapper);

                if (earliestMember != null) {
                    // 更新队伍的队长
                    team.setUserId(earliestMember.getUserId());
                    int updateCount = teamMapper.updateById(team);
                    if (updateCount == 0) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "设置新队长失败呢");
                    }
                }
            }

        }
        QueryWrapper<UserTeam> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("userId", userId).eq("teamId", teamId);


        return userTeamService.remove(deleteWrapper);
    }

    /**
     * 解散队伍/删除队伍
     *
     * @param id
     * @param currentUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(long id, User currentUser) {
        //校验请求参数
        if (id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //检查队伍是否存在，并且获得当前队伍
        Team team = getExistTeam(id);
        Long teamId = team.getId();

        //判断当前登录用户是否为队伍的队长（创建人）
        if (!Objects.equals(team.getUserId(), currentUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "您不是队伍的队长哟~");
        }

        //移除user_team表中的关联信息
        QueryWrapper<UserTeam> userTeamqueryWrapper = new QueryWrapper<>();
        userTeamqueryWrapper.eq("teamId", teamId);
        int deleteUserTeam = userTeamMapper.delete(userTeamqueryWrapper);

        if (deleteUserTeam <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        //删除队伍数据 team表
        int deleteTeam = teamMapper.deleteById(teamId);
        if (deleteTeam <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        return true;
    }

    /**
     * 获取我的小队
     *
     * @param currentUser
     * @return
     */
    @Override
    public List<Team> getMyTeamList(User currentUser) {

        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        Long userId = currentUser.getId();
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        List<Team> teamList = teamMapper.selectList(queryWrapper);
        if (teamList == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "您还没有小队呦~");
        }

        return teamList;
    }

    /**
     * 获取我加入的小队
     *
     * @param currentUser
     * @return
     */
    @Override
    public List<Team> getMyJoinTeamList(User currentUser) {
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        Long userId = currentUser.getId();

        //select * from team t left join user_team ut on t.id = ut.teamId where ut.userId = userId
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ut.userId", userId);
        queryWrapper.apply("LEFT JOIN user_team ut ON team.id = ut.teamId");
        List<Team> joinTeamList = teamMapper.selectList(queryWrapper);

        return joinTeamList;
    }

    /**
     * 查询每个队伍加入的人数
     *
     * @param teamList
     * @return
     */
    @Override
    public List<TeamUserVo> setHasJoinNum(List<TeamUserVo> teamList) {

        //获取队伍id
        List<Long> teamIdList = teamList.stream().map(TeamUserVo::getId).collect(Collectors.toList());

        //根据id查询每个队伍已加入的人数
        QueryWrapper<UserTeam> userInTeamNumberWrapper = new QueryWrapper<>();
        userInTeamNumberWrapper.in("teamId", teamIdList);
        List<UserTeam> userTeams = userTeamMapper.selectList(userInTeamNumberWrapper);

        //根据队伍id分组，key -> teamId , value -> 队伍人数
        Map<Long, List<UserTeam>> collect = userTeams.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamList.forEach(team -> team.setHasJoinNum(collect.getOrDefault(team.getId(), new ArrayList<>()).size()));

        return teamList;

    }


}
















