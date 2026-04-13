package com.dzc.dzcaicodemother.websocket.dto;

import lombok.Data;

/**
 * 历史消息请求
 */
@Data
public class HistoryRequest {
    private int count = 50; // 默认50条
}
