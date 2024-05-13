package com.younglin.partnerMatching.model.domain;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class UserLoginRedisInfo implements Serializable {

    private User user;

    private String ip;
}
