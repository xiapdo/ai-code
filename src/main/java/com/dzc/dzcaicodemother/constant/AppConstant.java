package com.dzc.dzcaicodemother.constant;

import java.util.Arrays;
import java.util.List;

/**
 * 应用常量
 */
public interface AppConstant {

    /**
     * 精选应用的优先级
     */
    Integer GOOD_APP_PRIORITY = 99;

    /**
     * 默认应用优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;

    /**
     * 应用生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";

    /**
     * 应用部署域名
     */
    String CODE_DEPLOY_HOST = "http://localhost";

    /**
     * 精选作品分类选项列表
     */
    List<String> CATEGORY_OPTIONS = Arrays.asList(
            "全部分类", "网站", "工具", "数据分析",
            "活动页面", "管理平台", "用户应用", "个人管理"
    );

    /**
     * 精选作品排序选项列表
     */
    List<String> SORT_OPTIONS = Arrays.asList(
            "默认排序", "最新", "最热"
    );
}