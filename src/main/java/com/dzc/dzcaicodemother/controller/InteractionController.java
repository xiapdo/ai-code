package com.dzc.dzcaicodemother.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dzc.dzcaicodemother.common.BaseResponse;
import com.dzc.dzcaicodemother.common.ResultUtils;
import com.dzc.dzcaicodemother.exception.ThrowUtils;
import com.dzc.dzcaicodemother.model.dto.interaction.CommentAddRequest;
import com.dzc.dzcaicodemother.model.dto.interaction.CommentQueryRequest;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.model.vo.AppVO;
import com.dzc.dzcaicodemother.model.vo.CommentVO;
import com.dzc.dzcaicodemother.model.vo.InteractionStatusVO;
import com.dzc.dzcaicodemother.ratelimter.annotation.RateLimit;
import com.dzc.dzcaicodemother.ratelimter.enums.RateLimitType;
import com.dzc.dzcaicodemother.service.InteractionService;
import com.dzc.dzcaicodemother.service.UserService;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/interaction")
@Slf4j
public class InteractionController {

    @Resource
    private InteractionService interactionService;

    @Resource
    private UserService userService;

    @GetMapping("/status")
    public BaseResponse<InteractionStatusVO> getInteractionStatus(@RequestParam Long appId,
                                                                  HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, com.dzc.dzcaicodemother.exception.ErrorCode.PARAMS_ERROR, "应用ID错误");
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser != null ? loginUser.getId() : null;
        InteractionStatusVO status = interactionService.getInteractionStatus(appId, userId);
        return ResultUtils.success(status);
    }

    @PostMapping("/like")
    @RateLimit(limitType = RateLimitType.USER, rate = 20, rateInterval = 60)
    public BaseResponse<Boolean> toggleLike(@RequestParam Long appId,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, com.dzc.dzcaicodemother.exception.ErrorCode.PARAMS_ERROR, "应用ID错误");
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, com.dzc.dzcaicodemother.exception.ErrorCode.NOT_LOGIN_ERROR);
        boolean result = interactionService.toggleLike(appId, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/collect")
    @RateLimit(limitType = RateLimitType.USER, rate = 20, rateInterval = 60)
    public BaseResponse<Boolean> toggleCollect(@RequestParam Long appId,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, com.dzc.dzcaicodemother.exception.ErrorCode.PARAMS_ERROR, "应用ID错误");
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, com.dzc.dzcaicodemother.exception.ErrorCode.NOT_LOGIN_ERROR);
        boolean result = interactionService.toggleCollect(appId, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/comment/add")
    @RateLimit(limitType = RateLimitType.USER, rate = 10, rateInterval = 60)
    public BaseResponse<CommentVO> addComment(@Valid @RequestBody CommentAddRequest commentRequest,
                                               HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, com.dzc.dzcaicodemother.exception.ErrorCode.NOT_LOGIN_ERROR);
        CommentVO comment = interactionService.addComment(commentRequest, loginUser);
        return ResultUtils.success(comment);
    }

    @GetMapping("/comment/list")
    public BaseResponse<List<CommentVO>> getCommentList(CommentQueryRequest queryRequest) {
        List<CommentVO> commentList = interactionService.getCommentList(queryRequest);
        return ResultUtils.success(commentList);
    }

    /**
     * 获取用户点赞的应用列表
     */
    @GetMapping("/liked")
    public BaseResponse<List<AppVO>> getUserLikedApps(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, com.dzc.dzcaicodemother.exception.ErrorCode.NOT_LOGIN_ERROR);
        List<AppVO> apps = interactionService.getUserLikedApps(loginUser.getId());
        return ResultUtils.success(apps);
    }

    /**
     * 获取用户收藏的应用列表
     */
    @GetMapping("/collected")
    public BaseResponse<List<AppVO>> getUserCollectedApps(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, com.dzc.dzcaicodemother.exception.ErrorCode.NOT_LOGIN_ERROR);
        List<AppVO> apps = interactionService.getUserCollectedApps(loginUser.getId());
        return ResultUtils.success(apps);
    }
}
