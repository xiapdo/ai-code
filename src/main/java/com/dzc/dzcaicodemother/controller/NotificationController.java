package com.dzc.dzcaicodemother.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dzc.dzcaicodemother.common.BaseResponse;
import com.dzc.dzcaicodemother.common.ResultUtils;
import com.dzc.dzcaicodemother.exception.ThrowUtils;
import com.dzc.dzcaicodemother.model.dto.notification.NotificationQueryRequest;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.model.vo.UserNotificationVO;
import com.dzc.dzcaicodemother.service.NotificationService;
import com.dzc.dzcaicodemother.service.UserService;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/notification")
@Slf4j
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @Resource
    private UserService userService;

    @GetMapping("/list")
    public BaseResponse<List<UserNotificationVO>> getNotificationList(
            NotificationQueryRequest request,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        ThrowUtils.throwIf(loginUser == null, com.dzc.dzcaicodemother.exception.ErrorCode.NOT_LOGIN_ERROR);
        List<UserNotificationVO> list = notificationService.getNotificationList(request, loginUser);
        return ResultUtils.success(list);
    }

    @GetMapping("/unread/count")
    public BaseResponse<Long> getUnreadCount(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, com.dzc.dzcaicodemother.exception.ErrorCode.NOT_LOGIN_ERROR);
        Long count = notificationService.getUnreadCount(loginUser.getId());
        return ResultUtils.success(count);
    }

    @PostMapping("/read")
    public BaseResponse<Boolean> markAsRead(@RequestParam Long notificationId,
                                            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, com.dzc.dzcaicodemother.exception.ErrorCode.NOT_LOGIN_ERROR);
        notificationService.markAsRead(notificationId, loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/read/all")
    public BaseResponse<Boolean> markAllAsRead(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, com.dzc.dzcaicodemother.exception.ErrorCode.NOT_LOGIN_ERROR);
        notificationService.markAllAsRead(loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/clear/all")
    public BaseResponse<Boolean> clearAllNotifications(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, com.dzc.dzcaicodemother.exception.ErrorCode.NOT_LOGIN_ERROR);
        notificationService.clearAllNotifications(loginUser);
        return ResultUtils.success(true);
    }
}
