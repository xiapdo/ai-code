package com.dzc.dzcaicodemother.model.dto.interaction;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentAddRequest implements Serializable {

    @NotNull(message = "应用ID不能为空")
    private Long appId;

    @NotNull(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过500字符")
    private String content;

    private Long replyToId;

    private Long parentId;
}
