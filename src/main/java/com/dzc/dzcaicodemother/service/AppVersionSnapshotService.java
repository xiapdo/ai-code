package com.dzc.dzcaicodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.dzc.dzcaicodemother.model.entity.AppVersionSnapshot;
import com.dzc.dzcaicodemother.model.vo.AppVersionSnapshotVO;

/**
 * 应用版本快照 服务层。
 *
 * @author dzc
 */
public interface AppVersionSnapshotService extends IService<AppVersionSnapshot> {

    /**
     * 保存版本快照
     *
     * @param appId 应用ID
     * @param snapshotData 快照数据（JSON格式）
     * @param changeSummary 变更摘要
     * @param chatHistoryId 对应的对话记录ID
     * @param userId 用户ID
     * @return 版本快照ID
     */
    Long saveVersionSnapshot(Long appId, String snapshotData, String changeSummary, Long chatHistoryId, Long userId);

    /**
     * 分页获取版本列表
     *
     * @param appId 应用ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 版本快照VO分页数据
     */
    Page<AppVersionSnapshotVO> listVersionByPage(Long appId, int pageNum, int pageSize);

    /**
     * 回退到指定版本
     *
     * @param versionId 版本快照ID
     * @param appId 应用ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean rollbackToVersion(Long versionId, Long appId, Long userId);

    /**
     * 回退到指定版本
     *
     * @param versionId 版本快照ID
     * @param appId 应用ID
     * @param userId 用户ID
     * @param deleteAfterRollback 回退后是否删除版本记录
     * @return 是否成功
     */
    boolean rollbackToVersion(Long versionId, Long appId, Long userId, boolean deleteAfterRollback);

    /**
     * 获取版本详情
     *
     * @param versionId 版本快照ID
     * @return 版本快照
     */
    AppVersionSnapshot getVersionDetail(Long versionId);

    /**
     * 根据应用ID删除所有版本快照
     *
     * @param appId 应用ID
     * @return 是否成功
     */
    boolean deleteByAppId(Long appId);

}
