package com.younglin.partnerMatching.utils;


import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.service.UserService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class GetHttpSessionUtil extends ServerEndpointConfig.Configurator {
    @Resource
    private UserService userService;

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        HttpSession httpSession = (HttpSession) request.getHttpSession();
        if (httpSession != null) {
            sec.getUserProperties().put(HttpSession.class.getName(), httpSession);
        }

    }
}
