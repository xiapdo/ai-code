package com.dzc.dzcaicodemother.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.api.RList;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dzc.dzcaicodemother.exception.ErrorCode;
import com.dzc.dzcaicodemother.exception.ThrowUtils;
import com.dzc.dzcaicodemother.mapper.AppCommentMapper;
import com.dzc.dzcaicodemother.mapper.AppMapper;
import com.dzc.dzcaicodemother.model.dto.interaction.CommentAddRequest;
import com.dzc.dzcaicodemother.model.dto.interaction.CommentQueryRequest;
import com.dzc.dzcaicodemother.model.entity.App;
import com.dzc.dzcaicodemother.model.entity.AppComment;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.model.vo.AppVO;
import com.dzc.dzcaicodemother.model.vo.CommentVO;
import com.dzc.dzcaicodemother.model.vo.InteractionStatusVO;
import com.dzc.dzcaicodemother.service.AppService;
import com.dzc.dzcaicodemother.service.InteractionService;
import com.dzc.dzcaicodemother.service.NotificationService;
import com.dzc.dzcaicodemother.service.UserService;
import com.dzc.dzcaicodemother.utils.InteractionCacheKeyUtils;
import com.mybatisflex.core.query.QueryWrapper;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InteractionServiceImpl implements InteractionService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private AppService appService;

    @Resource
    private AppCommentMapper appCommentMapper;

    @Resource
    private AppMapper appMapper;

    @Resource
    private UserService userService;

    @Resource
    private NotificationService notificationService;

    @Override
    public InteractionStatusVO getInteractionStatus(Long appId, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        InteractionStatusVO statusVO = new InteractionStatusVO();
        statusVO.setAppId(appId);
        statusVO.setLikeCount(app.getLikeCount() != null ? app.getLikeCount() : 0);
        statusVO.setCollectCount(app.getCollectCount() != null ? app.getCollectCount() : 0);
        statusVO.setCommentCount(app.getCommentCount() != null ? app.getCommentCount() : 0);

        if (userId != null) {
            RSet<Long> likeSet = redissonClient.getSet(InteractionCacheKeyUtils.getLikeKey(appId));
            RSet<Long> collectSet = redissonClient.getSet(InteractionCacheKeyUtils.getCollectKey(appId));

            if (likeSet.isEmpty()) {
                syncLikeSetFromDb(appId);
            }
            if (collectSet.isEmpty()) {
                syncCollectSetFromDb(appId);
            }

            statusVO.setIsLiked(likeSet.contains(userId));
            statusVO.setIsCollected(collectSet.contains(userId));
        } else {
            statusVO.setIsLiked(false);
            statusVO.setIsCollected(false);
        }

        return statusVO;
    }

    @Override
    @Transactional
    public boolean toggleLike(Long appId, User user) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = user.getId();

        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        RSet<Long> likeSet = redissonClient.getSet(InteractionCacheKeyUtils.getLikeKey(appId));
        RSet<Long> userLikeSet = redissonClient.getSet(InteractionCacheKeyUtils.getUserLikeKey(userId));

        if (likeSet.isEmpty()) {
            syncLikeSetFromDb(appId);
        }

        boolean isLiked = likeSet.contains(userId);
        boolean newLikedState = !isLiked;

        if (newLikedState) {
            likeSet.add(userId);
            userLikeSet.add(appId);
        } else {
            likeSet.remove(userId);
            userLikeSet.remove(appId);
        }

        likeSet.expire(CACHE_TTL.toMillis(), TimeUnit.MILLISECONDS);
        userLikeSet.expire(CACHE_TTL.toMillis(), TimeUnit.MILLISECONDS);

        int newCount = likeSet.size();
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setLikeCount(newCount);
        appService.updateById(updateApp);

        updateLikeUserIdsInDb(appId, likeSet);

        log.info("用户 {} {} 应用 {}, 当前点赞数: {}", userId, newLikedState ? "点赞" : "取消点赞", appId, newCount);

        if (newLikedState) {
            notificationService.sendLikeNotification(appId, user, app.getAppName());
        }
        return newLikedState;
    }

    @Override
    @Transactional
    public boolean toggleCollect(Long appId, User user) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = user.getId();

        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        RSet<Long> collectSet = redissonClient.getSet(InteractionCacheKeyUtils.getCollectKey(appId));
        RSet<Long> userCollectSet = redissonClient.getSet(InteractionCacheKeyUtils.getUserCollectKey(userId));

        if (collectSet.isEmpty()) {
            syncCollectSetFromDb(appId);
        }

        boolean isCollected = collectSet.contains(userId);
        boolean newCollectedState = !isCollected;

        if (newCollectedState) {
            collectSet.add(userId);
            userCollectSet.add(appId);
        } else {
            collectSet.remove(userId);
            userCollectSet.remove(appId);
        }

        collectSet.expire(CACHE_TTL.toMillis(), TimeUnit.MILLISECONDS);
        userCollectSet.expire(CACHE_TTL.toMillis(), TimeUnit.MILLISECONDS);

        int newCount = collectSet.size();
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setCollectCount(newCount);
        appService.updateById(updateApp);

        updateCollectUserIdsInDb(appId, collectSet);

        log.info("用户 {} {} 应用 {}, 当前收藏数: {}", userId, newCollectedState ? "收藏" : "取消收藏", appId, newCount);

        if (newCollectedState) {
            notificationService.sendCollectNotification(appId, user, app.getAppName());
        }
        return newCollectedState;
    }

    @Override
    @Transactional
    public CommentVO addComment(CommentAddRequest request, User user) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = user.getId();
        Long appId = request.getAppId();

        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getContent()), ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        ThrowUtils.throwIf(request.getContent().length() > 500, ErrorCode.PARAMS_ERROR, "评论内容不能超过500字符");

        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        AppComment comment = new AppComment();
        comment.setAppId(appId);
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setReplyToId(request.getReplyToId());
        comment.setParentId(request.getParentId());
        comment.setLikeCount(0);
        comment.setStatus(1);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());

        appCommentMapper.insert(comment);

        RList<String> commentList = redissonClient.getList(InteractionCacheKeyUtils.getCommentKey(appId));

        // 如果 Redis 缓存为空，先同步历史评论（排除刚插入的这条）
        if (commentList.isEmpty()) {
            List<CommentVO> dbComments = getAllCommentsFromDb(appId);
            // 排除刚插入的这条评论
            List<CommentVO> historyComments = dbComments.stream()
                    .filter(c -> !c.getId().equals(comment.getId()))
                    .collect(Collectors.toList());
            // 反转列表顺序，使最早评论在前，最新评论在后
            Collections.reverse(historyComments);
            // 批量转换为 JSON 并添加到 Redis
            List<String> jsonList = historyComments.stream()
                    .map(JSONUtil::toJsonStr)
                    .collect(Collectors.toList());
            commentList.addAll(jsonList);
        }

        String commentJson = JSONUtil.toJsonStr(buildCommentVO(comment, user));
        commentList.add(0, commentJson);
        commentList.expire(CACHE_TTL.toMillis(), TimeUnit.MILLISECONDS);

        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setCommentCount(commentList.size());
        appService.updateById(updateApp);

        log.info("用户 {} 在应用 {} 发布评论, 评论ID: {}", userId, appId, comment.getId());

        if (request.getParentId() == null) {
            notificationService.sendCommentNotification(appId, user, app.getAppName(), request.getContent(), comment.getId());
        } else {
            Long replyToCommentId = comment.getReplyToId();
            if (replyToCommentId != null) {
                AppComment replyToComment = appCommentMapper.selectOneByQuery(
                        QueryWrapper.create().eq("id", replyToCommentId));
                if (replyToComment != null) {
                    notificationService.sendReplyNotification(appId, replyToComment.getUserId(), user, app.getAppName(), request.getContent(), comment.getId());
                }
            }
        }
        return buildCommentVO(comment, user);
    }

    @Override
    public List<CommentVO> getCommentList(CommentQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long appId = request.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");

        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;

        log.info("getCommentList 请求: appId={}, pageNum={}, pageSize={}", appId, pageNum, pageSize);

        RList<String> commentList = redissonClient.getList(InteractionCacheKeyUtils.getCommentKey(appId));
        log.info("Redis 缓存评论数: {}", commentList.size());

        List<CommentVO> allComments;
        if (commentList.isEmpty()) {
            allComments = getAllCommentsFromDb(appId);
            log.info("从数据库加载评论数: {}", allComments.size());
        } else {
            allComments = commentList.range(0, -1).stream()
                    .map(json -> JSONUtil.toBean(json, CommentVO.class))
                    .collect(Collectors.toList());
            log.info("从 Redis 缓存加载评论数: {}", allComments.size());
        }

        if (request.getParentId() != null) {
            allComments = allComments.stream()
                    .filter(c -> request.getParentId().equals(c.getParentId()))
                    .collect(Collectors.toList());
        }

        int offset = (pageNum - 1) * pageSize;
        int endIndex = Math.min(offset + pageSize, allComments.size());
        log.info("分页计算: offset={}, endIndex={}, 总数={}", offset, endIndex, allComments.size());
        if (offset >= allComments.size()) {
            return new ArrayList<>();
        }

        return allComments.subList(offset, endIndex);
    }

    private void syncLikeSetFromDb(Long appId) {
        App app = appService.getById(appId);
        if (app != null && StrUtil.isNotBlank(app.getLikeUserIds())) {
            List<Long> userIds = JSONUtil.toList(app.getLikeUserIds(), Long.class);
            RSet<Long> likeSet = redissonClient.getSet(InteractionCacheKeyUtils.getLikeKey(appId));
            likeSet.addAll(userIds);
            likeSet.expire(CACHE_TTL.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private void syncCollectSetFromDb(Long appId) {
        App app = appService.getById(appId);
        if (app != null && StrUtil.isNotBlank(app.getCollectUserIds())) {
            List<Long> userIds = JSONUtil.toList(app.getCollectUserIds(), Long.class);
            RSet<Long> collectSet = redissonClient.getSet(InteractionCacheKeyUtils.getCollectKey(appId));
            collectSet.addAll(userIds);
            collectSet.expire(CACHE_TTL.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private void updateLikeUserIdsInDb(Long appId, RSet<Long> likeSet) {
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setLikeUserIds(JSONUtil.toJsonStr(likeSet.toArray()));
        appService.updateById(updateApp);
    }

    private void updateCollectUserIdsInDb(Long appId, RSet<Long> collectSet) {
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setCollectUserIds(JSONUtil.toJsonStr(collectSet.toArray()));
        appService.updateById(updateApp);
    }

    private CommentVO buildCommentVO(AppComment comment, User user) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setAppId(comment.getAppId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setReplyToId(comment.getReplyToId());
        vo.setParentId(comment.getParentId());
        vo.setLikeCount(comment.getLikeCount());
        vo.setCreateTime(comment.getCreateTime());
        if (user != null) {
            vo.setUser(userService.getUserVO(user));
        }
        return vo;
    }

    private List<CommentVO> getCommentListFromDb(Long appId, Long parentId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("appId", appId)
                .eq("status", 1);
        if (parentId != null) {
            wrapper.eq("parentId", parentId);
        } else {
            wrapper.isNull("parentId");
        }
        wrapper.orderBy("createTime", false);
        List<AppComment> comments = appCommentMapper.selectListByQuery(wrapper);

        if (CollUtil.isEmpty(comments)) {
            return new ArrayList<>();
        }

        Set<Long> userIds = comments.stream().map(AppComment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return comments.stream().map(comment -> {
            CommentVO vo = new CommentVO();
            BeanUtil.copyProperties(comment, vo);
            User author = userMap.get(comment.getUserId());
            if (author != null) {
                vo.setUser(userService.getUserVO(author));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private List<CommentVO> getAllCommentsFromDb(Long appId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("appId", appId)
                .orderBy("createTime", false);
        List<AppComment> comments = appCommentMapper.selectListByQuery(wrapper);

        if (CollUtil.isEmpty(comments)) {
            return new ArrayList<>();
        }

        Set<Long> userIds = comments.stream().map(AppComment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return comments.stream().map(comment -> {
            CommentVO vo = new CommentVO();
            BeanUtil.copyProperties(comment, vo);
            User author = userMap.get(comment.getUserId());
            if (author != null) {
                vo.setUser(userService.getUserVO(author));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<AppVO> getUserLikedApps(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID错误");

        QueryWrapper wrapper = QueryWrapper.create()
                .where("likeUserIds IS NOT NULL AND JSON_CONTAINS(likeUserIds, ?)", userId.toString());
        List<App> apps = appMapper.selectListByQuery(wrapper);
        
        if (CollUtil.isEmpty(apps)) {
            log.info("用户 {} 点赞列表为空", userId);
            return new ArrayList<>();
        }
        
        return appService.getAppVOList(apps);
    }

    @Override
    public List<AppVO> getUserCollectedApps(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID错误");

        QueryWrapper wrapper = QueryWrapper.create()
                .where("collectUserIds IS NOT NULL AND JSON_CONTAINS(collectUserIds, ?)", userId.toString());
        List<App> apps = appMapper.selectListByQuery(wrapper);
        
        if (CollUtil.isEmpty(apps)) {
            log.info("用户 {} 收藏列表为空", userId);
            return new ArrayList<>();
        }
        
        return appService.getAppVOList(apps);
    }
}
