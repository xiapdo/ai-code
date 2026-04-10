package com.dzc.dzcaicodemother.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 应用版本快照实体类。
 *
 * @author dzc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app_version_snapshot")
public class AppVersionSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 应用id
     */
    @Column("appId")
    private Long appId;

    /**
     * 版本号
     */
    @Column("versionNo")
    private Integer versionNo;

    /**
     * 版本快照数据（JSON格式）
     */
    @Column("snapshotData")
    private String snapshotData;

    /**
     * 版本缩略图URL
     */
    @Column("thumbnail")
    private String thumbnail;

    /**
     * 变更摘要
     */
    @Column("changeSummary")
    private String changeSummary;

    /**
     * 对应的对话记录ID（用于跳转）
     */
    @Column("chatHistoryId")
    private Long chatHistoryId;

    /**
     * 创建用户id
     */
    @Column("userId")
    private Long userId;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
