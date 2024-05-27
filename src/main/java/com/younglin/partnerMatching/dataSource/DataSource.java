package com.younglin.partnerMatching.dataSource;

import java.util.List;

public interface DataSource {

    List doSearch(String searchText, Integer pageNum, Integer pageSize);

}
