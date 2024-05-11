package com.younglin.partnerMatching.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.younglin.partnerMatching.common.BaseResponse;
import com.younglin.partnerMatching.common.ErrorCode;
import com.younglin.partnerMatching.common.ResultUtils;
import com.younglin.partnerMatching.exception.BusinessException;
import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.model.request.UserRequest.UserLoginRequest;
import com.younglin.partnerMatching.model.request.UserRequest.UserRegisterRequest;
import com.younglin.partnerMatching.model.request.UserRequest.UserUpdateRequest;
import com.younglin.partnerMatching.model.vo.UserVo;
import com.younglin.partnerMatching.service.ChatUserLinkService;
import com.younglin.partnerMatching.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户接口
 *
 * @author
 * @from
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = {"http://localhost:5173"})
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private ChatUserLinkService chatUserLinkService;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 校验
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 查询所有用户
     *
     * @param username
     * @param request
     * @return
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "缺少管理员权限");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    /**
     * 删除用户
     *
     * @param id
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 根据标签搜索用户
     *
     * @param tagNameList
     * @return
     */
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.searchUserByTags(tagNameList);

        return ResultUtils.success(userList);

    }

    /**
     * 更新用户信息
     *
     * @param editUser
     * @param request
     * @return
     */
    @PostMapping("/updateUser")
    public BaseResponse<Integer> updateUser(@RequestBody UserUpdateRequest editUser, HttpServletRequest request) {
        if (editUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//        //获取当前用户
        User currentUser = userService.getCurrentUser(request);

        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "用户未登录");
        }

        Integer result = userService.updateUser(editUser, currentUser);

        return ResultUtils.success(result);
    }

    /**
     * 获取当前用户
     *
     * @param request
     * @return
     */
    @GetMapping("/currentUser")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {

        User currentUser = userService.getCurrentUser(request);

        return ResultUtils.success(currentUser);
    }

    @GetMapping("/recommendUser")
    public BaseResponse<Page<List<User>>> getUserList(int pageSize, int pageNumber, HttpServletRequest request) {
        User currentLoginUser = userService.getCurrentUser(request);

        //设置rediskey
        String userRedisKey = String.format("redisKey:user:recommend:%s:pageNumber:%d", currentLoginUser.getId(), pageNumber);
        ValueOperations valueOperations = redisTemplate.opsForValue();

        //从缓存中读取数据
        Page<List<User>> userPage = (Page<List<User>>) valueOperations.get(userRedisKey);

        //如果有缓存，则直接从缓存中取出数据
        if (userPage != null) {
            return ResultUtils.success(userPage);
        }

        //无缓存，查询数据库，并将数据存入缓存
        QueryWrapper queryWrapper = new QueryWrapper();
        userPage = userService.page(new Page<>(pageNumber, pageSize), queryWrapper);

        //写缓存，30s过期
        try {
            valueOperations.set(userRedisKey, userPage, 30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }

        return ResultUtils.success(userPage);
    }

    /**
     * 距离算法 匹配用户
     *
     * @param num
     * @param request
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) {
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //获得当前的登录用户
        User currentUser = userService.getCurrentUser(request);

        String matchUserRedisKey = String.format("redisKey:user:matchUsers:%d", currentUser.getId());
        ValueOperations valueOperations = redisTemplate.opsForValue();
        List<User> matchUsers = (List<User>) valueOperations.get(matchUserRedisKey);

        if (matchUsers != null) {
            return ResultUtils.success(matchUsers);
        }

        //如果没有缓存中没有数据 查询匹配用户
        matchUsers = userService.matchUsers(num, currentUser);
        //写缓存，30s过期
        try {
            valueOperations.set(matchUserRedisKey, matchUsers, 30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }

        return ResultUtils.success(matchUsers);

    }

    /**
     * 查询伙伴
     *
     * @param request
     * @return
     */
    @GetMapping("/friends")
    public BaseResponse<List<UserVo>> searchFriends(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);

        Long userId = currentUser.getId();
        //todo 设计伙伴缓存
        List<UserVo> friends = userService.searchFriends(userId);

        return ResultUtils.success(friends);
    }
}
