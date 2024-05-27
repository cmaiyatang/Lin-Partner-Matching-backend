package com.younglin.partnerMatching.manager;

import com.younglin.partnerMatching.common.ErrorCode;
import com.younglin.partnerMatching.dataSource.DataSource;
import com.younglin.partnerMatching.dataSource.DataSourceRegistry;
import com.younglin.partnerMatching.exception.BusinessException;
import com.younglin.partnerMatching.model.request.SearchRequest;
import com.younglin.partnerMatching.model.vo.SearchVO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 搜索门面类
 */
@Component
public class SearchFacade {
    @Resource
    private DataSourceRegistry dataSourceRegistry;

    public SearchVO searchAll(SearchRequest searchRequest, HttpServletRequest request) {
        if (searchRequest == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //拿到请求参数
        String searchText = searchRequest.getSearchText();
        int pageNumber = searchRequest.getPageNumber();
        int pageSize = searchRequest.getPageSize();
        //数据分类
        String type = searchRequest.getType();
        if (type == null){
            //搜索所有数据
            return null;
        }else {

            //根据type拿到dataSource
            DataSource dataSource = dataSourceRegistry.getDataSourceByType(type);
            //执行搜索方法
            List<?> dataList = dataSource.doSearch(searchText, pageNumber, pageSize);

            SearchVO searchVO = new SearchVO();
            searchVO.setDataVOList(dataList);
            return searchVO;
        }

    }

}
