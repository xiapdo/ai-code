package com.dzc.dzcaicodemother.model.vo;

import java.io.Serializable;

import lombok.Data;

@Data
public class InteractionStatusVO implements Serializable {

    private Long appId;

    private Integer likeCount;

    private Integer collectCount;

    private Integer commentCount;

    private Boolean isLiked;

    private Boolean isCollected;
}
