package com.dzc.dzcaicodemother.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 签到状态视图对象
 *
 * @author dzc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinStatusVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 今日是否已签到
     */
    private Boolean checkedToday;

    /**
     * 连续签到天数
     */
    private Integer continuousDays;

    /**
     * 总签到天数
     */
    private Integer totalDays;

    /**
     * 最近 7 天签到记录
     */
    private List<CheckinRecordVO> recentRecords;

    /**
     * 金币奖励信息
     */
    private CoinRewardVO coinReward;
}
