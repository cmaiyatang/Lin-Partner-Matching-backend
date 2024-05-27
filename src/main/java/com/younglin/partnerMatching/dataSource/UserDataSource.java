package com.younglin.partnerMatching.dataSource;

import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.model.vo.UserVO;
import com.younglin.partnerMatching.service.UserService;
import com.younglin.partnerMatching.utils.RedisKeyUtil;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class UserDataSource implements DataSource {
    @Resource
    private UserService userService;

    @Override
    public List<UserVO> doSearch(String searchText, Integer pageNum, Integer pageSize) {
        List<String> tagNameList = new ArrayList<>();
        tagNameList.add(searchText);
        List<UserVO> userVOList = userService.searchUserByTags(tagNameList);

        return userVOList;
    }
}
