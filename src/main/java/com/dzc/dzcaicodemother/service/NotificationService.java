package com.dzc.dzcaicodemother.service;

import java.util.List;

import com.dzc.dzcaicodemother.model.dto.notification.NotificationQueryRequest;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.model.vo.UserNotificationVO;

public interface NotificationService {

    void sendLikeNotification(Long appId, User actor, String appName);

    void sendCollectNotification(Long appId, User actor, String appName);

    void sendCommentNotification(Long appId, User actor, String appName, String commentContent, Long commentId);

    void sendReplyNotification(Long appId, Long commentOwnerUserId, User actor, String appName, String replyContent, Long commentId);

    List<UserNotificationVO> getNotificationList(NotificationQueryRequest request, User user);

    Long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, User user);

    void markAllAsRead(User user);

    void clearAllNotifications(User user);
}
