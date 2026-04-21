package com.dzc.dzcaicodemother.model.vo;

import java.io.Serializable;
import java.util.Map;

import lombok.Data;

/**
 * 应用浏览统计视图对象
 *
 * @author dzc
 */
@Data
public class AppViewStatVO implements Serializable {

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 总浏览量
     */
    private Integer totalViewCount;

    /**
     * 今日浏览量
     */
    private Integer todayViewCount;

    /**
     * 昨日浏览量
     */
    private Integer yesterdayViewCount;

    /**
     * 平均日浏览量
     */
    private Integer avgViewCount;

    /**
     * 浏览趋势（日期 -> 浏览量）
     */
    private Map<String, Integer> viewTrend;

    private static final long serialVersionUID = 1L;
}
