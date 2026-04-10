package com.dzc.dzcaicodemother.model.dto.version;

import lombok.Data;

import java.io.Serializable;

/**
 * 版本回退请求
 *
 * @author dzc
 */
@Data
public class VersionRollbackRequest implements Serializable {

    /**
     * 版本快照ID
     */
    private Long versionId;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 回退后是否删除版本记录
     */
    private Boolean deleteAfterRollback = true;

}
