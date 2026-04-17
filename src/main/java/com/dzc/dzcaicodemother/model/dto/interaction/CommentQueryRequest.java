package com.dzc.dzcaicodemother.model.dto.interaction;

import java.io.Serializable;

import lombok.Data;

@Data
public class CommentQueryRequest implements Serializable {

    private Long appId;

    private Long parentId;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
