package com.dzc.dzcaicodemother.model.dto.chat;

import lombok.Data;
import java.io.Serializable;

/**
 * 加入群聊请求
 *
 * @author dzc
 */
@Data
public class ChatInviteJoinRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 邀请码 */
    private String inviteCode;
}
