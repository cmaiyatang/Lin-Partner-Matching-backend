package com.younglin.partnerMatching.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SearchVO implements Serializable {

    private List<UserVO> userVOList;

    private List<TeamUserVO> teamUserVOList;

    private List<?> dataVOList;

    private static final long serialVersionUID = 1L;

}
