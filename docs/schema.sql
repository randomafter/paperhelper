-- 历史特色创作平台 - MySQL 建库建表
-- 执行前请先创建数据库: CREATE DATABASE history_creation DEFAULT CHARACTER SET utf8mb4;

USE history_creation;

-- 角色表
CREATE TABLE IF NOT EXISTS `role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL COMMENT 'ADMIN/CREATOR',
  `description` VARCHAR(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(128) NOT NULL COMMENT 'SHA256或BCrypt',
  `email` VARCHAR(128) DEFAULT NULL,
  `nickname` VARCHAR(64) DEFAULT NULL,
  `intro` VARCHAR(200) DEFAULT NULL COMMENT '个人简介',
  `avatar` VARCHAR(512) DEFAULT NULL,
  `role_id` BIGINT NOT NULL DEFAULT 2 COMMENT '1=ADMIN 2=CREATOR',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `security_question` VARCHAR(255) NOT NULL COMMENT '安全问题',
  `security_answer` VARCHAR(255) NOT NULL COMMENT '安全答案',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_role` (`role_id`),
  CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- 验证码（短信/邮件）可先Mock，表结构预留
CREATE TABLE IF NOT EXISTS `verification_code` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `target` VARCHAR(128) NOT NULL COMMENT '手机号或邮箱',
  `code` VARCHAR(10) NOT NULL,
  `type` VARCHAR(20) NOT NULL COMMENT 'register/login/reset',
  `expires_at` DATETIME NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_target_type` (`target`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验证码';

-- 初始角色
INSERT INTO `role` (`id`, `name`, `description`) VALUES (1, 'ADMIN', '管理员'), (2, 'CREATOR', '普通创作者');

-- 标签表
CREATE TABLE IF NOT EXISTS `tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `type` VARCHAR(20) DEFAULT NULL COMMENT '可选分类',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签';

-- 素材表（汉/唐/宋/明）
CREATE TABLE IF NOT EXISTS `material` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `dynasty` VARCHAR(20) NOT NULL COMMENT '汉/唐/宋/明',
  `category` VARCHAR(20) NOT NULL COMMENT '制度/文化/服饰/语言/战争',
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT NOT NULL,
  `source_url` VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_dynasty` (`dynasty`),
  KEY `idx_category` (`category`),
  KEY `idx_dynasty_category` (`dynasty`, `category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='历史特色素材';

-- 素材-标签多对多
CREATE TABLE IF NOT EXISTS `material_tag` (
  `material_id` BIGINT NOT NULL,
  `tag_id` BIGINT NOT NULL,
  PRIMARY KEY (`material_id`, `tag_id`),
  KEY `idx_tag_id` (`tag_id`),
  CONSTRAINT `fk_mt_material` FOREIGN KEY (`material_id`) REFERENCES `material` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_mt_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='素材标签关联';

-- 素材收藏表
CREATE TABLE IF NOT EXISTS `material_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `material_id` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_material` (`user_id`, `material_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_material` (`material_id`),
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_favorite_material` FOREIGN KEY (`material_id`) REFERENCES `material` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='素材收藏';

-- 创作作品表（工作台）
CREATE TABLE IF NOT EXISTS `creation_work` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(200) NOT NULL DEFAULT '未命名',
  `content` LONGTEXT,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  CONSTRAINT `fk_work_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作作品';

-- 创作版本（最近5版）
CREATE TABLE IF NOT EXISTS `creation_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `work_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `content_snapshot` LONGTEXT,
  `version_number` INT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_work` (`work_id`),
  CONSTRAINT `fk_version_work` FOREIGN KEY (`work_id`) REFERENCES `creation_work` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作版本';

-- 历史风格提示词模板（汉/唐/宋/明）
CREATE TABLE IF NOT EXISTS `prompt_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `dynasty` VARCHAR(20) NOT NULL,
  `scene_type` VARCHAR(50) NOT NULL COMMENT 'scene/dialogue/fragment',
  `name` VARCHAR(100) DEFAULT NULL,
  `template_text` TEXT NOT NULL COMMENT '占位符: {material_context} {user_input}',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_dynasty_type` (`dynasty`, `scene_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提示词模板';

-- 默认提示词模板示例
INSERT INTO `prompt_template` (`dynasty`, `scene_type`, `name`, `template_text`) VALUES
('汉', 'scene', '汉代场景', '你是一位熟悉汉代历史的创作者。请根据以下素材与用户要求，用汉代风格描写场景。\n\n素材：{material_context}\n\n用户要求：{user_input}'),
('唐', 'dialogue', '唐代对话', '你是一位熟悉唐代文化的创作者。请根据以下素材，写一段唐代风格的人物对话。\n\n素材：{material_context}\n\n用户要求：{user_input}');
