package com.dzc.dzcaicodemother.model.dto.version;

import lombok.Data;

import java.io.Serializable;

/**
 * 版本查询请求
 *
 * @author dzc
 */
@Data
public class VersionQueryRequest implements Serializable {

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 20;

}
