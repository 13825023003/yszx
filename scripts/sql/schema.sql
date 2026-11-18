-- ============================================================================
-- 云枢智学 数据库建表脚本
-- 依据：docs/design/03.数据库设计.md（评审版，23 张表）
-- 数据库名：yszx
-- 数据库：MySQL 8.0.16+（CHECK 约束需 8.0.16 及以上；字符集 utf8mb4）
-- 说明：
--   1. 本脚本只包含表结构、索引、约束与基础初始化数据，不包含物理外键。
--   2. 所有时间字段按 UTC 存储；执行与运行时连接均应将 time_zone 设置为 +00:00。
--   3. 全站业务表使用 delete_flag（0 未删除 / 1 已删除）逻辑删除；
--      operation_log、role_permission、system_setting 按 03 §6 例外处理。
--   4. 表间关联由应用层维护，禁止在 SQL / ORM / 迁移脚本中创建 FOREIGN KEY。
--   5. 逻辑删除与唯一约束共存（02 R4）：含业务唯一键且可逻辑删除的表增加
--      deleted_token（0=未删除，删除时置为本行 id），唯一索引为 (业务键..., deleted_token)。
--      逻辑删除需在同一事务内同时写 delete_flag=1 与 deleted_token=id。
-- ============================================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE DATABASE IF NOT EXISTS `yszx`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `yszx`;

-- ============================================================================
-- 一、账号、角色与系统配置
-- ============================================================================

CREATE TABLE `student` (
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `mobile`              VARCHAR(11)     NOT NULL                COMMENT '手机号（11 位），登录账号',
    `password_hash`       VARCHAR(100)    NOT NULL                COMMENT 'BCrypt 哈希',
    `nickname`            VARCHAR(50)     NOT NULL                COMMENT '用户名（个人中心可编辑，注册时应用层赋默认值）',
    `avatar`              VARCHAR(500)    NULL                    COMMENT '头像 URL（MinIO）',
    `email`               VARCHAR(100)    NULL                    COMMENT '邮箱（uk_student_email 唯一，允许多行 NULL）',
    `gender`              TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '性别：0保密 1男 2女',
    `preferred_subject_id` BIGINT UNSIGNED NULL                   COMMENT '技术方向 -> subject.id',
    `guide_finished`      TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '新手引导是否看过：0否 1是',
    `status`              TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '状态：1已启用 0已禁用',
    `last_login_at`       DATETIME        NULL                    COMMENT '最后登录时间（UTC）',
    `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`         TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    `deleted_token`       BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '删除令牌：0未删除，删除时置为本行 id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_mobile` (`mobile`, `deleted_token`),
    UNIQUE KEY `uk_student_email` (`email`, `deleted_token`),
    KEY `idx_student_created_at` (`created_at`),
    KEY `idx_student_status` (`status`),
    CONSTRAINT `chk_student_gender` CHECK (`gender` IN (0, 1, 2)),
    CONSTRAINT `chk_student_status` CHECK (`status` IN (0, 1)),
    CONSTRAINT `chk_student_guide` CHECK (`guide_finished` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学员表';

CREATE TABLE `admin_user` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`       VARCHAR(50)     NOT NULL                COMMENT '用户名（登录账号）',
    `password_hash`  VARCHAR(100)    NOT NULL                COMMENT 'BCrypt 哈希',
    `mobile`         VARCHAR(11)     NULL                    COMMENT '手机号；常规用户必填，应用层校验',
    `email`          VARCHAR(100)    NULL                    COMMENT '邮箱（选填）',
    `avatar`         VARCHAR(500)    NULL                    COMMENT '头像 URL（MinIO）',
    `role_id`        BIGINT UNSIGNED NOT NULL                COMMENT '角色 id，逻辑关联 role.id；单角色',
    `status`         TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '状态：1已启用 0已禁用',
    `is_builtin`     TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '是否内置超级管理员：0否 1是，全系统仅允许一个',
    `last_login_at`  DATETIME        NULL                    COMMENT '最后登录时间（UTC）',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`    TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    `deleted_token`  BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '删除令牌：0未删除，删除时置为本行 id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_user_username` (`username`, `deleted_token`),
    UNIQUE KEY `uk_admin_user_mobile` (`mobile`, `deleted_token`),
    KEY `idx_admin_user_role` (`role_id`),
    CONSTRAINT `chk_admin_user_status` CHECK (`status` IN (0, 1)),
    CONSTRAINT `chk_admin_user_builtin` CHECK (`is_builtin` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台用户表';

CREATE TABLE `role` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`           VARCHAR(50)     NOT NULL                COMMENT '角色名称',
    `code`           VARCHAR(50)     NOT NULL                COMMENT '角色编码，全局唯一',
    `description`    VARCHAR(255)    NULL                    COMMENT '描述',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`    TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    `deleted_token`  BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '删除令牌：0未删除，删除时置为本行 id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`code`, `deleted_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台角色表';

CREATE TABLE `permission` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`      BIGINT UNSIGNED NULL                    COMMENT '父节点；NULL=组节点（两层级）',
    `name`           VARCHAR(50)     NOT NULL                COMMENT '组名/权限点名',
    `code`           VARCHAR(100)    NOT NULL                COMMENT '权限编码（如 question:view）',
    `type`           TINYINT UNSIGNED NOT NULL               COMMENT '类型：1组（菜单级） 2操作点',
    `sort_order`     INT             NOT NULL DEFAULT 0      COMMENT '排序',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`    TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    `deleted_token`  BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '删除令牌：0未删除，删除时置为本行 id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`code`, `deleted_token`),
    KEY `idx_permission_parent` (`parent_id`),
    CONSTRAINT `chk_permission_type` CHECK (`type` IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表（权限树 9 组）';

CREATE TABLE `role_permission` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id`        BIGINT UNSIGNED NOT NULL                COMMENT '角色 id，逻辑关联 role.id',
    `permission_id`  BIGINT UNSIGNED NOT NULL                COMMENT '权限 id，逻辑关联 permission.id',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_permission_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-权限关联（物理覆盖，无 delete_flag）';

CREATE TABLE `operation_log` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT UNSIGNED NOT NULL                COMMENT '操作人 id，逻辑关联 admin_user.id',
    `username`       VARCHAR(50)     NOT NULL                COMMENT '用户名快照（可追溯）',
    `action`         VARCHAR(50)     NOT NULL                COMMENT '动作：登录/登出/新增/修改/删除/禁用启用/导出/其他',
    `target_type`    VARCHAR(50)     NOT NULL                COMMENT '操作对象类型（question/course/student 等）',
    `target_name`    VARCHAR(255)    NOT NULL                COMMENT '操作对象描述',
    `ip`             VARCHAR(64)     NOT NULL                COMMENT 'IP 地址',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间（UTC）',
    PRIMARY KEY (`id`),
    KEY `idx_operation_log_user` (`user_id`),
    KEY `idx_operation_log_time` (`created_at`),
    KEY `idx_operation_log_action` (`action`),
    CONSTRAINT `chk_operation_log_action` CHECK (`action` IN ('登录', '登出', '新增', '修改', '删除', '禁用启用', '导出', '其他'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志（审计，无 delete_flag；清空=物理删除）';

CREATE TABLE `system_setting` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key`     VARCHAR(50)     NOT NULL                COMMENT '配置键',
    `config_value`   TEXT            NULL                    COMMENT '配置值（文本/JSON）',
    `remark`         VARCHAR(255)    NULL                    COMMENT '说明',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_system_setting_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统设置（key-value，无 delete_flag）';

-- ============================================================================
-- 二、内容体系：学科、阶段、题目、课程、路线、面试题库
-- ============================================================================

CREATE TABLE `subject` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`           VARCHAR(50)     NOT NULL                COMMENT '学科名称（全局唯一）',
    `icon`           VARCHAR(50)     NOT NULL                COMMENT '图标（Emoji，必填）',
    `description`    VARCHAR(255)    NOT NULL                COMMENT '描述',
    `theme_color`    VARCHAR(20)     NOT NULL DEFAULT '#000000' COMMENT '主题色（预置色板）',
    `sort_order`     INT             NOT NULL DEFAULT 0      COMMENT '排序（越小越靠前，支持上移/下移）',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`    TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    `deleted_token`  BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '删除令牌：0未删除，删除时置为本行 id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subject_name` (`name`, `deleted_token`),
    KEY `idx_subject_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学科表（同时作为 AI 面试方向）';

CREATE TABLE `stage` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `subject_id`     BIGINT UNSIGNED NOT NULL                COMMENT '学科 id，逻辑关联 subject.id',
    `name`           VARCHAR(50)     NOT NULL                COMMENT '阶段名称（同学科下唯一）',
    `icon`           VARCHAR(50)     NULL                    COMMENT '图标（留空默认）',
    `sort_order`     INT             NOT NULL DEFAULT 0      COMMENT '排序',
    `status`         TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '状态：1启用 0禁用',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`    TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    `deleted_token`  BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '删除令牌：0未删除，删除时置为本行 id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stage_subject_name` (`subject_id`, `name`, `deleted_token`),
    KEY `idx_stage_subject` (`subject_id`),
    CONSTRAINT `chk_stage_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学科阶段表';

CREATE TABLE `course` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`              VARCHAR(100)    NOT NULL                COMMENT '课程名称',
    `cover_url`         VARCHAR(500)    NULL                    COMMENT '封面 URL（外链）',
    `duration_minutes`  INT             NOT NULL DEFAULT 0      COMMENT '时长（分钟），展示转小时:分钟',
    `source`            TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '来源：1 B站 2其他',
    `link`              VARCHAR(500)    NULL                    COMMENT '课程链接',
    `bvid`              VARCHAR(20)     NULL                    COMMENT 'BV 号（非 B 站来源为空；存在时唯一）',
    `subject_id`        BIGINT UNSIGNED NULL                    COMMENT '主学科 -> subject.id；NULL=其他学科',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`       TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    `deleted_token`     BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '删除令牌：0未删除，删除时置为本行 id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_bvid` (`bvid`, `deleted_token`),
    KEY `idx_course_subject` (`subject_id`),
    KEY `idx_course_source` (`source`),
    CONSTRAINT `chk_course_source` CHECK (`source` IN (1, 2)),
    CONSTRAINT `chk_course_duration` CHECK (`duration_minutes` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='B站课程库';

CREATE TABLE `course_subject` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_id`      BIGINT UNSIGNED NOT NULL                COMMENT '课程 id，逻辑关联 course.id',
    `subject_id`     BIGINT UNSIGNED NOT NULL                COMMENT '学科 id，逻辑关联 subject.id',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`    TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    `deleted_token`  BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '删除令牌：0未删除，删除时置为本行 id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_subject` (`course_id`, `subject_id`, `deleted_token`),
    KEY `idx_course_subject_subject` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程-学科多学科关联（额外学科）';

CREATE TABLE `question` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `stage_id`       BIGINT UNSIGNED NOT NULL                COMMENT '阶段 id，逻辑关联 stage.id',
    `type`           TINYINT UNSIGNED NOT NULL               COMMENT '类型：1单选 2多选 3判断 4简答',
    `difficulty`     TINYINT UNSIGNED NOT NULL DEFAULT 2     COMMENT '难度：1简单 2中等 3困难',
    `content`        TEXT            NOT NULL                COMMENT '题干',
    `answer`         TEXT            NULL                    COMMENT '判断题正确/错误文本；简答参考答案；单选/多选 NULL',
    `analysis`       TEXT            NULL                    COMMENT '解析',
    `tags`           JSON            NULL                    COMMENT '标签，JSON 字符串数组',
    `status`         TINYINT UNSIGNED NOT NULL DEFAULT 3     COMMENT '状态：1已上架 2已下架 3待审核 4草稿(预留) 5已驳回',
    `reject_reason`  VARCHAR(255)    NULL                    COMMENT '驳回原因（通过或重新提交时清空）',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`    TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_question_stage` (`stage_id`),
    KEY `idx_question_status` (`status`),
    KEY `idx_question_type_difficulty` (`type`, `difficulty`),
    KEY `idx_question_created` (`created_at`),
    CONSTRAINT `chk_question_type` CHECK (`type` IN (1, 2, 3, 4)),
    CONSTRAINT `chk_question_difficulty` CHECK (`difficulty` IN (1, 2, 3)),
    CONSTRAINT `chk_question_status` CHECK (`status` IN (1, 2, 3, 4, 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题库题目';

CREATE TABLE `question_option` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id`    BIGINT UNSIGNED NOT NULL                COMMENT '题目 id，逻辑关联 question.id',
    `option_key`     VARCHAR(4)      NOT NULL                COMMENT '选项标识 A/B/C/D',
    `option_text`    VARCHAR(500)    NOT NULL                COMMENT '选项内容',
    `is_correct`     TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '是否正确：1是 0否',
    `sort_order`     INT             NOT NULL DEFAULT 0      COMMENT '选项排序',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`    TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    `deleted_token`  BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '删除令牌：0未删除，删除时置为本行 id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_option` (`question_id`, `option_key`, `deleted_token`),
    KEY `idx_question_option_qid` (`question_id`),
    CONSTRAINT `chk_question_option_correct` CHECK (`is_correct` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目选项（判断/简答无选项行）';

CREATE TABLE `interview_question` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `subject_id`        BIGINT UNSIGNED NOT NULL                COMMENT '方向 -> subject.id（=学科）',
    `content`           TEXT            NOT NULL                COMMENT '题干',
    `reference_answer`  TEXT            NULL                    COMMENT '参考答案（可留空）',
    `topic_id`          BIGINT UNSIGNED NULL                    COMMENT '主题 id，逻辑关联 interview_topic.id；NULL=词表外主题或未分类',
    `topic`             VARCHAR(50)     NULL                    COMMENT '主题名称快照（NULL=未分类；允许词表外主题）',
    `difficulty`        TINYINT UNSIGNED NOT NULL DEFAULT 2     COMMENT '难度：1简单 2中等 3困难',
    `tags`              JSON            NULL                    COMMENT '标签，JSON 字符串数组',
    `source_type`       TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '来源：1文档解析 2手工录入 3手动导入',
    `source_doc_name`   VARCHAR(255)    NULL                    COMMENT '来源文档名（文件不入库，仅留名）',
    `source_seq`        INT             NULL                    COMMENT '文档内第 N 题',
    `status`            TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '状态：1已上架 2已下架',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`       TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_interview_question_subject_status` (`subject_id`, `status`),
    KEY `idx_interview_question_topic` (`topic_id`),
    KEY `idx_interview_question_difficulty` (`difficulty`),
    CONSTRAINT `chk_interview_question_difficulty` CHECK (`difficulty` IN (1, 2, 3)),
    CONSTRAINT `chk_interview_question_source` CHECK (`source_type` IN (1, 2, 3)),
    CONSTRAINT `chk_interview_question_status` CHECK (`status` IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 面试题库';

CREATE TABLE `interview_topic` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `subject_id`     BIGINT UNSIGNED NULL                    COMMENT '学科 id；NULL=全局主题',
    `name`           VARCHAR(50)     NOT NULL                COMMENT '主题名称',
    `sort_order`     INT             NOT NULL DEFAULT 0      COMMENT '排序',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`    TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    `deleted_token`  BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '删除令牌：0未删除，删除时置为本行 id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_interview_topic_subject_name` (`subject_id`, `name`, `deleted_token`),
    KEY `idx_interview_topic_subject` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题主题词表';

CREATE TABLE `learning_route` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`              VARCHAR(100)    NOT NULL                COMMENT '路线名称',
    `subject_id`        BIGINT UNSIGNED NOT NULL                COMMENT '关联学科 -> subject.id（单学科）',
    `status`            TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '状态：1启用 0禁用',
    `icon`              VARCHAR(50)     NULL                    COMMENT '路线图标',
    `target_education`  VARCHAR(100)    NULL                    COMMENT '适合学历',
    `description`       TEXT            NULL                    COMMENT '路线描述',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`       TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_learning_route_subject` (`subject_id`),
    KEY `idx_learning_route_status` (`status`),
    CONSTRAINT `chk_learning_route_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习路线';

CREATE TABLE `route_course` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `route_id`       BIGINT UNSIGNED NOT NULL                COMMENT '路线 id，逻辑关联 learning_route.id',
    `course_id`      BIGINT UNSIGNED NOT NULL                COMMENT '课程 id，逻辑关联 course.id（含跨学科课程）',
    `sort_order`     INT             NOT NULL DEFAULT 0      COMMENT '课程顺序（顺序即学习顺序）',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`    TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    `deleted_token`  BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '删除令牌：0未删除，删除时置为本行 id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_route_course` (`route_id`, `course_id`, `deleted_token`),
    KEY `idx_route_course_course` (`course_id`),
    CONSTRAINT `chk_route_course_sort` CHECK (`sort_order` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='路线-课程关联';

CREATE TABLE `route_course_completion` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`        BIGINT UNSIGNED NOT NULL                COMMENT '学员 id，逻辑关联 student.id',
    `route_course_id`   BIGINT UNSIGNED NOT NULL                COMMENT '路线课程 id，逻辑关联 route_course.id',
    `completed_at`      DATETIME        NOT NULL                COMMENT '完成时间（UTC）',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`       TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_route_course_completion` (`student_id`, `route_course_id`),
    KEY `idx_route_course_completion_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学员课程完成表';

-- ============================================================================
-- 三、学习训练：答题明细、错题本
-- ============================================================================

CREATE TABLE `practice_record` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`        BIGINT UNSIGNED NOT NULL                COMMENT '学员 id，逻辑关联 student.id',
    `question_id`       BIGINT UNSIGNED NOT NULL                COMMENT '题目 id，逻辑关联 question.id',
    `mode`              TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '练习模式：1顺序 2随机',
    `is_correct`        TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '是否判定正确：1是 0否',
    `duration_seconds`  INT             NULL                    COMMENT '该题已用时长（秒）',
    `practiced_at`      DATETIME        NOT NULL                COMMENT '作答时间（UTC）',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`       TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_practice_record_student_time` (`student_id`, `practiced_at`),
    KEY `idx_practice_record_student_question` (`student_id`, `question_id`),
    KEY `idx_practice_record_question` (`question_id`),
    CONSTRAINT `chk_practice_record_mode` CHECK (`mode` IN (1, 2)),
    CONSTRAINT `chk_practice_record_correct` CHECK (`is_correct` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='刷题作答明细（统计事实来源）';

CREATE TABLE `wrong_question` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`        BIGINT UNSIGNED NOT NULL                COMMENT '学员 id，逻辑关联 student.id',
    `question_id`       BIGINT UNSIGNED NOT NULL                COMMENT '题目 id，逻辑关联 question.id',
    `error_count`       INT UNSIGNED    NOT NULL DEFAULT 1      COMMENT '错误次数（等级 >=3 高频 / =2 多次 / <2 偶尔）',
    `corrected_count`   INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '已纠正次数（连续答对 +1，达 3 自动已掌握）',
    `status`            TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '状态：1待复习 2已掌握',
    `last_wrong_at`     DATETIME        NULL                    COMMENT '最近答错时间（UTC，列表倒序）',
    `last_answer`       TEXT            NULL                    COMMENT '最近一次错误作答快照（支撑“你的答案”）',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`       TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wrong_question` (`student_id`, `question_id`),
    KEY `idx_wrong_question_student_status` (`student_id`, `status`),
    KEY `idx_wrong_question_last_wrong` (`last_wrong_at`),
    CONSTRAINT `chk_wrong_question_count` CHECK (`error_count` >= 1),
    CONSTRAINT `chk_wrong_question_corrected` CHECK (`corrected_count` >= 0),
    CONSTRAINT `chk_wrong_question_status` CHECK (`status` IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='错题本';

-- ============================================================================
-- 四、AI 面试：面试记录、逐题评分、面试报告
-- ============================================================================

CREATE TABLE `interview_record` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`        BIGINT UNSIGNED NOT NULL                COMMENT '学员 id，逻辑关联 student.id',
    `subject_id`        BIGINT UNSIGNED NOT NULL                COMMENT '面试方向 -> subject.id',
    `difficulty`        TINYINT UNSIGNED NOT NULL               COMMENT '难度：1简单 2中等 3困难',
    `question_count`    INT UNSIGNED    NOT NULL                COMMENT '题数（3/5/8/10/15）',
    `overall_score`     DECIMAL(4,1)    NULL                    COMMENT '综合评分 0.0-100.0（逐题 0-10 分平均 ×10）',
    `total_duration`    INT UNSIGNED    NULL                    COMMENT '累计时长（秒）',
    `started_at`        DATETIME        NOT NULL                COMMENT '开始时间（UTC）',
    `completed_at`      DATETIME        NOT NULL                COMMENT '完成时间（UTC）',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`       TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_interview_record_student_time` (`student_id`, `created_at`),
    KEY `idx_interview_record_student_subject_time` (`student_id`, `subject_id`, `created_at`),
    KEY `idx_interview_record_subject` (`subject_id`),
    CONSTRAINT `chk_interview_record_difficulty` CHECK (`difficulty` IN (1, 2, 3)),
    CONSTRAINT `chk_interview_record_count` CHECK (`question_count` IN (3, 5, 8, 10, 15)),
    CONSTRAINT `chk_interview_record_score` CHECK (`overall_score` IS NULL OR `overall_score` BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 面试记录（仅已完成会话）';

CREATE TABLE `interview_question_score` (
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `interview_record_id` BIGINT UNSIGNED NOT NULL                COMMENT '面试记录 id，逻辑关联 interview_record.id',
    `question_order`      INT             NOT NULL                COMMENT '第 N 题（1 起）',
    `question_id`         BIGINT UNSIGNED NULL                    COMMENT '面试题 id；NULL=AI 快照题',
    `question_text`       TEXT            NOT NULL                COMMENT '题干快照（回放展示）',
    `student_answer`      TEXT            NULL                    COMMENT '学员作答',
    `score`               DECIMAL(3,1)    NOT NULL                COMMENT '本题得分 0.0-10.0',
    `ai_feedback`         TEXT            NULL                    COMMENT 'AI 评分理由/反馈',
    `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`         TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_interview_score_record` (`interview_record_id`, `question_order`),
    CONSTRAINT `chk_interview_score` CHECK (`score` BETWEEN 0 AND 10),
    CONSTRAINT `chk_interview_score_order` CHECK (`question_order` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试逐题评分表';

CREATE TABLE `interview_report` (
    `id`                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `interview_record_id`     BIGINT UNSIGNED NOT NULL                COMMENT '面试记录 id，逻辑关联 interview_record.id（1:1）',
    `radar_json`              TEXT            NULL                    COMMENT '6 维能力分析（JSON）',
    `strengths`               TEXT            NULL                    COMMENT '优点，JSON 字符串数组（TEXT 存 JSON）',
    `weaknesses`              TEXT            NULL                    COMMENT '待提升项，JSON 字符串数组（TEXT 存 JSON）',
    `suggestions`             TEXT            NULL                    COMMENT '改进建议，JSON 字符串数组（TEXT 存 JSON）',
    `improvement_cards_json`  TEXT            NULL                    COMMENT '3 张能力提升卡（JSON）',
    `created_at`              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
    `updated_at`              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
    `delete_flag`             TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_interview_report_record` (`interview_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试报告（与面试记录 1:1）';

-- ============================================================================
-- 五、基础初始化数据
-- ============================================================================

-- 5.1 预置角色（3 个）
INSERT IGNORE INTO `role` (`id`, `name`, `code`, `description`) VALUES
(1, '超级管理员', 'super_admin', '拥有全部管理权限，内置唯一超级管理员账号不可新增/切换'),
(2, '内容审核', 'content_reviewer', '只把关不生产：查看全部内容并审核题目，无增删改权限'),
(3, '运营人员', 'operator', '除系统管理外的全部操作，含内容维护与学员管理，不具备审核权限');

-- 5.2 权限树：9 个分组
INSERT IGNORE INTO `permission` (`id`, `parent_id`, `name`, `code`, `type`, `sort_order`) VALUES
(1, NULL, '数据看板',   'dashboard',   1, 1),
(2, NULL, '学科管理',   'subject',     1, 2),
(3, NULL, '课程库',     'course',      1, 3),
(4, NULL, '题库管理',   'question',    1, 4),
(5, NULL, '面试题库',   'interviewbank', 1, 5),
(6, NULL, '学习路线',   'route',       1, 6),
(7, NULL, '学员管理',   'student',     1, 7),
(8, NULL, '用户管理',   'user',        1, 8),
(9, NULL, '系统管理',   'system',      1, 9);

-- 5.3 权限点：9 组共 34 项（D4-5 定稿）
INSERT IGNORE INTO `permission` (`id`, `parent_id`, `name`, `code`, `type`, `sort_order`) VALUES
(11, 4, '查看题目',       'question:view',        2, 1),
(12, 4, '新增题目',       'question:add',         2, 2),
(13, 4, '编辑题目',       'question:edit',        2, 3),
(14, 4, '删除题目',       'question:delete',      2, 4),
(15, 4, '导入题目',       'question:import',      2, 5),
(16, 4, '导出题目',       'question:export',      2, 6),
(17, 4, '审核题目',       'question:audit',       2, 7),

(21, 5, '查看面试题',     'interviewbank:view',   2, 1),
(22, 5, '上传面试题',     'interviewbank:upload', 2, 2),
(23, 5, '编辑面试题',     'interviewbank:edit',   2, 3),
(24, 5, '删除面试题',     'interviewbank:delete', 2, 4),
(25, 5, '检索测试',       'interviewbank:search_test', 2, 5),

(31, 8, '查看后台用户',   'user:view',            2, 1),
(32, 8, '新增后台用户',   'user:add',             2, 2),
(33, 8, '编辑后台用户',   'user:edit',            2, 3),
(34, 8, '禁用/删除后台用户', 'user:disable',      2, 4),

(41, 6, '查看路线',       'route:view',           2, 1),
(42, 6, '新增路线',       'route:add',            2, 2),
(43, 6, '编辑路线',       'route:edit',           2, 3),
(44, 6, '删除路线',       'route:delete',         2, 4),

(51, 7, '查看学员',       'student:view',         2, 1),
(52, 7, '编辑学员',       'student:edit',         2, 2),
(53, 7, '删除学员',       'student:delete',       2, 3),

(61, 9, '操作日志',       'system:log_view',      2, 1),
(62, 9, '系统设置',       'system:setting',       2, 2),

(71, 2, '查看学科',       'subject:view',         2, 1),
(72, 2, '新增学科',       'subject:add',          2, 2),
(73, 2, '编辑学科',       'subject:edit',         2, 3),
(74, 2, '删除学科',       'subject:delete',       2, 4),

(75, 3, '查看课程',       'course:view',          2, 1),
(76, 3, '新增课程',       'course:add',           2, 2),
(77, 3, '编辑课程',       'course:edit',          2, 3),
(78, 3, '删除课程',       'course:delete',        2, 4),

(79, 1, '查看数据看板',   'dashboard:view',       2, 1);

-- 5.4 角色-权限映射
-- 超级管理员：全部权限点
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `permission` WHERE `type` = 2;

-- 内容审核：查看全部内容 + 审核题目
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
SELECT 2, `id` FROM `permission`
WHERE `code` IN (
    'subject:view', 'course:view', 'question:view', 'question:audit',
    'interviewbank:view', 'route:view'
);

-- 运营人员：除系统管理组 + 题目审核外的全部操作
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
SELECT 3, `id` FROM `permission`
WHERE `type` = 2 AND `parent_id` <> 9 AND `code` <> 'question:audit';

-- 5.5 系统设置（4 项固定配置）
INSERT IGNORE INTO `system_setting` (`config_key`, `config_value`, `remark`) VALUES
('site_name', '云枢智学', '平台名称'),
('site_description', 'AI 赋能的智能面试训练平台', '平台描述'),
('logo_url', NULL, '平台 Logo 对象存储 URL'),
('default_avatar', NULL, '默认头像对象存储 URL');

-- 5.6 8 个学科基础数据（同时作为 AI 面试方向）
INSERT IGNORE INTO `subject` (`id`, `name`, `icon`, `description`, `theme_color`, `sort_order`) VALUES
(1, 'AI应用与智能体开发', '🤖', '从大模型基础到 Agent 工程化落地，掌握 Prompt、RAG、工具编排与多智能体协作。', '#6366f1', 1),
(2, 'AI大模型开发', '🧬', '深入 Transformer、预训练与微调，掌握大模型训练、评测与推理部署的完整链路。', '#8b5cf6', 2),
(3, 'AI具身智能机器人开发', '🦾', '面向真实物理世界的智能体，覆盖运动学、感知、强化学习与机器人系统集成。', '#0ea5e9', 3),
(4, 'AI嵌入式_机器人开发', '🔧', '从 MCU、RTOS 到边缘推理，构建低功耗、实时可靠的嵌入式智能设备。', '#14b8a6', 4),
(5, 'AI测试', '🧪', '覆盖测试基础、自动化与 AI 模型质量评测，保障 AI 产品的可靠性与一致性。', '#f59e0b', 5),
(6, 'AI运维', '⚙️', '掌握容器化、GPU 集群与模型服务运维，让 AI 系统稳定高效运行。', '#10b981', 6),
(7, 'AI视觉设计', '🎨', '将 AIGC 融入视觉设计工作流，从提示词到成稿，提升创意产出效率。', '#ec4899', 7),
(8, 'AI视频与短剧创作', '🎬', 'AI 驱动的视频叙事与生产：分镜脚本、生成工具链与成片工作流。', '#f43f5e', 8);

-- 5.7 初始超级管理员
-- 不在仓库中保存明文密码或固定 BCrypt 摘要。部署时由后端初始化任务使用 BCrypt 生成 password_hash，
-- 插入 admin_user 并设置 role_id=1（super_admin）、is_builtin=1；全系统仅允许一个内置超级管理员。

-- ============================================================================
-- 脚本结束
-- ============================================================================
