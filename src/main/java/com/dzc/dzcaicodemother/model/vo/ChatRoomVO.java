package com.dzc.dzcaicodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 群聊VO（包含群主信息，用于管理端展示）
 *
 * @author dzc
 */
@Data
public class ChatRoomVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String roomName;

    private String roomAvatar;

    private String description;

    private String inviteCode;

    private LocalDateTime inviteExpiryTime;

    private Long ownerUserId;

    private Integer memberCount;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private UserVO owner;
}
