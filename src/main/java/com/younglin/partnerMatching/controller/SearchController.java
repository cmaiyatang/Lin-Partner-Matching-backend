package com.younglin.partnerMatching.controller;

import com.younglin.partnerMatching.common.BaseResponse;
import com.younglin.partnerMatching.common.ResultUtils;
import com.younglin.partnerMatching.manager.SearchFacade;
import com.younglin.partnerMatching.model.request.SearchRequest;
import com.younglin.partnerMatching.model.vo.SearchVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 统一搜索接口
 *
 * @author YoungLin
 */
@RestController
@Slf4j
@RequestMapping("/search")
public class SearchController {

    @Resource
    private SearchFacade searchFacade;

    @PostMapping("/all")
    public BaseResponse<SearchVO> searchAll(@RequestBody SearchRequest searchRequest, HttpServletRequest request) {
        return ResultUtils.success(searchFacade.searchAll(searchRequest, request));
    }


}
