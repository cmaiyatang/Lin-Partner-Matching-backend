package com.younglin.partnerMatching.manager;

import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.model.domain.UserLoginRedisInfo;
import com.younglin.partnerMatching.service.UserService;
import com.younglin.partnerMatching.utils.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import static com.younglin.partnerMatching.contant.RedisKeyConstant.*;

@Component
@Slf4j
public class SessionManager {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIndexedSessionRepository sessionRepository;

    @Value("${spring.session.timeout}")
    private Long sessionTimeOut;

    @Lazy
    @Resource
    private UserService userService;


    /**
     * 登录
     *
     * @param user
     * @param request
     * @return
     * @throws UnknownHostException
     */
    public String login(User user, HttpServletRequest request) throws UnknownHostException {
        String message = "登录成功";
        //获取设备的ip地址
        InetAddress localHost = InetAddress.getLocalHost();
        String ipAddress = localHost.getHostAddress();
        //检查其他端是否已登录
        String oldSessionId = this.checkOtherLogin(user.getId(), ipAddress, request);

        //不为空，说明已在其他端登录
        if (StringUtils.isNotBlank(oldSessionId)) {
            //删除oldSessionId的登录态
            this.removeOtherSessionLoginAttribute(oldSessionId, user.getId());
            message += ",已移除其他设备的登录";
        }
        UserLoginRedisInfo userLoginRedisInfo = UserLoginRedisInfo.builder()
                .user(user)
                .ip(ipAddress)
                .build();

        this.setLoginAttribute(request, USER_LOGIN_STATE, userLoginRedisInfo);

        return message;
    }


    /**
     * 检查是否已在其他端登录
     *
     * @param userId
     * @param currentIp
     * @return
     */
    public String checkOtherLogin(Long userId, String currentIp, HttpServletRequest request) {
        //拿到以前的sessionId  校验sessionId
        Object oldSessionObj =
                stringRedisTemplate.opsForHash()
                        .get(RedisKeyUtil.getUserExtraInfoKey(userId), SESSION_ID);
        String oldSessionId = null;

        if (oldSessionObj != null) {
            oldSessionId = oldSessionObj.toString();
        }

        //拿到以前的Ip   校验ip
        Object oldIpObj = stringRedisTemplate.opsForHash().get(RedisKeyUtil.getUserExtraInfoKey(userId), IP);
        String oldIp = null;

        if (oldIpObj != null) {
            oldIp = oldIpObj.toString();
        }

        //判断 sessionId 如果
        //      为空或相等 返回null
        //      不相等，判断ip 如果
        //          为空或相等，返回null
        //          不相等，返回 oldSessionId
        if (StringUtils.isBlank(oldSessionId) || oldSessionId.equals(request.getSession().getId())) {
            return null;
        } else {
            if (StringUtils.isBlank(oldIp) || oldIp.equals(currentIp)) {
                return null;
            }
            return oldSessionId;
        }

    }

    /**
     * 删除其他设备的登录态
     *
     * @param oldSessionId
     * @param userId
     */
    private void removeOtherSessionLoginAttribute(String oldSessionId, Long userId) {
        String sessionKey = RedisKeyUtil.getSessionKey(oldSessionId);
        String sessionAttrKey = RedisKeyUtil.getSessionAttrKey(USER_LOGIN_STATE);
        //删除用户的额外信息
        Boolean userExtraInfoDelete = stringRedisTemplate.delete(RedisKeyUtil.getUserExtraInfoKey(userId));
        Long delete = sessionRepository.getSessionRedisOperations().opsForHash().delete(sessionKey, sessionAttrKey);

        log.info("oldSessionId:{} ,user extra info delete result:{},user login state delete result:{}"
                , oldSessionId, userExtraInfoDelete, delete);


    }



    /**
     * 设置属性
     *
     * @param request 请求信息
     * @param key     键
     * @param value   值
     * @param login   登录
     */
    public void setAttribute(HttpServletRequest request, String key, Object value, boolean login) {
        HttpSession session = request.getSession();
        if (login) {
            UserLoginRedisInfo userLoginRedisInfo = (UserLoginRedisInfo) value;
            User user = userLoginRedisInfo.getUser();

            //存储登录态
            session.setAttribute(key, user);

            //存储sessionId 和 ip 信息
            String sessionId = session.getId();
            String userExtraInfoKey = RedisKeyUtil.getUserExtraInfoKey(user.getId());
            //put(userExtraInfoKey, SESSION_ID, sessionId)
            // 表示向名为 userExtraInfoKey 的 Hash 中存储一个键值对，
            // 其中键为 SESSION_ID，值为 sessionId。
            stringRedisTemplate.opsForHash().put(userExtraInfoKey, SESSION_ID, sessionId);
            stringRedisTemplate.opsForHash().put(userExtraInfoKey, IP, userLoginRedisInfo.getIp());
            stringRedisTemplate.expire(userExtraInfoKey, sessionTimeOut, TimeUnit.SECONDS);
        } else {
            session.setAttribute(key, value);
        }

    }

    /**
     * 设置登录属性
     *
     * @param request            请求信息
     * @param loginKey           登录键
     * @param userLoginRedisInfo 用户信息
     */
    public void setLoginAttribute(HttpServletRequest request, String loginKey, UserLoginRedisInfo userLoginRedisInfo) {
        setAttribute(request, loginKey, userLoginRedisInfo, true);
    }

    /**
     * 删除属性
     *
     * @param request 请求信息
     * @param key     键
     */
    public void removeAttribute(HttpServletRequest request, String key) {
        HttpSession session = request.getSession();
        session.removeAttribute(key);
    }

    /**
     * 退出登录
     *
     * @param request 请求信息
     */
    public void logout(HttpServletRequest request) {
        User loginUser = userService.getCurrentUser(request);
        removeAttribute(request, USER_LOGIN_STATE);
        stringRedisTemplate.delete(RedisKeyUtil.getUserExtraInfoKey(loginUser.getId()));
    }




}












