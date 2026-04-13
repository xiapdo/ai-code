package com.dzc.dzcaicodemother.model.dto.chat;

import lombok.Data;
import java.io.Serializable;

/**
 * 更新群聊请求
 *
 * @author dzc
 */
@Data
public class ChatRoomUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 群聊ID */
    private Long id;

    /** 群聊名称 */
    private String roomName;

    /** 群聊头像 */
    private String roomAvatar;

    /** 群聊描述 */
    private String description;
}
