package com.dzc.dzcaicodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.dzc.dzcaicodemother.common.BaseResponse;
import com.dzc.dzcaicodemother.common.ResultUtils;
import com.dzc.dzcaicodemother.exception.ErrorCode;
import com.dzc.dzcaicodemother.exception.ThrowUtils;
import com.dzc.dzcaicodemother.model.dto.version.VersionQueryRequest;
import com.dzc.dzcaicodemother.model.dto.version.VersionRollbackRequest;
import com.dzc.dzcaicodemother.model.entity.App;
import com.dzc.dzcaicodemother.model.entity.AppVersionSnapshot;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.model.vo.AppVersionSnapshotVO;
import com.dzc.dzcaicodemother.service.AppService;
import com.dzc.dzcaicodemother.service.AppVersionSnapshotService;
import com.dzc.dzcaicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 应用版本 控制层。
 *
 * @author dzc
 */
@RestController
@RequestMapping("/version")
public class AppVersionController {

    @Resource
    private AppVersionSnapshotService appVersionSnapshotService;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    /**
     * 分页获取版本列表
     *
     * @param versionQueryRequest 查询请求
     * @return 版本列表
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<AppVersionSnapshotVO>> listVersionByPage(
            @RequestBody VersionQueryRequest versionQueryRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(versionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = versionQueryRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");

        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new RuntimeException("无权限查看该应用的版本历史");
        }

        int pageNum = versionQueryRequest.getPageNum() != null ? versionQueryRequest.getPageNum() : 1;
        int pageSize = versionQueryRequest.getPageSize() != null ? versionQueryRequest.getPageSize() : 20;

        Page<AppVersionSnapshotVO> versionPage = appVersionSnapshotService.listVersionByPage(appId, pageNum, pageSize);
        return ResultUtils.success(versionPage);
    }

    /**
     * 回退到指定版本
     *
     * @param versionRollbackRequest 回退请求
     * @param request 请求
     * @return 是否成功
     */
    @PostMapping("/rollback")
    public BaseResponse<Boolean> rollbackVersion(
            @RequestBody VersionRollbackRequest versionRollbackRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(versionRollbackRequest == null, ErrorCode.PARAMS_ERROR);

        Long versionId = versionRollbackRequest.getVersionId();
        Long appId = versionRollbackRequest.getAppId();
        Boolean deleteAfterRollback = versionRollbackRequest.getDeleteAfterRollback();
        if (deleteAfterRollback == null) {
            deleteAfterRollback = true;
        }
        ThrowUtils.throwIf(versionId == null || versionId <= 0, ErrorCode.PARAMS_ERROR, "版本ID错误");
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");

        User loginUser = userService.getLoginUser(request);

        boolean result = appVersionSnapshotService.rollbackToVersion(versionId, appId, loginUser.getId(), deleteAfterRollback);
        return ResultUtils.success(result);
    }

    /**
     * 获取版本详情
     *
     * @param versionId 版本ID
     * @return 版本详情
     */
    @GetMapping("/get/{versionId}")
    public BaseResponse<AppVersionSnapshot> getVersionDetail(@PathVariable Long versionId, HttpServletRequest request) {
        ThrowUtils.throwIf(versionId == null || versionId <= 0, ErrorCode.PARAMS_ERROR, "版本ID错误");

        AppVersionSnapshot snapshot = appVersionSnapshotService.getVersionDetail(versionId);
        ThrowUtils.throwIf(snapshot == null, ErrorCode.NOT_FOUND_ERROR, "版本快照不存在");

        App app = appService.getById(snapshot.getAppId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new RuntimeException("无权限查看该版本详情");
        }

        return ResultUtils.success(snapshot);
    }

}
