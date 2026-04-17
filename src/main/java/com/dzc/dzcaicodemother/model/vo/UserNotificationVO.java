package com.dzc.dzcaicodemother.model.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserNotificationVO implements Serializable {

    private Long id;

    private Long userId;

    private String type;

    private Long appId;

    private String appName;

    private Long actorUserId;

    private String actorUserName;

    private String actorUserAvatar;

    private String content;

    private Boolean isRead;

    private Long commentId;

    private LocalDateTime createTime;
}
