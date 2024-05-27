package com.younglin.partnerMatching;

import com.younglin.partnerMatching.manager.SessionManager;
import com.younglin.partnerMatching.utils.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;

import javax.annotation.Resource;

import static com.younglin.partnerMatching.contant.RedisKeyConstant.USER_LOGIN_STATE;

@SpringBootTest(classes = UserCenterApplication.class)
@Slf4j
public class UserLogoutTest {
    @Resource
    private SessionManager sessionManager;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIndexedSessionRepository sessionRepository;

    @Test
    public void testUesrLogout() {
        String sessionId = "ea99fcb6-a926-460c-b78e-4f410d4fd138";
        Long userId = 2L;
        String sessionKey = RedisKeyUtil.getSessionKey(sessionId);
        String sessionAttrKey = RedisKeyUtil.getSessionAttrKey(USER_LOGIN_STATE);
        Object o = stringRedisTemplate.opsForHash().get(sessionKey, sessionAttrKey);
        System.out.println(o);
        sessionManager.removeOtherSessionLoginAttribute(sessionId,userId);
//        ea99fcb6-a926-460c-b78e-4f410d4fd138


//        Long delete = sessionRepository.getSessionRedisOperations().opsForHash().delete(sessionKey,sessionAttrKey);

    }

}
