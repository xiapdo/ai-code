package com.dzc.dzcaicodemother.model.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 群聊消息VO（包含发送者信息）
 *
 * @author dzc
 */
@Data
public class ChatRoomMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long roomId;

    private Long userId;

    private String messageContent;

    private String messageType;

    private String fileUrl;

    private Long replyToId;

    private Integer isRevoked;

    private LocalDateTime revokedTime;

    private LocalDateTime createTime;

    private String userName;

    private String userAvatar;
}
