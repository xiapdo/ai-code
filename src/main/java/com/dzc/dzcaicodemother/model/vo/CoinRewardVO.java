package com.dzc.dzcaicodemother.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 金币奖励视图对象
 *
 * @author dzc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinRewardVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 本次获得的金币数
     */
    private Integer earnedCoins;

    /**
     * 当前总金币数
     */
    private Integer totalCoins;

    /**
     * 是否需要兑换（金币达到 7 枚）
     */
    private Boolean needExchange;

    /**
     * 兑换后获得的群聊名额
     */
    private Integer exchangedQuota;

    /**
     * 兑换后的剩余金币数
     */
    private Integer remainingCoins;
}
