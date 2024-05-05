package com.younglin.partnerMatching.model.request;

import lombok.Data;

/**
 * 通用删除请求参数
 */
@Data
public class DeleteRequest {
    /**
     * 队伍id
     */
    private Long id;
}
