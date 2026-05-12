/*
 ============================================
 动态表单字段联动条件节点表 - 添加 version 字段
 参考 DynamicFormField 对 version 的处理
 ============================================
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 添加字段
ALTER TABLE `cat_dynamic_form_linkage_node`
  ADD COLUMN `form_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '表单ID' AFTER `rule_id`,
  ADD COLUMN `version` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '版本号' AFTER `form_id`;

-- 2. 回填已有数据：通过 rule_id 关联 rule 表回填 form_id 和 version
UPDATE `cat_dynamic_form_linkage_node` n
  INNER JOIN `cat_dynamic_form_linkage_rule` r ON n.rule_id = r.id
SET n.form_id = r.form_id,
    n.version = r.version;

-- 3. 添加索引
ALTER TABLE `cat_dynamic_form_linkage_node`
  ADD INDEX `idx_form_version` (`form_id`, `version`) USING BTREE;

-- 4. 验证
SELECT '回填前节点数' AS label, COUNT(*) AS cnt FROM `cat_dynamic_form_linkage_node`
UNION ALL
SELECT '回填后 form_id 为空的节点数', COUNT(*) FROM `cat_dynamic_form_linkage_node` WHERE `form_id` = '' OR `form_id` IS NULL
UNION ALL
SELECT '回填后 version 为空的节点数', COUNT(*) FROM `cat_dynamic_form_linkage_node` WHERE `version` = '' OR `version` IS NULL;

SET FOREIGN_KEY_CHECKS = 1;
