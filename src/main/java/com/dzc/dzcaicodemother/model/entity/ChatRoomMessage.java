package com.dzc.dzcaicodemother.model.entity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 群聊消息表
 *
 * @author dzc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_room_message")
public class ChatRoomMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 消息ID */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /** 群聊ID */
    @Column("roomId")
    private Long roomId;

    /** 发送者ID(null表示系统/AI消息) */
    @Column("userId")
    private Long userId;

    /** 消息内容 */
    @Column("messageContent")
    private String messageContent;

    /** 消息类型：text/image/file/ai_start/ai_chunk/ai_complete/system */
    @Column("messageType")
    private String messageType;

    /** 文件URL（图片或文件） */
    @Column("fileUrl")
    private String fileUrl;

    /** 回复消息ID */
    @Column("replyToId")
    private Long replyToId;

    /** 是否已撤回 */
    @Column("isRevoked")
    private Integer isRevoked;

    /** 撤回时间 */
    @Column("revokedTime")
    private LocalDateTime revokedTime;

    /** 发送时间 */
    @Column(value = "createTime", onInsertValue = "now()")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(value = "updateTime", onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
