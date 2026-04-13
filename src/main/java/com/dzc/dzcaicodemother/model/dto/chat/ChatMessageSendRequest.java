package com.dzc.dzcaicodemother.model.dto.chat;

import lombok.Data;
import java.io.Serializable;

/**
 * 发送消息请求
 *
 * @author dzc
 */
@Data
public class ChatMessageSendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 群聊ID */
    private Long roomId;

    /** 消息内容 */
    private String messageContent;

    /** 消息类型：text/image/file/ai */
    private String messageType;

    /** 文件URL（图片或文件） */
    private String fileUrl;

    /** 回复消息ID */
    private Long replyToId;
}
