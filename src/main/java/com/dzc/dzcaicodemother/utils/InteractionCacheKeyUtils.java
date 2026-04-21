package com.dzc.dzcaicodemother.utils;

public class InteractionCacheKeyUtils {

    private static final String APP_LIKE_KEY_PREFIX = "app:like:";
    private static final String APP_COLLECT_KEY_PREFIX = "app:collect:";
    private static final String APP_COMMENT_KEY_PREFIX = "app:comment:";
    private static final String USER_LIKE_KEY_PREFIX = "user:like:";
    private static final String USER_COLLECT_KEY_PREFIX = "user:collect:";
    private static final String APP_VIEW_KEY_PREFIX = "app:view:";
    private static final String APP_VIEW_STAT_KEY_PREFIX = "app:view:stat:";
    private static final String APP_VIEW_STAT_CACHE_KEY_PREFIX = "app:view:stat:cache:";
    private static final String USER_VIEW_KEY_PREFIX = "user:view:";

    private InteractionCacheKeyUtils() {
    }

    public static String getLikeKey(Long appId) {
        return APP_LIKE_KEY_PREFIX + appId;
    }

    public static String getCollectKey(Long appId) {
        return APP_COLLECT_KEY_PREFIX + appId;
    }

    public static String getCommentKey(Long appId) {
        return APP_COMMENT_KEY_PREFIX + appId;
    }

    public static String getUserLikeKey(Long userId) {
        return USER_LIKE_KEY_PREFIX + userId;
    }

    public static String getUserCollectKey(Long userId) {
        return USER_COLLECT_KEY_PREFIX + userId;
    }

    /**
     * 获取应用浏览记录缓存键
     * 格式：app:view:{appId}:{date}
     *
     * @param appId 应用ID
     * @param date  日期字符串（格式：yyyy-MM-dd）
     * @return 缓存键
     */
    public static String getAppViewKey(Long appId, String date) {
        return APP_VIEW_KEY_PREFIX + appId + ":" + date;
    }

    /**
     * 获取应用浏览统计缓存键（用于定时任务同步）
     * 格式：app:view:stat:{appId}:{date}
     *
     * @param appId 应用ID
     * @param date  日期字符串（格式：yyyy-MM-dd）
     * @return 缓存键
     */
    public static String getAppViewStatKey(Long appId, String date) {
        return APP_VIEW_STAT_KEY_PREFIX + appId + ":" + date;
    }

    /**
     * 获取应用浏览统计查询缓存键（用于5分钟缓存）
     * 格式：app:view:stat:cache:{appId}
     *
     * @param appId 应用ID
     * @return 缓存键
     */
    public static String getAppViewStatCacheKey(Long appId) {
        return APP_VIEW_STAT_CACHE_KEY_PREFIX + appId;
    }

    /**
     * 获取用户浏览记录缓存键
     * 格式：user:view:{userId}:{date}
     *
     * @param userId 用户ID
     * @param date   日期字符串（格式：yyyy-MM-dd）
     * @return 缓存键
     */
    public static String getUserViewKey(Long userId, String date) {
        return USER_VIEW_KEY_PREFIX + userId + ":" + date;
    }
}
