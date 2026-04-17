package com.dzc.dzcaicodemother.utils;

import java.util.regex.Pattern;

import com.dzc.dzcaicodemother.exception.BusinessException;
import com.dzc.dzcaicodemother.exception.ErrorCode;

import cn.hutool.core.util.StrUtil;

/**
 * 参数校验工具类
 */
public class ValidationUtils {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    /**
     * 校验手机号格式
     *
     * @param phone 手机号
     */
    public static void validatePhone(String phone) {
        if (StrUtil.isNotBlank(phone)) {
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
            }
        }
    }

    /**
     * 校验邮箱格式
     *
     * @param email 邮箱
     */
    public static void validateEmail(String email) {
        if (StrUtil.isNotBlank(email)) {
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
            }
        }
    }
}
