package com.dzc.dzcaicodemother.utils;

public class NotificationCacheKeyUtils {

    private static final String USER_NOTIFICATION_COUNT_KEY_PREFIX = "notification:count:";
    private static final String USER_NOTIFICATION_LIST_KEY_PREFIX = "notification:list:";

    private NotificationCacheKeyUtils() {
    }

    public static String getNotificationCountKey(Long userId) {
        return USER_NOTIFICATION_COUNT_KEY_PREFIX + userId;
    }

    public static String getNotificationListKey(Long userId) {
        return USER_NOTIFICATION_LIST_KEY_PREFIX + userId;
    }
}
