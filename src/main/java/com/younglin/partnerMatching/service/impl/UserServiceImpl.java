package com.younglin.partnerMatching.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.younglin.partnerMatching.common.ErrorCode;
import com.younglin.partnerMatching.exception.BusinessException;
import com.younglin.partnerMatching.manager.SessionManager;
import com.younglin.partnerMatching.mapper.UserMapper;
import com.younglin.partnerMatching.model.domain.ChatUserLink;
import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.model.request.UserRequest.UserUpdateRequest;
import com.younglin.partnerMatching.model.vo.UserVo;
import com.younglin.partnerMatching.service.ChatUserLinkService;
import com.younglin.partnerMatching.service.UserService;
import com.younglin.partnerMatching.utils.AlgorithmUtils;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.younglin.partnerMatching.contant.RedisKeyConstant.USER_LOGIN_STATE;
import static com.younglin.partnerMatching.contant.UserConstant.ADMIN_ROLE;

/**
 * 用户服务实现类
 *
 * @author
 * @from
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private ChatUserLinkService chatUserLinkService;

    @Resource
    private SessionManager sessionManager;


    // https://www.code-nav.cn/

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "yupi";

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return -1;
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            return -1;
        }
        // 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
//        // 星球编号不能重复
//        queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("planetCode", planetCode);
//        count = userMapper.selectCount(queryWrapper);
//        if (count > 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编号重复");
//        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            return -1;
        }
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        if (userAccount.length() < 4) {
            return null;
        }
        if (userPassword.length() < 8) {
            return null;
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return null;
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "账号或密码错误");
        }
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        // 4. 记录用户的登录态


        //实现单设备多端登录，禁止多设备登录
        try {
            sessionManager.login(safetyUser, request);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        return safetyUser;
    }

    /**
     * 用户信息脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setStatus(originUser.getStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setProfile(originUser.getProfile());
        return safetyUser;
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签查询用户
     * sql查询
     *
     * @param tagNameList
     * @return
     */
    @Override
    public List<User> searchUserByTags(List<String> tagNameList) {
        //数据校验
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return sqlSearch(tagNameList);
    }

    /**
     * 数据库查询
     *
     * @param tagNameList
     * @return
     */
    public List<User> sqlSearch(List<String> tagNameList) {
        //设置开始时间
        long starTime = System.currentTimeMillis();
        //封装查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //拼接tag
        for (String tagList : tagNameList) {
            queryWrapper = queryWrapper.like("tags", tagList);
        }

        //执行查询
        List<User> userList = userMapper.selectList(queryWrapper);
        log.info("sql query time = " + (System.currentTimeMillis() - starTime));

        //获得脱敏后的用户对象
        //返回数据
        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     * 内存查询
     *
     * @param tagNameList
     * @return
     */
    public List<User> memorySearch(List<String> tagNameList) {
        long starTime = System.currentTimeMillis();
        //1.先查询所有用户
        QueryWrapper queryWrapper = new QueryWrapper();


        //一定要指定List的泛型为User
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();

        //2.判断内存中是否包含要求的标签,过滤含有标签的用户对象
        return userList.stream().filter(user -> {
            String tagsStr = user.getTags();
            if (StringUtils.isBlank(tagsStr)) {
                return false;
            }

            Set<String> tempTagNameSet = gson.fromJson(tagsStr, new TypeToken<List<User>>() {
            }.getType());
            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
            for (String tagName : tagNameList) {
                if (!tempTagNameSet.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     * 获取当前用户
     *
     * @param request
     * @return
     */
    @Override
    public User getCurrentUser(HttpServletRequest request) {

        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = currentUser.getId();
        // TODO 校验用户是否合法
        User user1 = userMapper.selectById(userId);
        User safetyUser = getSafetyUser(user1);
        return safetyUser;
    }

    /**
     * 更新用户信息
     *
     * @param userUpdateRequest
     * @param currentUser
     * @return
     */
    @Override
    public int updateUser(UserUpdateRequest userUpdateRequest, User currentUser) {


        if (userUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "用户未登录");
        }

        String username = userUpdateRequest.getUsername();
        String phone = userUpdateRequest.getPhone();
        String email = userUpdateRequest.getEmail();
        Integer gender = userUpdateRequest.getGender();
        String avatarUrl = userUpdateRequest.getAvatarUrl();
        String profile = userUpdateRequest.getProfile();

        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("id", currentUser.getId());
        User updateUser = userMapper.selectOne(queryWrapper);

        //如果前端传过来的用户数据都为空，则不更新用户数据
        if (StringUtils.isAllBlank(username, phone, email, avatarUrl, profile) && gender == null) {
            return 0;
        }

        updateUser.setUsername(username);
        updateUser.setPhone(phone);
        updateUser.setEmail(email);
        updateUser.setGender(gender);
        updateUser.setAvatarUrl(avatarUrl);
        updateUser.setProfile(profile);

        return userMapper.updateById(updateUser);
    }

    @Override
    public List<User> getUserList() {

        QueryWrapper queryWrapper = new QueryWrapper();

        List<User> userList = userMapper.selectList(queryWrapper);
        //用户信息脱敏
        userList = userList.stream().map(user -> {
            return getSafetyUser(user);
        }).collect(Collectors.toList());

        return userList;
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 匹配用户
     *
     * @param num
     * @param currentUser
     * @return
     */
    @Override
    public List<User> matchUsers(long num, User currentUser) {
        //获取当前登录用户的标签信息
        String tags = currentUser.getTags();
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());

        //查询所有用户 tags ！= null
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("tags").select("id", "tags");
        List<User> userList = this.list(queryWrapper);

        //用户列表的下表 =》相似度 key-value
        List<Pair<User, Long>> list = new ArrayList<>();

        //遍历用户集合userList 计算两个标签的相似度 值越小，越匹配
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            //过滤掉没有标签的用户，且用户为当前登录用户
            if (userTags == null || currentUser.getId() == user.getId()) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            //计算distance 值越小相似度越高
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            list.add(new Pair<>(user, distance));
        }
        //对匹配到的数据进行排序，值越小越靠前 从小到大排序
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        //从topUserPairList中取出已匹配的用户的id
        List<Long> userIdList = topUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());

        //再次根据id查询完整的脱敏用户信息
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userIdList);
        Map<Long, List<User>> userListMap = this.list(userQueryWrapper).stream().map(this::getSafetyUser).collect(Collectors.groupingBy(User::getId));

        //因为上面查询打乱了顺序，这里根据上面有序userId列表赋值
        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userIdList) {
            finalUserList.add(userListMap.get(userId).get(0));
        }


        return finalUserList;
    }



}

