/*
 Navicat Premium Data Transfer

 Source Server         : localhost的3306
 Source Server Type    : MySQL
 Source Server Version : 80025
 Source Host           : localhost:3306
 Source Schema         : history_creation

 Target Server Type    : MySQL
 Target Server Version : 80025
 File Encoding         : 65001

 Date: 16/03/2026 21:52:35
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for creation_version
-- ----------------------------
DROP TABLE IF EXISTS `creation_version`;
CREATE TABLE `creation_version`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `work_id` bigint(0) NOT NULL,
  `user_id` bigint(0) NOT NULL,
  `content_snapshot` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `version_number` int(0) NOT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_work`(`work_id`) USING BTREE,
  CONSTRAINT `fk_version_work` FOREIGN KEY (`work_id`) REFERENCES `creation_work` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '创作版本' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of creation_version
-- ----------------------------

-- ----------------------------
-- Table structure for creation_work
-- ----------------------------
DROP TABLE IF EXISTS `creation_work`;
CREATE TABLE `creation_work`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '未命名',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `group_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分组名称',
  `last_opened_at` datetime(0) NULL DEFAULT NULL COMMENT '最近打开时间',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE,
  CONSTRAINT `fk_work_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '创作作品' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of creation_work
-- ----------------------------
INSERT INTO `creation_work` VALUES (5, 4, 'gyjug', 'uiyfkctyijc', NULL, NULL, '2026-03-14 21:49:43', '2026-03-14 21:49:43');
INSERT INTO `creation_work` VALUES (9, 7, '曹', 'fdg', NULL, '2026-03-14 21:53:50', '2026-03-14 21:53:50', '2026-03-14 21:54:07');
INSERT INTO `creation_work` VALUES (10, 7, '先', '', NULL, '2026-03-14 21:54:11', '2026-03-14 21:54:11', '2026-03-14 21:54:19');
INSERT INTO `creation_work` VALUES (11, 7, '琴', '', NULL, '2026-03-14 21:54:22', '2026-03-14 21:54:22', '2026-03-14 21:54:29');
INSERT INTO `creation_work` VALUES (12, 6, 'cao', 'sadfsadnh fjsd fisdfds', '玉华', '2026-03-14 21:56:18', '2026-03-14 21:54:57', '2026-03-14 21:56:22');
INSERT INTO `creation_work` VALUES (13, 6, 'yu', 'jhdskfasjdfhsadv grfg ', NULL, '2026-03-14 21:56:01', '2026-03-14 21:55:08', '2026-03-14 21:56:06');
INSERT INTO `creation_work` VALUES (14, 6, '未命名胜多负少的', '的说法都是富士达fghd hh', '玉华', '2026-03-14 23:24:50', '2026-03-14 21:57:04', '2026-03-14 23:24:56');
INSERT INTO `creation_work` VALUES (15, 6, '未命名', '', NULL, '2026-03-14 23:29:30', '2026-03-14 22:00:03', '2026-03-14 22:00:03');
INSERT INTO `creation_work` VALUES (16, 6, '未命名', '', NULL, '2026-03-14 23:52:57', '2026-03-14 23:32:36', '2026-03-14 23:32:36');
INSERT INTO `creation_work` VALUES (17, 6, '未命名v自动发布重点', '非常v注册从v\n【一个好办法不是】\n发给本宫本人\n', '玉华子', '2026-03-15 01:24:08', '2026-03-15 01:15:47', '2026-03-15 01:16:07');

-- ----------------------------
-- Table structure for favorite_group
-- ----------------------------
DROP TABLE IF EXISTS `favorite_group`;
CREATE TABLE `favorite_group`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL COMMENT '所属用户ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分组名称',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_name`(`user_id`, `name`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  CONSTRAINT `fk_fav_group_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收藏分组' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of favorite_group
-- ----------------------------

-- ----------------------------
-- Table structure for feedback
-- ----------------------------
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/read/resolved',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_created`(`created_at`) USING BTREE,
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户反馈' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of feedback
-- ----------------------------
INSERT INTO `feedback` VALUES (1, 6, '6', 'sdfsd', 'pending', '2026-03-15 01:14:04', '2026-03-15 01:14:04');

-- ----------------------------
-- Table structure for material
-- ----------------------------
DROP TABLE IF EXISTS `material`;
CREATE TABLE `material`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '历史沉淀/传统民俗/服饰装扮/行业手艺/宗教信仰/兵器武林/饮食文化/玉石珍宝/传说典故/科技文明/五行异象/其他',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `source_user_id` bigint(0) NULL DEFAULT NULL COMMENT '若为用户投稿素材，则记录提交人',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED=正式; PENDING=待审核; REJECTED=已驳回',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '历史特色素材' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of material
-- ----------------------------
INSERT INTO `material` VALUES (1, '服饰装扮', '一个好办法不是', '发给本宫本人', NULL, 'PUBLISHED', '2026-03-14 18:38:21', '2026-03-14 18:38:21');

-- ----------------------------
-- Table structure for material_favorite
-- ----------------------------
DROP TABLE IF EXISTS `material_favorite`;
CREATE TABLE `material_favorite`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `material_id` bigint(0) NOT NULL,
  `group_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '未定义' COMMENT '用户自定义收藏分组名称',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_material`(`user_id`, `material_id`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE,
  INDEX `idx_material`(`material_id`) USING BTREE,
  CONSTRAINT `fk_favorite_material` FOREIGN KEY (`material_id`) REFERENCES `material` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '素材收藏' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of material_favorite
-- ----------------------------
INSERT INTO `material_favorite` VALUES (1, 5, 1, '未定义', '2026-03-14 19:03:02');
INSERT INTO `material_favorite` VALUES (2, 6, 1, '改', '2026-03-15 01:14:47');

-- ----------------------------
-- Table structure for material_tag
-- ----------------------------
DROP TABLE IF EXISTS `material_tag`;
CREATE TABLE `material_tag`  (
  `material_id` bigint(0) NOT NULL,
  `tag_id` bigint(0) NOT NULL,
  PRIMARY KEY (`material_id`, `tag_id`) USING BTREE,
  INDEX `idx_tag_id`(`tag_id`) USING BTREE,
  CONSTRAINT `fk_mt_material` FOREIGN KEY (`material_id`) REFERENCES `material` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_mt_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '素材标签关联' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of material_tag
-- ----------------------------

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL COMMENT '接收者ID',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'material_approved/material_rejected',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `ref_id` bigint(0) NULL DEFAULT NULL COMMENT '关联素材ID',
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE,
  INDEX `idx_user_read`(`user_id`, `is_read`) USING BTREE,
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站内通知' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notification
-- ----------------------------
INSERT INTO `notification` VALUES (1, 5, 'material_approved', '素材审核通过', '你的素材《一个好办法不是》已审核通过，现已收录至素材检索库！', 1, 1, '2026-03-14 18:38:21');
INSERT INTO `notification` VALUES (2, 4, 'material_rejected', '素材审核未通过', '你的素材《gfsbsrbwet》未通过审核。审核意见：gdhnrgnwr ', 2, 1, '2026-03-14 18:39:23');

-- ----------------------------
-- Table structure for prompt_template
-- ----------------------------
DROP TABLE IF EXISTS `prompt_template`;
CREATE TABLE `prompt_template`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `dynasty` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `scene_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'scene/dialogue/fragment',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `template_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '占位符: {material_context} {user_input}',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_dynasty_type`(`dynasty`, `scene_type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提示词模板' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of prompt_template
-- ----------------------------
INSERT INTO `prompt_template` VALUES (1, '汉', 'scene', '汉代场景', '你是一位熟悉汉代历史的创作者。请根据以下素材与用户要求，用汉代风格描写场景。\n\n素材：{material_context}\n\n用户要求：{user_input}', '2026-03-08 22:43:20');
INSERT INTO `prompt_template` VALUES (2, '唐', 'dialogue', '唐代对话', '你是一位熟悉唐代文化的创作者。请根据以下素材，写一段唐代风格的人物对话。\n\n素材：{material_context}\n\n用户要求：{user_input}', '2026-03-08 22:43:20');

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'ADMIN/CREATOR',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role
-- ----------------------------
INSERT INTO `role` VALUES (1, 'ADMIN', '管理员');
INSERT INTO `role` VALUES (2, 'CREATOR', '普通创作者');

-- ----------------------------
-- Table structure for tag
-- ----------------------------
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '可选分类',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '标签' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tag
-- ----------------------------

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SHA256或BCrypt',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `role_id` bigint(0) NOT NULL DEFAULT 2 COMMENT '1=ADMIN 2=CREATOR',
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `intro` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '个人简介',
  `security_question` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `security_answer` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE,
  UNIQUE INDEX `uk_email`(`email`) USING BTREE,
  INDEX `idx_role`(`role_id`) USING BTREE,
  CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (4, 'master', '$2a$10$3hyECIV7A2XqFDZ0Q/prHOEzITkjqKR99kalh7pnn9Dc558ne0xCS', '1043997344@qq.com', '阿童木', NULL, 1, 1, '2026-03-08 23:03:36', '2026-03-08 23:41:20', NULL, '', '');
INSERT INTO `user` VALUES (5, 'moon', '$2a$10$xcpXlovYF.zgPGGRd7cGyu//YcKUxQmE5rqJa2XeK7rx8jXtymDqS', '', '', NULL, 2, 1, '2026-03-09 19:51:02', '2026-03-09 19:51:02', NULL, '我的OC是？', '韵律');
INSERT INTO `user` VALUES (6, 'one', '$2a$10$kh5cnXPJv7bw2VfAlGReKOVZb0TKoRZWmuwBINeO6k9pHcK.TtbE2', NULL, '123123', NULL, 2, 1, '2026-03-14 20:49:30', '2026-03-14 20:49:30', NULL, '我的名字', 'K');
INSERT INTO `user` VALUES (7, '111', '$2a$10$2iD3oSOrqhgrgF1SFnIiy.7U/C5SmCrUHmQWJLdGRaRAgJ9t.Nh7K', NULL, '', NULL, 2, 1, '2026-03-14 21:52:30', '2026-03-14 21:52:30', NULL, '1', '1');

-- ----------------------------
-- Table structure for user_material
-- ----------------------------
DROP TABLE IF EXISTS `user_material`;
CREATE TABLE `user_material`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL COMMENT '创建者ID',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标签，逗号分隔',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'draft' COMMENT 'draft=草稿/pending=待审核/approved=已通过/rejected=已拒绝',
  `admin_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '审核意见',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  CONSTRAINT `fk_user_material_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户自建素材' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_material
-- ----------------------------
INSERT INTO `user_material` VALUES (1, 5, '服饰装扮', '一个好办法不是', '发给本宫本人', '', 'approved', 'trgdbf', '2026-03-14 18:36:31', '2026-03-14 18:36:31');
INSERT INTO `user_material` VALUES (2, 4, '兵器武林', 'gfsbsrbwet', 'erafgvefbettbteb', '', 'rejected', 'gdhnrgnwr ', '2026-03-14 18:39:11', '2026-03-14 18:39:11');
INSERT INTO `user_material` VALUES (3, 5, '服饰装扮', 'afaergqer', 'dfwreferger', '', 'draft', NULL, '2026-03-14 18:41:06', '2026-03-14 18:41:06');
INSERT INTO `user_material` VALUES (4, 5, '历史沉淀', 'ghvhghyvggyiv', 'hjhbjhbvhg', '', 'pending', NULL, '2026-03-14 19:03:50', '2026-03-14 19:03:50');

-- ----------------------------
-- Table structure for verification_code
-- ----------------------------
DROP TABLE IF EXISTS `verification_code`;
CREATE TABLE `verification_code`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `target` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号或邮箱',
  `code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'register/login/reset',
  `expires_at` datetime(0) NOT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_target_type`(`target`, `type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '验证码' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of verification_code
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
