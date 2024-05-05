package com.younglin.partnerMatching.model.request;


import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

/**
 * 分页请求体
 */
@Data
public class PageRequest{

    private int pageNumber;

    private int pageSize;
}
