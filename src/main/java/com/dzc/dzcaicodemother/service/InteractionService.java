package com.dzc.dzcaicodemother.service;

import java.util.List;

import com.dzc.dzcaicodemother.model.dto.interaction.CommentAddRequest;
import com.dzc.dzcaicodemother.model.dto.interaction.CommentQueryRequest;
import com.dzc.dzcaicodemother.model.entity.User;
import com.dzc.dzcaicodemother.model.vo.AppVO;
import com.dzc.dzcaicodemother.model.vo.CommentVO;
import com.dzc.dzcaicodemother.model.vo.InteractionStatusVO;

public interface InteractionService {

    InteractionStatusVO getInteractionStatus(Long appId, Long userId);

    boolean toggleLike(Long appId, User user);

    boolean toggleCollect(Long appId, User user);

    CommentVO addComment(CommentAddRequest request, User user);

    List<CommentVO> getCommentList(CommentQueryRequest request);

    /**
     * 获取用户点赞的应用列表
     *
     * @param userId 用户ID
     * @return 应用列表
     */
    List<AppVO> getUserLikedApps(Long userId);

    /**
     * 获取用户收藏的应用列表
     *
     * @param userId 用户ID
     * @return 应用列表
     */
    List<AppVO> getUserCollectedApps(Long userId);
}
