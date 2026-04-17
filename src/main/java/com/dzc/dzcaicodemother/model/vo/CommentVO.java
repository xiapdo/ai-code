package com.dzc.dzcaicodemother.model.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CommentVO implements Serializable {

    private Long id;

    private Long appId;

    private Long userId;

    private String content;

    private Long replyToId;

    private Long parentId;

    private Integer likeCount;

    private LocalDateTime createTime;

    private UserVO user;
}
