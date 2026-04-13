package com.dzc.dzcaicodemother.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息撤回DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageRevokeDTO {
    private Long messageId;
    private Long revokedBy;
}
