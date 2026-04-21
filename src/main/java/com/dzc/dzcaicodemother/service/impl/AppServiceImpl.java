package com.dzc.dzcaicodemother.service.impl;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.api.RKeys;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.dzc.dzcaicodemother.ai.AiCodeGenTypeRoutingService;
import com.dzc.dzcaicodemother.ai.AiCodeGenTypeRoutingServiceFactory;
import com.dzc.dzcaicodemother.constant.AppConstant;
import com.dzc.dzcaicodemother.core.AiCodeGeneratorFacade;
import com.dzc.dzcaicodemother.core.builder.VueProjectBuilder;
import com.dzc.dzcaicodemother.core.handler.StreamHandlerExecutor;
import com.dzc.dzcaicodemother.exception.BusinessException;
import com.dzc.dzcaicodemother.exception.ErrorCode;
import com.dzc.dzcaicodemother.exception.ThrowUtils;
import com.dzc.dzcaicodemother.mapper.AppMapper;
import com.dzc.dzcaicodemother.model.dto.app.AppAddRequest;
import com.dzc.dzcaicodemother.model.dto.app.AppQueryRequest;
import com.dzc.dzcaicodemother.model.entity.App;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.dzc.dzcaicodemother.model.enums.CodeGenTypeEnum;
import com.dzc.dzcaicodemother.model.vo.AppVO;
import com.dzc.dzcaicodemother.model.vo.AppViewStatVO;
import com.dzc.dzcaicodemother.model.vo.UserVO;
import com.dzc.dzcaicodemother.monitor.MonitorContext;
import com.dzc.dzcaicodemother.monitor.MonitorContextHolder;
import com.dzc.dzcaicodemother.service.AppService;
import com.dzc.dzcaicodemother.service.AppVersionSnapshotService;
import com.dzc.dzcaicodemother.service.ChatHistoryOriginalService;
import com.dzc.dzcaicodemother.service.ChatHistoryService;
import com.dzc.dzcaicodemother.service.ScreenshotService;
import com.dzc.dzcaicodemother.service.UserService;
import com.dzc.dzcaicodemother.utils.InteractionCacheKeyUtils;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 应用 服务层实现。
 *
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    private static final int VIEW_STAT_RETENTION_DAYS = 90;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${deploy-host:http://localhost}")
    private String deployHost;

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private RedissonClient redissonClient;

@Resource
private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

@Resource
private ChatHistoryOriginalService chatHistoryOriginalService;

@Resource
@Lazy
private AppVersionSnapshotService appVersionSnapshotService;

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验，仅本人可以和自己的应用对话
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用代码生成类型错误");
        }
        //5. 通过校验后将用户消息添加到对话历史
        chatHistoryOriginalService.addOriginalChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 6. 在调用 AI 前，先保存用户消息到数据库中
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 7. 设置监控上下文（用户 ID 和应用 ID）
        MonitorContextHolder.setContext(
                MonitorContext.builder()
                        .userId(loginUser.getId().toString())
                        .appId(appId.toString())
                        .build()
        );
        // 8. 调用 AI 生成代码（流式）
        // 转换为ServerSentEvent格式
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        // 9. 收集 AI 响应的内容，并且在完成后保存记录到对话历史
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, chatHistoryOriginalService, appId, loginUser, codeGenTypeEnum)
                .doFinally(signalType -> {
                    // 流结束时清理（无论成功/失败/取消）
                    MonitorContextHolder.clearContext();
                });
    }

    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 使用 AI 智能选择代码生成类型（多例模式）
        AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), selectedCodeGenType.getValue());
        return app.getId();
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验，仅本人可以部署自己的应用
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 如果没有，则生成 6 位 deployKey（字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，获取原始代码生成路径（应用访问目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查路径是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码路径不存在，请先生成应用");
        }
        // 7. Vue 项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请重试");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 构建完成后，需要将构建后的文件复制到部署目录
            sourceDir = distDir;
        }
        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用部署失败：" + e.getMessage());
        }
// 9. 更新数据库
App updateApp = new App();
updateApp.setId(appId);
updateApp.setDeployKey(deployKey);
updateApp.setDeployedTime(LocalDateTime.now());
boolean updateResult = this.updateById(updateApp);
ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
// 10. 构建应用访问 URL
String appDeployUrl = String.format("%s/%s/", deployHost, deployKey); // 11. 异步生成截图并且更新应用封面
generateAppScreenshotAsync(appId, appDeployUrl);
// 12. 异步更新当前版本的缩略图
final Long currentVersionId = app.getCurrentVersionId();
if (currentVersionId != null) {
    Thread.startVirtualThread(() -> {
        try {
            String thumbnailUrl = screenshotService.generateAndUploadScreenshot(appDeployUrl);
            if (StrUtil.isNotBlank(thumbnailUrl)) {
                com.dzc.dzcaicodemother.model.entity.AppVersionSnapshot updateSnapshot = new com.dzc.dzcaicodemother.model.entity.AppVersionSnapshot();
                updateSnapshot.setId(currentVersionId);
                updateSnapshot.setThumbnail(thumbnailUrl);
                appVersionSnapshotService.updateById(updateSnapshot);
                log.info("部署后更新版本缩略图成功: appId={}, versionId={}, thumbnailUrl={}", appId, currentVersionId, thumbnailUrl);
            }
        } catch (Exception e) {
            log.error("部署后更新版本缩略图失败: appId={}, versionId={}", appId, currentVersionId, e);
        }
    });
}
return appDeployUrl;
    }

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程并执行
            Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            // 更新数据库的封面
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        });
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        appVO.setLikeCount(app.getLikeCount() != null ? app.getLikeCount() : 0);
        appVO.setCollectCount(app.getCollectCount() != null ? app.getCollectCount() : 0);
        appVO.setCommentCount(app.getCommentCount() != null ? app.getCommentCount() : 0);
        appVO.setViewCount(app.getViewCount() != null ? app.getViewCount() : 0);
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 删除应用时，关联删除对话历史
     *
     * @param id
     * @return
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        long appId = Long.parseLong(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
            chatHistoryOriginalService.deleteByAppId(appId);
        } catch (Exception e) {
            log.error("删除应用关联的对话历史失败：{}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }

    @Override
    public void recordView(Long appId, Long userId, String ip) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");
        
        String today = LocalDate.now().format(DATE_FORMATTER);
        String appViewKey = InteractionCacheKeyUtils.getAppViewKey(appId, today);
        
        RSet<String> viewSet = redissonClient.getSet(appViewKey);
        
        // 已登录用户使用userId，游客使用IP地址作为标识
        String viewIdentifier = userId != null ? userId.toString() : (ip != null ? "ip_" + ip : "unknown");
        
        if (viewSet.contains(viewIdentifier)) {
            return;
        }
        
        // 添加到Redis去重集合
        viewSet.add(viewIdentifier);
        viewSet.expire(1, TimeUnit.DAYS);
        
        // 异步更新：仅写Redis计数器，不立即更新数据库
        String appViewStatKey = InteractionCacheKeyUtils.getAppViewStatKey(appId, today);
        RSet<String> statSet = redissonClient.getSet(appViewStatKey);
        statSet.add(viewIdentifier);
        statSet.expire(VIEW_STAT_RETENTION_DAYS + 1, TimeUnit.DAYS);
        
        log.debug("记录应用浏览(仅Redis): appId={}, userId={}, ip={}", appId, userId, ip);
    }

    @Override
    public AppViewStatVO getViewStat(Long appId, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");
        
        // 先尝试从缓存获取
        String cacheKey = InteractionCacheKeyUtils.getAppViewStatCacheKey(appId);
        RMap<String, String> cacheMap = redissonClient.getMap(cacheKey);
        String cachedStat = cacheMap.get("data");
        if (cachedStat != null && !cachedStat.isEmpty()) {
            log.debug("命中浏览统计缓存: appId={}", appId);
            return JSONUtil.toBean(cachedStat, AppViewStatVO.class);
        }
        
        // 缓存未命中，从数据库获取
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        
        Map<String, Integer> viewStatData = parseViewStatData(app.getViewStatData());
        
        String today = LocalDate.now().format(DATE_FORMATTER);
        String yesterday = LocalDate.now().minusDays(1).format(DATE_FORMATTER);
        
        int totalViewCount = app.getViewCount() != null ? app.getViewCount() : 0;
        int todayViewCount = viewStatData.getOrDefault(today, 0);
        int yesterdayViewCount = viewStatData.getOrDefault(yesterday, 0);
        
        int avgViewCount = 0;
        if (!viewStatData.isEmpty()) {
            avgViewCount = totalViewCount / viewStatData.size();
        }
        
        Map<String, Integer> viewTrend = new LinkedHashMap<>();
        for (int i = VIEW_STAT_RETENTION_DAYS - 1; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(DATE_FORMATTER);
            viewTrend.put(date, viewStatData.getOrDefault(date, 0));
        }
        
        AppViewStatVO statVO = new AppViewStatVO();
        statVO.setAppId(appId);
        statVO.setTotalViewCount(totalViewCount);
        statVO.setTodayViewCount(todayViewCount);
        statVO.setYesterdayViewCount(yesterdayViewCount);
        statVO.setAvgViewCount(avgViewCount);
        statVO.setViewTrend(viewTrend);
        
        // 写入缓存，5分钟过期
        cacheMap.put("data", JSONUtil.toJsonStr(statVO));
        cacheMap.expire(5, TimeUnit.MINUTES);
        
        return statVO;
    }

    /**
     * 解析浏览统计数据
     *
     * @param viewStatDataJson JSON格式的浏览统计数据
     * @return 浏览统计Map
     */
    private Map<String, Integer> parseViewStatData(String viewStatDataJson) {
        if (StrUtil.isBlank(viewStatDataJson)) {
            return new LinkedHashMap<>();
        }
        try {
            JSONObject json = JSONUtil.parseObj(viewStatDataJson);
            Map<String, Integer> result = new LinkedHashMap<>();
            for (String key : json.keySet()) {
                result.put(key, json.getInt(key, 0));
            }
            return result;
        } catch (Exception e) {
            log.error("解析浏览统计数据失败: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    /**
     * 保留最近N天的浏览统计数据
     *
     * @param viewStatData 浏览统计数据
     * @param days         保留天数
     * @return 过滤后的浏览统计数据
     */
    private Map<String, Integer> retainRecentDays(Map<String, Integer> viewStatData, int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : viewStatData.entrySet()) {
            try {
                LocalDate date = LocalDate.parse(entry.getKey(), DATE_FORMATTER);
                if (!date.isBefore(cutoffDate)) {
                    result.put(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                log.warn("无效的日期格式: {}", entry.getKey());
            }
        }
        return result;
    }

    /**
     * 定时任务：每5分钟将Redis中的浏览量数据同步到数据库
     */
    @Scheduled(fixedRate = 300000)
    public void syncViewStatsToDatabase() {
        log.info("开始同步浏览统计数据到数据库...");
        
        try {
            RKeys keys = redissonClient.getKeys();
            String viewStatPattern = "app:view:stat:*:*";
            
            int syncedCount = 0;
            for (String key : keys.getKeysByPattern(viewStatPattern, 100)) {
                String[] parts = key.split(":");
                if (parts.length < 5) continue;
                
                // 跳过缓存键（app:view:stat:cache:{appId}）
                if ("cache".equals(parts[3])) continue;
                
                Long appId = Long.parseLong(parts[3]);
                String date = parts[4];
                
                RSet<String> viewSet = redissonClient.getSet(key);
                int viewCount = viewSet.size();
                
                if (viewCount == 0) continue;
                
                App app = this.getById(appId);
                if (app == null) {
                    keys.unlink(key);
                    continue;
                }
                
                Map<String, Integer> viewStatData = parseViewStatData(app.getViewStatData());
                viewStatData.merge(date, viewCount, Integer::sum);
                viewStatData = retainRecentDays(viewStatData, VIEW_STAT_RETENTION_DAYS);
                
                int totalViewCount = viewStatData.values().stream().mapToInt(Integer::intValue).sum();
                
                App updateApp = new App();
                updateApp.setId(appId);
                updateApp.setViewCount(totalViewCount);
                updateApp.setViewStatData(JSONUtil.toJsonStr(viewStatData));
                this.updateById(updateApp);
                
                keys.unlink(key);
                syncedCount++;
                
                log.debug("同步应用浏览统计: appId={}, date={}, count={}", appId, date, viewCount);
            }
            
            if (syncedCount > 0) {
                log.info("同步浏览统计完成: 共同步 {} 条记录", syncedCount);
            }
        } catch (Exception e) {
            log.error("同步浏览统计数据到数据库失败", e);
        }
    }
}
