package com.dzc.dzcaicodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用版本快照 VO
 *
 * @author dzc
 */
@Data
public class AppVersionSnapshotVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 版本缩略图URL
     */
    private String thumbnail;

    /**
     * 变更摘要
     */
    private String changeSummary;

    /**
     * 对应的对话记录ID
     */
    private Long chatHistoryId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 是否为当前版本
     */
    private Boolean isCurrent;

}
