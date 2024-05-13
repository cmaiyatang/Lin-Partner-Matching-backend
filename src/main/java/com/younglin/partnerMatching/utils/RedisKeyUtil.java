package com.younglin.partnerMatching.utils;

import javax.xml.bind.annotation.XmlType;

import static com.younglin.partnerMatching.contant.RedisKeyConstant.*;
import static org.springframework.session.data.redis.RedisIndexedSessionRepository.DEFAULT_NAMESPACE;

public class RedisKeyUtil {

    /**
     * 获取已登录用户的IP 和 sessionId 对应的 key
     * @param userId 用户id
     * @return
     */
    public static String getUserExtraInfoKey(Long userId){
        return USER_EXTRA_INFO + userId;
    }

    public static String getSessionKey(String sessionId){
        return DEFAULT_NAMESPACE + ":" + SESSION_KEY_POSTFIX + ":" + sessionId;
    }

    /**
     * 获取session中某一属性的key
     * @param attrName 属性名称
     * @return
     */
    public static String getSessionAttrKey(String attrName){
        return SESSION_ATTRIBUTE_PREFIX + ":" + attrName;
    }

}














