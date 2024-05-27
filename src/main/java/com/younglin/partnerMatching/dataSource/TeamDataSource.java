package com.younglin.partnerMatching.dataSource;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.younglin.partnerMatching.model.dto.SearchTeamDto;
import com.younglin.partnerMatching.model.vo.TeamUserVO;
import com.younglin.partnerMatching.service.TeamService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;


@Component
public class TeamDataSource implements DataSource {
    @Resource
    private TeamService teamService;


    @Override
    public List<TeamUserVO> doSearch(String searchText, Integer pageNum, Integer pageSize) {
        SearchTeamDto searchTeamDto = new SearchTeamDto();
        searchTeamDto.setSearchText(searchText);
        searchTeamDto.setPageNumber(pageNum);
        searchTeamDto.setPageSize(pageSize);

        IPage<TeamUserVO> teamUserVOIPage = teamService.searchTeams(searchTeamDto, true);
        List<TeamUserVO> teamRecords = teamUserVOIPage.getRecords();


        return teamRecords;
    }
}
