package com.dzc.dzcaicodemother.model.dto.chat;

import lombok.Data;
import java.io.Serializable;

/**
 * 创建群聊请求
 *
 * @author dzc
 */
@Data
public class ChatRoomCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 群聊名称 */
    private String roomName;

    /** 群聊描述 */
    private String description;

    /** 搜索词 */
    private String searchText;
}
