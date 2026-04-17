package com.dzc.dzcaicodemother.model.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 群聊成员VO（包含用户信息）
 *
 * @author dzc
 */
@Data
public class ChatRoomMemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long roomId;

    private Long userId;

    private String memberRole;

    private LocalDateTime joinTime;

    private LocalDateTime lastActiveTime;

    private Integer isMuted;

    private Integer onlineStatus;

    private String userName;

    private String userAvatar;

    private String userProfile;
}
