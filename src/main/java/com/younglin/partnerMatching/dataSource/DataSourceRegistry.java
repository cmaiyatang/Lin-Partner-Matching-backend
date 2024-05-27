package com.younglin.partnerMatching.dataSource;


import com.younglin.partnerMatching.contant.SearchTypeEnum;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Component
public class DataSourceRegistry {

    @Resource
    private TeamDataSource teamDataSource;

    @Resource
    private UserDataSource userDataSource;


    private Map<String,DataSource> dataSourceMap;

    //当bean加载完成后再初始化map
    @PostConstruct
    public void doInitMap(){
        dataSourceMap = new HashMap(){{
            put(SearchTypeEnum.USERTYPE.getValue(),userDataSource);
            put(SearchTypeEnum.TEAMTYPE.getValue(),teamDataSource);
        }};
    }

    //根据type拿到dataSource
    public DataSource getDataSourceByType(String type){
        if (dataSourceMap == null){
            return null;
        }
        return dataSourceMap.get(type);
    }


}
