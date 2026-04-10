package com.dzc.dzcaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.dzc.dzcaicodemother.constant.AppConstant;
import com.dzc.dzcaicodemother.exception.BusinessException;
import com.dzc.dzcaicodemother.exception.ErrorCode;
import com.dzc.dzcaicodemother.exception.ThrowUtils;
import com.dzc.dzcaicodemother.mapper.AppVersionSnapshotMapper;
import com.dzc.dzcaicodemother.model.entity.App;
import com.dzc.dzcaicodemother.model.entity.AppVersionSnapshot;
import com.dzc.dzcaicodemother.model.entity.ChatHistory;
import com.dzc.dzcaicodemother.model.entity.ChatHistoryOriginal;
import com.dzc.dzcaicodemother.model.vo.AppVersionSnapshotVO;
import com.dzc.dzcaicodemother.service.AppService;
import com.dzc.dzcaicodemother.service.AppVersionSnapshotService;
import com.dzc.dzcaicodemother.service.ChatHistoryOriginalService;
import com.dzc.dzcaicodemother.service.ChatHistoryService;
import com.dzc.dzcaicodemother.service.ScreenshotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用版本快照 服务层实现。
 *
 * @author dzc
 */
@Service
@Slf4j
public class AppVersionSnapshotServiceImpl extends ServiceImpl<AppVersionSnapshotMapper, AppVersionSnapshot> implements AppVersionSnapshotService {

    @Value("${deploy-host:http://localhost}")
    private String deployHost;

    @Resource
    private AppService appService;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ChatHistoryOriginalService chatHistoryOriginalService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveVersionSnapshot(Long appId, String snapshotData, String changeSummary, Long chatHistoryId, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");
        ThrowUtils.throwIf(StrUtil.isBlank(snapshotData), ErrorCode.PARAMS_ERROR, "快照数据不能为空");

        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        Integer currentVersionNo = app.getVersionNo();
        int newVersionNo = (currentVersionNo == null ? 0 : currentVersionNo) + 1;

        AppVersionSnapshot snapshot = new AppVersionSnapshot();
        snapshot.setAppId(appId);
        snapshot.setVersionNo(newVersionNo);
        snapshot.setSnapshotData(snapshotData);
        snapshot.setChangeSummary(StrUtil.isBlank(changeSummary) ? "版本 " + newVersionNo : changeSummary);
        snapshot.setChatHistoryId(chatHistoryId);
        snapshot.setUserId(userId);

        boolean saved = this.save(snapshot);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "保存版本快照失败");

        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setVersionNo(newVersionNo);
        updateApp.setCurrentVersionId(snapshot.getId());
        appService.updateById(updateApp);

        Thread.startVirtualThread(() -> {
            try {
                generateVersionThumbnailAsync(appId, snapshot.getId(), newVersionNo);
            } catch (Exception e) {
                log.error("生成版本缩略图失败: appId={}, versionId={}", appId, snapshot.getId(), e);
            }
        });

        log.info("保存版本快照成功: appId={}, versionNo={}, versionId={}", appId, newVersionNo, snapshot.getId());
        return snapshot.getId();
    }

    @Override
    public Page<AppVersionSnapshotVO> listVersionByPage(Long appId, int pageNum, int pageSize) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "每页最多查询50个版本");

        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        Long currentVersionId = app.getCurrentVersionId();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId)
                .orderBy("createTime", true);

        Page<AppVersionSnapshot> snapshotPage = this.page(Page.of(pageNum, pageSize), queryWrapper);

        Page<AppVersionSnapshotVO> voPage = new Page<>(pageNum, pageSize, snapshotPage.getTotalRow());
        voPage.setRecords(snapshotPage.getRecords().stream().map(snapshot -> {
            AppVersionSnapshotVO vo = new AppVersionSnapshotVO();
            BeanUtil.copyProperties(snapshot, vo);
            vo.setIsCurrent(snapshot.getId().equals(currentVersionId));
            return vo;
        }).toList());

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollbackToVersion(Long versionId, Long appId, Long userId) {
        return rollbackToVersion(versionId, appId, userId, true);
    }

@Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollbackToVersion(Long versionId, Long appId, Long userId, boolean deleteAfterRollback) {
        log.info("开始回退版本: versionId={}, appId={}, userId={}, deleteAfterRollback={}", versionId, appId, userId, deleteAfterRollback);

        ThrowUtils.throwIf(versionId == null || versionId <= 0, ErrorCode.PARAMS_ERROR, "版本ID错误");
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");

        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限回退该应用");
        }

        AppVersionSnapshot snapshot = this.getById(versionId);
        ThrowUtils.throwIf(snapshot == null, ErrorCode.NOT_FOUND_ERROR, "版本快照不存在");
        ThrowUtils.throwIf(!snapshot.getAppId().equals(appId), ErrorCode.PARAMS_ERROR, "版本快照不属于该应用");

        // 获取被回退版本的版本号和创建时间
        Integer targetVersionNo = snapshot.getVersionNo();
        LocalDateTime targetVersionTime = snapshot.getCreateTime();
        log.info("回退到版本: versionNo={}, createTime={}", targetVersionNo, targetVersionTime);

        // 恢复代码文件
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        log.info("恢复代码文件到: {}", sourceDirPath);

        FileUtil.del(sourceDirPath);
        FileUtil.mkdir(sourceDirPath);

        AppVersionSnapshotData snapshotData = parseSnapshotData(snapshot.getSnapshotData());
        if (snapshotData != null && CollUtil.isNotEmpty(snapshotData.getFiles())) {
            for (FileInfo file : snapshotData.getFiles()) {
                String filePath = sourceDirPath + File.separator + file.getPath();
                FileUtil.writeUtf8String(file.getContent(), filePath);
            }
        }

// 更新当前版本ID和版本号为被回退的版本
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setCurrentVersionId(versionId);
        updateApp.setVersionNo(targetVersionNo);
        appService.updateById(updateApp);

if (deleteAfterRollback) {
        // 删除被回退版本之后的所有版本，使用 gt 而非 ge 避免删除被回退的版本本身
        QueryWrapper deleteWrapper = QueryWrapper.create()
            .eq("appId", appId)
            .gt("createTime", targetVersionTime)
            .orderBy("createTime", true);

        List<AppVersionSnapshot> versionsToDelete = this.list(deleteWrapper);

        // 删除版本记录
        this.remove(deleteWrapper);
        log.info("回退版本成功，删除 {} 个后续版本: appId={}", versionsToDelete.size(), appId);

        // 删除目标版本之后的所有对话历史记录（不限于下一个版本）
        LocalDateTime endTime;
        if (versionsToDelete != null && !versionsToDelete.isEmpty()) {
            // 获取最后一个被删除版本的创建时间（最新的版本），删除到最新版本的所有记录
            endTime = versionsToDelete.get(versionsToDelete.size() - 1).getCreateTime();
        } else {
            // 没有后续版本，删除targetVersionTime之后的所有记录
            endTime = LocalDateTime.now();
        }

        // 查询时间区间内的所有对话历史记录
        QueryWrapper chatHistoryQueryWrapper = QueryWrapper.create()
            .eq("appId", appId)
            .gt("createTime", targetVersionTime)
            .le("createTime", endTime);

        List<ChatHistory> chatHistoriesToDelete = chatHistoryService.list(chatHistoryQueryWrapper);

        // 遍历更新为逻辑删除
        for (ChatHistory chatHistory : chatHistoriesToDelete) {
            ChatHistory updateEntity = new ChatHistory();
            updateEntity.setId(chatHistory.getId());
            updateEntity.setIsDelete(1);
            chatHistoryService.updateById(updateEntity);
        }

        List<ChatHistoryOriginal> chatHistoryOriginalsToDelete = chatHistoryOriginalService.list(chatHistoryQueryWrapper);
        for (ChatHistoryOriginal chatHistoryOriginal : chatHistoryOriginalsToDelete) {
            ChatHistoryOriginal updateEntity = new ChatHistoryOriginal();
            updateEntity.setId(chatHistoryOriginal.getId());
            updateEntity.setIsDelete(1);
            chatHistoryOriginalService.updateById(updateEntity);
        }

log.info("删除时间区间内的对话历史: appId={}, startTime={}, endTime={}, count={}", appId, targetVersionTime, endTime, chatHistoriesToDelete.size());
        } else {
            log.info("回退版本成功: appId={}, versionId={}", appId, versionId);
        }
        return true;
    }

    @Override
    public AppVersionSnapshot getVersionDetail(Long versionId) {
        ThrowUtils.throwIf(versionId == null || versionId <= 0, ErrorCode.PARAMS_ERROR, "版本ID错误");
        return this.getById(versionId);
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        if (appId == null || appId <= 0) {
            return false;
        }
        QueryWrapper queryWrapper = QueryWrapper.create().eq("appId", appId);
        return this.remove(queryWrapper);
    }

private void generateVersionThumbnailAsync(Long appId, Long versionId, int versionNo) {
    log.info("开始生成版本缩略图: appId={}, versionId={}, versionNo={}", appId, versionId, versionNo);
    App app = appService.getById(appId);
    if (app == null) {
        log.warn("应用不存在，跳过生成缩略图: appId={}", appId);
        return;
    }
    if (StrUtil.isBlank(app.getDeployKey())) {
        log.warn("应用未部署(deployKey为空)，跳过生成缩略图: appId={}", appId);
        return;
    }
    if (StrUtil.isBlank(deployHost)) {
        log.error("deploy-host未配置，无法生成缩略图");
        return;
    }

    String appUrl = String.format("%s/%s/", deployHost, app.getDeployKey());
    log.info("生成缩略图的URL: {}", appUrl);

    try {
        String thumbnailUrl = screenshotService.generateAndUploadScreenshot(appUrl);
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            AppVersionSnapshot updateSnapshot = new AppVersionSnapshot();
            updateSnapshot.setId(versionId);
            updateSnapshot.setThumbnail(thumbnailUrl);
            boolean updated = this.updateById(updateSnapshot);
            if (updated) {
                log.info("版本缩略图生成并保存成功: appId={}, versionNo={}, thumbnailUrl={}", appId, versionNo, thumbnailUrl);
            } else {
                log.error("版本缩略图保存失败: appId={}, versionId={}", appId, versionId);
            }
        } else {
            log.error("截图服务返回空URL: appId={}", appId);
        }
    } catch (Exception e) {
        log.error("生成版本缩略图异常: appId={}, versionId={}", appId, versionId, e);
    }
}

    private AppVersionSnapshotData parseSnapshotData(String snapshotData) {
        try {
            return cn.hutool.json.JSONUtil.toBean(snapshotData, AppVersionSnapshotData.class);
        } catch (Exception e) {
            log.error("解析快照数据失败", e);
            return null;
        }
    }

    private static class AppVersionSnapshotData {
        private java.util.List<FileInfo> files;

        public java.util.List<FileInfo> getFiles() {
            return files;
        }

        public void setFiles(java.util.List<FileInfo> files) {
            this.files = files;
        }
    }

    private static class FileInfo {
        private String path;
        private String content;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

}
