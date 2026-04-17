# 数据库初始化

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

-- 群聊表
CREATE TABLE IF NOT EXISTS chat_room (
     id BIGINT AUTO_INCREMENT COMMENT '群聊ID' PRIMARY KEY,
     roomName VARCHAR(256) NOT NULL COMMENT '群聊名称',
     roomAvatar VARCHAR(512) COMMENT '群聊头像URL',
     description VARCHAR(512) COMMENT '群聊描述',
     inviteCode VARCHAR(24) UNIQUE COMMENT '邀请码（24位UUID）',
     inviteExpiryTime DATETIME COMMENT '邀请码过期时间',
     ownerUserId BIGINT NOT NULL COMMENT '创建者ID',
     memberCount INT DEFAULT 0 NOT NULL COMMENT '当前成员数',
     status TINYINT DEFAULT 1 NOT NULL COMMENT '1-正常 2-解散',
     isDeleted TINYINT DEFAULT 0 NOT NULL COMMENT '逻辑删除标记',
     createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
     updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

     INDEX idx_owner_user_id (ownerUserId),
     INDEX idx_status (status),
     INDEX idx_invite_code (inviteCode),
     INDEX idx_create_time (createTime)
) COMMENT '群聊表' COLLATE utf8mb4_unicode_ci;

ALTER table chat_room_message CHANGE  COLUMN  isDeleted isDelete TINYINT;

ALTER TABLE chat_room_member
    ADD COLUMN lastReadTime DATETIME NULL COMMENT '最后阅读时间',
    ADD COLUMN lastActiveTime DATETIME NULL COMMENT '最后活跃时间';
-- 为 chat_room_message 表添加 update_time 字段
ALTER TABLE `chat_room_message`
ADD COLUMN `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
-- 2. 群聊成员表
CREATE TABLE IF NOT EXISTS chat_room_member (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    roomId BIGINT NOT NULL COMMENT '群聊ID',
    userId BIGINT NOT NULL COMMENT '用户ID',
    memberRole VARCHAR(32) DEFAULT 'member' NOT NULL COMMENT '角色：owner-群主/member-成员',
    joinTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '加入时间',
    isMuted TINYINT DEFAULT 0 NOT NULL COMMENT '是否被禁言',
    onlineStatus TINYINT DEFAULT 0 NOT NULL COMMENT '在线状态：0-离线 1-在线',
    isDeleted TINYINT DEFAULT 0 NOT NULL COMMENT '逻辑删除标记',

    UNIQUE KEY uk_room_user (roomId, userId),
    INDEX idx_room_id (roomId),
    INDEX idx_user_id (userId)
) COMMENT '群聊成员表' COLLATE utf8mb4_unicode_ci;

-- 3. 群聊消息表
CREATE TABLE IF NOT EXISTS chat_room_message (
     id BIGINT AUTO_INCREMENT COMMENT '消息ID' PRIMARY KEY,
     roomId BIGINT NOT NULL COMMENT '群聊ID',
     userId BIGINT COMMENT '发送者ID(null表示系统/AI消息)',
     messageContent TEXT NOT NULL COMMENT '消息内容',
     messageType VARCHAR(32) DEFAULT 'text' NOT NULL COMMENT 'text文本/image图片/file文件/ai_start/ai_chunk/ai_complete/system',
     fileUrl VARCHAR(512) COMMENT '文件URL',
     replyToId BIGINT COMMENT '回复消息ID',
     isRevoked TINYINT DEFAULT 0 NOT NULL COMMENT '是否已撤回',
     revokedTime DATETIME COMMENT '撤回时间',
     createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '发送时间',
     isDeleted TINYINT DEFAULT 0 NOT NULL COMMENT '逻辑删除标记',

     INDEX idx_room_id (roomId),
     INDEX idx_user_id (userId),
     INDEX idx_create_time (createTime)
) COMMENT '群聊消息表' COLLATE utf8mb4_unicode_ci;

ALTER TABLE `user` ADD COLUMN `phone` char(11) DEFAULT NULL COMMENT '手机号' AFTER `userProfile`;
ALTER TABLE `user` ADD COLUMN `email` varchar(24) DEFAULT NULL COMMENT '邮箱' AFTER `phone`;

ALTER TABLE app ADD COLUMN likeCount INT DEFAULT 0 COMMENT '点赞数';
ALTER TABLE app ADD COLUMN collectCount INT DEFAULT 0 COMMENT '收藏数';
ALTER TABLE app ADD COLUMN commentCount INT DEFAULT 0 COMMENT '评论数';
ALTER TABLE app ADD COLUMN likeUserIds TEXT COMMENT '点赞用户ID列表(JSON)';
ALTER TABLE app ADD COLUMN collectUserIds TEXT COMMENT '收藏用户ID列表(JSON)';

ALTER TABLE user ADD COLUMN unreadNotificationCount INT DEFAULT 0 COMMENT '未读通知数量';

# ALTER TABLE  user DROP COLUMN unread_notification_count;
-- 3. app_comment 表（评论持久化存储）
CREATE TABLE IF NOT EXISTS app_comment (
   id BIGINT AUTO_INCREMENT PRIMARY KEY,
   appId BIGINT NOT NULL COMMENT '应用ID',
   userId BIGINT NOT NULL COMMENT '评论用户ID',
   content VARCHAR(500) NOT NULL COMMENT '评论内容(最多500字符)',
   replyToId BIGINT COMMENT '回复的评论ID',
   parentId BIGINT COMMENT '父评论ID(用于嵌套回复)',
   likeCount INT DEFAULT 0 COMMENT '评论点赞数',
   status TINYINT DEFAULT 1 NOT NULL COMMENT '状态: 1-正常 2-删除',
   createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
   updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   isDelete TINYINT DEFAULT 0 NOT NULL COMMENT '逻辑删除',
   INDEX idx_app_id (appId),
   INDEX idx_user_id (userId),
   INDEX idx_create_time (createTime),
   INDEX idx_parent_id (updateTime)
) COMMENT='应用评论表' COLLATE utf8mb4_unicode_ci;

ALTER TABLE user ADD COLUMN coins INT DEFAULT 0 COMMENT '金币数量';
ALTER TABLE user ADD COLUMN chatRoomQuota INT DEFAULT 0 COMMENT '群聊创建名额';