package com.dzc.dzcaicodemother.model.vo;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 签到记录视图对象
 *
 * @author dzc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 签到日期（格式：yyyy-MM-dd）
     */
    private String date;

    /**
     * 是否已签到
     */
    private Boolean checked;

    /**
     * 星期几（格式：周一、周二...）
     */
    private String dayOfWeek;
}
