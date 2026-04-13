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
 * 群聊表
 *
 * @author dzc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_room")
public class ChatRoom implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 群聊ID */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /** 群聊名称 */
    @Column("roomName")
    private String roomName;

    /** 群聊头像URL */
    @Column("roomAvatar")
    private String roomAvatar;

    /** 群聊描述 */
    @Column("description")
    private String description;

    /** 邀请码（24位UUID） */
    @Column("inviteCode")
    private String inviteCode;

    /** 邀请码过期时间 */
    @Column("inviteExpiryTime")
    private LocalDateTime inviteExpiryTime;

    /** 创建者ID */
    @Column("ownerUserId")
    private Long ownerUserId;

    /** 当前成员数 */
    @Column("memberCount")
    private Integer memberCount;

    /** 状态：1-正常 2-解散 */
    @Column("status")
    private Integer status;

    /** 逻辑删除标记 */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

    /** 创建时间 */
    @Column(value = "createTime", onInsertValue = "now()")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(value = "updateTime", onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;
}
