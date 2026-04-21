package com.dzc.dzcaicodemother.model.dto.app;

import java.io.Serializable;

import lombok.Data;

/**
 * 更新代码文件请求
 */
@Data
public class CodeFileUpdateRequest implements Serializable {

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 文件路径（相对路径，如 src/App.vue）
     */
    private String filePath;

    /**
     * 文件内容
     */
    private String content;
}
