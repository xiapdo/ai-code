package com.dzc.dzcaicodemother.model.dto.chat;

import com.dzc.dzcaicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;

/**
 * 查询群聊请求
 *
 * @author dzc
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatRoomQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 群聊ID */
    private Long id;

    /** 群聊名称 */
    private String roomName;

    /** 群主ID */
    private Long ownerUserId;

    /** 状态：1-正常 2-解散 */
    private Integer status;
}
