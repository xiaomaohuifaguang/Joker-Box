/*
 ============================================
 动态表单字段联动规则 - 单表拆树形结构迁移脚本
 目标：支持 (A AND (B OR C)) 任意嵌套 + 版本管理
 执行前请确保已备份数据库
 ============================================
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 创建规则表（目标字段 + 动作）
-- ============================================
DROP TABLE IF EXISTS `cat_dynamic_form_linkage_rule`;
CREATE TABLE `cat_dynamic_form_linkage_rule` (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `form_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '表单ID，关联 cat_dynamic_form.id',
  `version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '表单版本号',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '规则名称（供前端展示）',
  `target_field_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目标字段 fieldId（前端设计ID）',
  `action_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '动作类型：SHOW(显示)/HIDE(隐藏)/REQUIRED(必填)/OPTION(设置选项)/VALUE(设置值)/DISABLED(禁用)/ENABLED(启用)/SET_PATTERN(设置正则)/SET_SPAN(设置宽度)',
  `action_value` json DEFAULT NULL COMMENT '动作参数，JSON格式（如OPTION时传选项列表）',
  `enable` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '规则执行顺序，越小越先执行',
  `deleted` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '逻辑删除 0未删除 1已删除',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_form_version` (`form_id`,`version`) USING BTREE,
  KEY `idx_target` (`target_field_id`) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态表单字段联动规则表' ROW_FORMAT=Dynamic;

-- ============================================
-- 2. 创建条件节点表（树形结构，支持嵌套 AND/OR/CONDITION）
-- ============================================
DROP TABLE IF EXISTS `cat_dynamic_form_linkage_node`;
CREATE TABLE `cat_dynamic_form_linkage_node` (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `rule_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属规则ID，关联 cat_dynamic_form_linkage_rule.id',
  `parent_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '父节点ID，null表示根节点',
  `node_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点类型：AND(与)/OR(或)/CONDITION(条件)',
  `trigger_field_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '触发字段 fieldId，仅CONDITION节点有效',
  `trigger_condition` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '触发条件：EQ(等于)/NE(不等于)/GT(大于)/LT(小于)/GE(大于等于)/LE(小于等于)/IN(包含)/NOT_IN(不包含)/EMPTY(为空)/NOT_EMPTY(非空)/REGEX(正则)，仅CONDITION节点有效',
  `trigger_value` json DEFAULT NULL COMMENT '触发值，JSON格式，仅CONDITION节点有效',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '同级节点排序',
  `deleted` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '逻辑删除 0未删除 1已删除',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_rule_id` (`rule_id`) USING BTREE,
  KEY `idx_parent_id` (`parent_id`) USING BTREE,
  KEY `idx_node_type` (`node_type`) USING BTREE,
  KEY `idx_trigger_field` (`trigger_field_id`) USING BTREE
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态表单字段联动条件节点表' ROW_FORMAT=Dynamic;

-- ============================================
-- 3. 迁移规则数据（旧表每条记录 = 一条规则）
-- ============================================
INSERT INTO `cat_dynamic_form_linkage_rule`
(`id`, `form_id`, `version`, `name`, `target_field_id`, `action_type`, `action_value`,
 `enable`, `sort_order`, `deleted`, `create_by`, `create_time`, `update_time`)
SELECT
  `id`,
  `form_id`,
  `version`,
  NULL,
  `target_field_id`,
  `action_type`,
  `action_value`,
  1,
  `sort_order`,
  `deleted`,
  `create_by`,
  `create_time`,
  `update_time`
FROM `cat_dynamic_form_linkage`;

-- ============================================
-- 4. 迁移根节点（每条规则统一创建一个 AND 根节点，id复用rule_id）
--    原因：保持树结构统一，所有规则的根都是 LOGIC 节点，前端渲染和判定逻辑一致
-- ============================================
INSERT INTO `cat_dynamic_form_linkage_node`
(`id`, `rule_id`, `parent_id`, `node_type`, `trigger_field_id`, `trigger_condition`, `trigger_value`, `sort_order`, `deleted`, `create_by`, `create_time`, `update_time`)
SELECT
  `id`,
  `id`,
  NULL,
  'AND',
  NULL,
  NULL,
  NULL,
  0,
  `deleted`,
  `create_by`,
  `create_time`,
  `update_time`
FROM `cat_dynamic_form_linkage`;

-- ============================================
-- 5. 迁移条件节点（旧记录的触发条件作为根节点的子节点）
--    parent_id = 根节点id（即旧记录id / rule_id）
-- ============================================
INSERT INTO `cat_dynamic_form_linkage_node`
(`id`, `rule_id`, `parent_id`, `node_type`, `trigger_field_id`, `trigger_condition`, `trigger_value`, `sort_order`, `deleted`, `create_by`, `create_time`, `update_time`)
SELECT
  UUID(),
  `id`,
  `id`,
  'CONDITION',
  `trigger_field_id`,
  `trigger_condition`,
  `trigger_value`,
  `sort_order`,
  `deleted`,
  `create_by`,
  `create_time`,
  `update_time`
FROM `cat_dynamic_form_linkage`;

-- ============================================
-- 6. 验证数据完整性（可选，执行后自查）
-- ============================================
-- 规则数、根节点数、条件节点数应该相等
SELECT '规则数' AS label, COUNT(*) AS cnt FROM `cat_dynamic_form_linkage_rule`
UNION ALL
SELECT '根节点(AND)数', COUNT(*) FROM `cat_dynamic_form_linkage_node` WHERE `parent_id` IS NULL
UNION ALL
SELECT '条件节点数', COUNT(*) FROM `cat_dynamic_form_linkage_node` WHERE `node_type` = 'CONDITION';

-- ============================================
-- 7. 备份旧表（确认数据无误后再执行）
-- ============================================
-- RENAME TABLE `cat_dynamic_form_linkage` TO `cat_dynamic_form_linkage_bak_20260508`;

SET FOREIGN_KEY_CHECKS = 1;
