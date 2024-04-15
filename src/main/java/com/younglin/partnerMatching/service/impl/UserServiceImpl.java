package com.younglin.partnerMatching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.common.ErrorCode;
import com.younglin.partnerMatching.exception.BusinessException;
import com.younglin.partnerMatching.model.request.UserUpdateRequest;
import com.younglin.partnerMatching.service.UserService;
import com.younglin.partnerMatching.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.younglin.partnerMatching.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

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
     * @param planetCode    星球编号
     * @return 新用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
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
        // 星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编号重复");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            return -1;
        }
        return user.getId();
    }

    // [加入星球](https://www.code-nav.cn/) 从 0 到 1 项目实战，经验拉满！10+ 原创项目手把手教程、7 日项目提升训练营、60+ 编程经验分享直播、1000+ 项目经验笔记

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
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"账号或密码错误");
        }
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
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
        safetyUser.setPlanetCode(originUser.getPlanetCode());
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
     * @param tagNameList
     * @return
     */
    public List<User> sqlSearch(List<String> tagNameList){
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
     * @param tagNameList
     * @return
     */
    public List<User> memorySearch(List<String > tagNameList){
        long starTime = System.currentTimeMillis();
        //1.先查询所有用户
        QueryWrapper queryWrapper = new QueryWrapper();


        //一定要指定List的泛型为User
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();

        //2.判断内存中是否包含要求的标签,过滤含有标签的用户对象
        return userList.stream().filter(user ->{
            String tagsStr = user.getTags();
            if(StringUtils.isBlank(tagsStr)){
                return false;
            }

            Set<String> tempTagNameSet =  gson.fromJson(tagsStr,new TypeToken<List<User>>(){}.getType());
            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
            for(String tagName : tagNameList){
                if(!tempTagNameSet.contains(tagName)) {
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
     * @param userUpdateRequest
     * @param currentUser
     * @return
     */
    @Override
    public int updateUser(UserUpdateRequest userUpdateRequest, User currentUser){


        if(userUpdateRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        if (currentUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN,"用户未登录");
        }

        String username = userUpdateRequest.getUsername();
        String phone = userUpdateRequest.getPhone();
        String email = userUpdateRequest.getEmail();
        Integer gender = userUpdateRequest.getGender();
        String avatarUrl = userUpdateRequest.getAvatarUrl();

        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("id",currentUser.getId());
        User updateUser = userMapper.selectOne(queryWrapper);

        //如果前端传过来的用户数据都为空，则不更新用户数据
        if (StringUtils.isAllBlank(username,phone,email,avatarUrl)&&gender==null){
            return 0;
        }

        updateUser.setUsername(username);
        updateUser.setPhone(phone);
        updateUser.setEmail(email);
        updateUser.setGender(gender);
        updateUser.setAvatarUrl(avatarUrl);

        return userMapper.updateById(updateUser);
    }

    @Override
    public List<User> getUserList(){

        QueryWrapper queryWrapper = new QueryWrapper();

        List<User> userList = userMapper.selectList(queryWrapper);
        //用户信息脱敏
        userList = userList.stream().map(user ->{
            return getSafetyUser(user);
        }).collect(Collectors.toList());

        return userList;
    }
}

