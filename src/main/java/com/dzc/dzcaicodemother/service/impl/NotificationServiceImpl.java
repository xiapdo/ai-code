package com.dzc.dzcaicodemother.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.dzc.dzcaicodemother.exception.ErrorCode;
import com.dzc.dzcaicodemother.exception.ThrowUtils;
import com.dzc.dzcaicodemother.model.dto.notification.NotificationQueryRequest;
import com.dzc.dzcaicodemother.model.entity.App;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.model.enums.NotificationTypeEnum;
import com.dzc.dzcaicodemother.model.vo.UserNotificationVO;
import com.dzc.dzcaicodemother.service.AppService;
import com.dzc.dzcaicodemother.service.NotificationService;
import com.dzc.dzcaicodemother.service.UserService;
import com.dzc.dzcaicodemother.utils.NotificationCacheKeyUtils;

import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final Duration NOTIFICATION_TTL = Duration.ofDays(7);
    private static final int MAX_NOTIFICATIONS_PER_USER = 100;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;

    @Resource
    private SimpMessagingTemplate messagingTemplate;

    @Override
    @Async
    public void sendLikeNotification(Long appId, User actor, String appName) {
        App app = appService.getById(appId);
        if (app == null || app.getUserId().equals(actor.getId())) {
            return;
        }

        UserNotificationVO notification = buildNotification(app.getUserId(), NotificationTypeEnum.LIKE, appId, actor,
                String.format("%s 点赞了你的应用《%s》", actor.getUserName(), appName), appName);

        saveNotificationToRedis(app.getUserId(), notification);
        incrementUnreadCount(app.getUserId());
        pushNotification(app.getUserId(), notification);

        log.info("发送点赞通知: actor={}, target={}, appId={}", actor.getId(), app.getUserId(), appId);
    }

    @Override
    @Async
    public void sendCollectNotification(Long appId, User actor, String appName) {
        App app = appService.getById(appId);
        if (app == null || app.getUserId().equals(actor.getId())) {
            return;
        }

        UserNotificationVO notification = buildNotification(app.getUserId(), NotificationTypeEnum.COLLECT, appId, actor,
                String.format("%s 收藏了你的应用《%s》", actor.getUserName(), appName), appName);

        saveNotificationToRedis(app.getUserId(), notification);
        incrementUnreadCount(app.getUserId());
        pushNotification(app.getUserId(), notification);

        log.info("发送收藏通知: actor={}, target={}, appId={}", actor.getId(), app.getUserId(), appId);
    }

    @Override
    @Async
    public void sendCommentNotification(Long appId, User actor, String appName, String commentContent, Long commentId) {
        App app = appService.getById(appId);
        if (app == null || app.getUserId().equals(actor.getId())) {
            return;
        }

        String content = commentContent.length() > 50
                ? commentContent.substring(0, 50) + "..."
                : commentContent;

        UserNotificationVO notification = buildNotification(app.getUserId(), NotificationTypeEnum.COMMENT, appId, actor,
                String.format("%s 评论了你的应用《%s》: %s", actor.getUserName(), appName, content), appName);
        notification.setCommentId(commentId);

        saveNotificationToRedis(app.getUserId(), notification);
        incrementUnreadCount(app.getUserId());
        pushNotification(app.getUserId(), notification);

        log.info("发送评论通知: actor={}, target={}, appId={}, commentId={}", actor.getId(), app.getUserId(), appId, commentId);
    }

    @Override
    @Async
    public void sendReplyNotification(Long appId, Long commentOwnerUserId, User actor, String appName, String replyContent, Long commentId) {
        if (commentOwnerUserId == null || commentOwnerUserId.equals(actor.getId())) {
            return;
        }

        UserNotificationVO notification = buildNotification(commentOwnerUserId, NotificationTypeEnum.REPLY, appId, actor,
                String.format("%s 回复了你: %s", actor.getUserName(), replyContent), appName);
        notification.setCommentId(commentId);

        saveNotificationToRedis(commentOwnerUserId, notification);
        incrementUnreadCount(commentOwnerUserId);
        pushNotification(commentOwnerUserId, notification);

        log.info("发送回复通知: actor={}, target={}, commentId={}, appId={}", actor.getId(), commentOwnerUserId, commentId, appId);
    }

    @Override
    public List<UserNotificationVO> getNotificationList(NotificationQueryRequest request, User user) {
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);

        RList<String> notificationList = redissonClient.getList(NotificationCacheKeyUtils.getNotificationListKey(user.getId()));
        List<String> jsonList = notificationList.range(0, -1);

        if (jsonList.isEmpty()) {
            return new ArrayList<>();
        }

        List<UserNotificationVO> notifications = jsonList.stream()
                .map(json -> JSONUtil.toBean(json, UserNotificationVO.class))
                .collect(Collectors.toList());

        if (request.getUnreadOnly() != null && request.getUnreadOnly()) {
            notifications = notifications.stream()
                    .filter(n -> !Boolean.TRUE.equals(n.getIsRead()))
                    .collect(Collectors.toList());
        }

        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, notifications.size());

        if (start >= notifications.size()) {
            return new ArrayList<>();
        }

        return notifications.subList(start, end);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        User user = userService.getById(userId);
        if (user == null || user.getUnreadNotificationCount() == null) {
            return 0L;
        }
        return (long) user.getUnreadNotificationCount();
    }

    @Override
    public void markAsRead(Long notificationId, User user) {
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);

        RList<String> notificationList = redissonClient.getList(NotificationCacheKeyUtils.getNotificationListKey(user.getId()));
        List<String> jsonList = notificationList.range(0, -1);

        for (int i = 0; i < jsonList.size(); i++) {
            UserNotificationVO notification = JSONUtil.toBean(jsonList.get(i), UserNotificationVO.class);
            if (notification.getId().equals(notificationId) && !Boolean.TRUE.equals(notification.getIsRead())) {
                notification.setIsRead(true);
                notificationList.set(i, JSONUtil.toJsonStr(notification));
                decrementUnreadCount(user.getId());
                log.info("标记通知已读: notificationId={}, userId={}", notificationId, user.getId());
                return;
            }
        }
    }

    @Override
    public void markAllAsRead(User user) {
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);

        RList<String> notificationList = redissonClient.getList(NotificationCacheKeyUtils.getNotificationListKey(user.getId()));
        List<String> jsonList = notificationList.range(0, -1);

        for (int i = 0; i < jsonList.size(); i++) {
            UserNotificationVO notification = JSONUtil.toBean(jsonList.get(i), UserNotificationVO.class);
            if (!Boolean.TRUE.equals(notification.getIsRead())) {
                notification.setIsRead(true);
                jsonList.set(i, JSONUtil.toJsonStr(notification));
            }
        }

        notificationList.clear();
        notificationList.addAll(jsonList);
        notificationList.expire(NOTIFICATION_TTL.toMillis(), TimeUnit.MILLISECONDS);

        resetUnreadCount(user.getId());
        log.info("标记所有通知已读: userId={}", user.getId());
    }

    @Override
    public void clearAllNotifications(User user) {
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);

        RList<String> notificationList = redissonClient.getList(NotificationCacheKeyUtils.getNotificationListKey(user.getId()));
        notificationList.clear();
        resetUnreadCount(user.getId());
        log.info("清除所有通知: userId={}", user.getId());
    }

    private UserNotificationVO buildNotification(Long targetUserId, NotificationTypeEnum type, Long appId, User actor, String content, String appName) {
        UserNotificationVO vo = new UserNotificationVO();
        vo.setId(System.currentTimeMillis());
        vo.setUserId(targetUserId);
        vo.setType(type.getValue());
        vo.setAppId(appId);
        vo.setAppName(appName);
        vo.setActorUserId(actor.getId());
        vo.setActorUserName(actor.getUserName());
        vo.setActorUserAvatar(actor.getUserAvatar());
        vo.setContent(content);
        vo.setIsRead(false);
        vo.setCreateTime(LocalDateTime.now());
        return vo;
    }

    private void saveNotificationToRedis(Long userId, UserNotificationVO notification) {
        RList<String> notificationList = redissonClient.getList(NotificationCacheKeyUtils.getNotificationListKey(userId));
        notificationList.add(0, JSONUtil.toJsonStr(notification));

        while (notificationList.size() > MAX_NOTIFICATIONS_PER_USER) {
            notificationList.remove(notificationList.size() - 1);
        }

        notificationList.expire(NOTIFICATION_TTL.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void incrementUnreadCount(Long userId) {
        User user = userService.getById(userId);
        if (user != null) {
            int currentCount = user.getUnreadNotificationCount() != null ? user.getUnreadNotificationCount() : 0;
            user.setUnreadNotificationCount(currentCount + 1);
            userService.updateById(user);
        }
    }

    private void decrementUnreadCount(Long userId) {
        User user = userService.getById(userId);
        if (user != null && user.getUnreadNotificationCount() != null && user.getUnreadNotificationCount() > 0) {
            user.setUnreadNotificationCount(user.getUnreadNotificationCount() - 1);
            userService.updateById(user);
        }
    }

    private void resetUnreadCount(Long userId) {
        User user = userService.getById(userId);
        if (user != null) {
            user.setUnreadNotificationCount(0);
            userService.updateById(user);
        }
    }

    private void pushNotification(Long userId, UserNotificationVO notification) {
        try {
            String destination = "/topic/notification/" + userId;
            messagingTemplate.convertAndSend(destination, notification);
            log.info("WebSocket通知推送成功: destination={}", destination);
        } catch (Exception e) {
            log.error("WebSocket通知推送失败: userId={}, error={}", userId, e.getMessage());
        }
    }
}
