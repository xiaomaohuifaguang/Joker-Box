/*
 Navicat Premium Dump SQL

 Source Server         : R7000P-root
 Source Server Type    : MySQL
 Source Server Version : 80404 (8.4.4)
 Source Host           : 192.168.3.35:32001
 Source Schema         : joker-box-dev

 Target Server Type    : MySQL
 Target Server Version : 80404 (8.4.4)
 File Encoding         : 65001

 Date: 07/05/2026 15:35:42
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for cat_dynamic_form_linkage
-- ----------------------------
DROP TABLE IF EXISTS `cat_dynamic_form_linkage`;
CREATE TABLE `cat_dynamic_form_linkage`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `form_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '表单ID，关联 cat_dynamic_form.id',
  `version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '表单版本号',
  `trigger_field_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发字段 fieldId（前端设计ID）',
  `trigger_condition` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'EQ' COMMENT '触发条件：EQ(等于)/NE(不等于)/GT(大于)/LT(小于)/GE(大于等于)/LE(小于等于)/IN(包含)/NOT_IN(不包含)/EMPTY(为空)/NOT_EMPTY(非空)/REGEX(正则)',
  `trigger_value` json NULL COMMENT '触发值，JSON格式（字符串/数字/数组等）',
  `action_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '动作类型：SHOW(显示)/HIDE(隐藏)/REQUIRED(必填)/OPTION(设置选项)/VALUE(设置值)/DISABLED(禁用)/ENABLED(启用)/SET_PATTERN(设置正则)/SET_SPAN(设置宽度)',
  `target_field_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目标字段 fieldId（前端设计ID）',
  `action_value` json NULL COMMENT '动作参数，JSON格式（如OPTION时传选项列表）',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '规则执行顺序，越小越先执行',
  `deleted` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '逻辑删除 0未删除 1已删除',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_form_version`(`form_id` ASC, `version` ASC) USING BTREE,
  INDEX `idx_trigger`(`trigger_field_id` ASC) USING BTREE,
  INDEX `idx_target`(`target_field_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '动态表单字段联动规则表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
