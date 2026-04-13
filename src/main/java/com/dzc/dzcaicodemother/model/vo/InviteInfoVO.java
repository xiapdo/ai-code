package com.dzc.dzcaicodemother.model.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 邀请码信息VO
 *
 * @author dzc
 */
@Data
public class InviteInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 邀请码 */
    private String inviteCode;

    /** 是否有效 */
    private Boolean valid;

    /** 有效期（分钟） */
    private Integer expiryMinutes;

    /** 群聊ID */
    private Long roomId;

    /** 群聊名称 */
    private String roomName;

    /** 群主名称 */
    private String ownerName;

    /** 群主用户ID */
    private Long ownerUserId;

    /** 当前成员数量 */
    private Integer memberCount;

    /** 群描述 */
    private String description;
}
