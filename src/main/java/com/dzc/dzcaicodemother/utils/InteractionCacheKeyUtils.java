package com.dzc.dzcaicodemother.utils;

public class InteractionCacheKeyUtils {

    private static final String APP_LIKE_KEY_PREFIX = "app:like:";
    private static final String APP_COLLECT_KEY_PREFIX = "app:collect:";
    private static final String APP_COMMENT_KEY_PREFIX = "app:comment:";
    private static final String USER_LIKE_KEY_PREFIX = "user:like:";
    private static final String USER_COLLECT_KEY_PREFIX = "user:collect:";

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
}
