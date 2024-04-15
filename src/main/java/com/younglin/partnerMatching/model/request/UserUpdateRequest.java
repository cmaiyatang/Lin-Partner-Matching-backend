package com.younglin.partnerMatching.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;


@Data
public class UserUpdateRequest implements Serializable {
    private String username;
    private String phone;
    private String email;
    private Integer gender;
    private String avatarUrl;
}
