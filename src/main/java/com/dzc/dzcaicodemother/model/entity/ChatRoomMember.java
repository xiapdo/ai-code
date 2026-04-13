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
 * 群聊成员表
 *
 * @author dzc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_room_member")
public class ChatRoomMember implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /** 群聊ID */
    @Column("roomId")
    private Long roomId;

    /** 用户ID */
    @Column("userId")
    private Long userId;

    /** 角色：owner-群主/member-成员 */
    @Column("memberRole")
    private String memberRole;

    /** 加入时间 */
    @Column("joinTime")
    private LocalDateTime joinTime;

    /** 最后已读时间 */
    @Column("lastReadTime")
    private LocalDateTime lastReadTime;

    /** 最后活跃时间 */
    @Column("lastActiveTime")
    private LocalDateTime lastActiveTime;

    /** 是否被禁言 */
    @Column("isMuted")
    private Integer isMuted;

    /** 在线状态：0-离线 1-在线 */
    @Column("onlineStatus")
    private Integer onlineStatus;

    /** 逻辑删除标记 */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
