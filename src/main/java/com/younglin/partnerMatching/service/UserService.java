package com.younglin.partnerMatching.service;

import com.younglin.partnerMatching.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.younglin.partnerMatching.model.request.UserRequest.UserUpdateRequest;
import com.younglin.partnerMatching.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户服务
 *
 * @author
 * @from
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    User getSafetyUser(User originUser);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);

    /**
     * 根据标签查询用户
     * @param tagNameList
     * @return
     */
    List<UserVO> searchUserByTags(List<String> tagNameList);


    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getCurrentUser(HttpServletRequest request);

    /**
     * 更新用户信息
     * @param userUpdateRequest
     * @param
     * @return
     */
    int updateUser(UserUpdateRequest userUpdateRequest, User currentUser);

    /**
     * 获取推荐用户的信息
     * @return
     */
    List<User> getUserList();

    boolean isAdmin(HttpServletRequest request);

    /**
     * 匹配用户 距离算法
     * @param num
     * @param currentUser
     * @return
     */
    List<User> matchUsers(long num, User currentUser);

}
