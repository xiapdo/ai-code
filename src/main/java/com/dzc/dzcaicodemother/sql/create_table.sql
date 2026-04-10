# 数据库初始化
# @author <a href="https://github.com/liyupi">程序员鱼皮</a>
# @from <a href="https://codefather.cn">编程导航学习圈</a>

-- 创建库
create database if not exists dzc_code_mother;

-- 切换库
use dzc_code_mother;

-- 用户表
-- 以下是建表语句

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 应用表
create table app
(
    id           bigint auto_increment comment 'id' primary key,
    appName      varchar(256)                       null comment '应用名称',
    cover        varchar(512)                       null comment '应用封面',
    initPrompt   text                               null comment '应用初始化的 prompt',
    codeGenType  varchar(64)                        null comment '代码生成类型（枚举）',
    deployKey    varchar(64)                        null comment '部署标识',
    deployedTime datetime                           null comment '部署时间',
    priority     int      default 0                 not null comment '优先级',
    userId       bigint                             not null comment '创建用户id',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uk_deployKey (deployKey), -- 确保部署标识唯一
    INDEX idx_appName (appName),         -- 提升基于应用名称的查询性能
    INDEX idx_userId (userId)            -- 提升基于用户 ID 的查询性能
) comment '应用' collate = utf8mb4_unicode_ci;

ALTER TABLE `app` ADD COLUMN `versionNo` INT DEFAULT 0 COMMENT '当前版本号';
ALTER TABLE `app` ADD COLUMN `currentVersionId` BIGINT DEFAULT NULL COMMENT '当前版本快照ID';

-- 对话历史表
create table chat_history
(
    id          bigint auto_increment comment 'id' primary key,
    message     mediumtext                             not null comment '消息',
    messageType varchar(32)                        not null comment 'user/ai',
    appId       bigint                             not null comment '应用id',
    userId      bigint                             not null comment '创建用户id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    INDEX idx_appId (appId),                       -- 提升基于应用的查询性能
    INDEX idx_createTime (createTime),             -- 提升基于时间的查询性能
    INDEX idx_appId_createTime (appId, createTime) -- 游标查询核心索引
) comment '对话历史' collate = utf8mb4_unicode_ci;

-- auto-generated definition
create table chat_history_original
(
    id          bigint auto_increment comment 'id'
        primary key,
    message     mediumtext                               not null comment '消息',
    messageType varchar(32)                        not null comment 'user/ai/toolExecutionRequest/toolExecutionResult',
    appId       bigint                             not null comment '应用id',
    userId      bigint                             not null comment '创建用户id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除'
)
    comment '对话历史' collate = utf8mb4_unicode_ci;

create index idx_appId
    on chat_history_original (appId);

create index idx_appId_createTime
    on chat_history_original (appId, createTime);

create index idx_createTime
    on chat_history_original (createTime);


CREATE TABLE `app_version_snapshot`
(
    `id` bigint NOT NULL AUTO_INCREMENT,
    `appId` bigint NOT NULL COMMENT '应用ID',
    `versionNo` int NOT NULL COMMENT '版本号',
    `snapshotData` json NOT NULL COMMENT '版本快照数据(JSON格式)',
    `thumbnail` varchar(512) DEFAULT NULL COMMENT '版本缩略图URL',
    `changeSummary` varchar(500) DEFAULT NULL COMMENT '变更摘要',
    `chatHistoryId` bigint DEFAULT NULL COMMENT '对应的对话记录ID',
    `userId` bigint NOT NULL COMMENT '创建者ID',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_appId` (`appId`),
    KEY `idx_versionNo` (`versionNo`),
    KEY `idx_userId` (`userId`)
) COMMENT='应用版本快照表' collate = utf8mb4_unicode_ci;
