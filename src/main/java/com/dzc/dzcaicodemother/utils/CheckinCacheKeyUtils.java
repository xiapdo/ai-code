package com.dzc.dzcaicodemother.utils;

/**
 * 签到缓存 Key 工具类
 *
 * @author dzc
 */
public class CheckinCacheKeyUtils {

    /**
     * 用户签到记录缓存前缀（存储签到日期集合）
     */
    private static final String USER_CHECKIN_KEY_PREFIX = "user:checkin:";

    private CheckinCacheKeyUtils() {
    }

    /**
     * 获取用户签到记录缓存 Key
     *
     * @param userId 用户 ID
     * @return 缓存 Key
     */
    public static String getCheckinKey(Long userId) {
        return USER_CHECKIN_KEY_PREFIX + userId;
    }
}
