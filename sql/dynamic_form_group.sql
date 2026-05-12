-- ============================================================
-- 动态表单字段分组
-- 支持：字段按分组展示/折叠，分组独立排序，版本管理
-- 历史版本兼容：存量字段 group_id = null，前端归入默认组
-- ============================================================

-- ----------------------------
-- 1. 字段分组表
-- ----------------------------
DROP TABLE IF EXISTS `cat_dynamic_form_field_group`;
CREATE TABLE `cat_dynamic_form_field_group` (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分组id',
  `form_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '表单id',
  `version` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '版本',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分组名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分组描述',
  `sort` int NULL DEFAULT 0 COMMENT '分组排序，越小越靠前',
  `collapsed` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '默认折叠 0展开 1折叠',
  `deleted` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_form_version`(`form_id`, `version`) USING BTREE COMMENT '按表单+版本查询分组'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '动态表单字段分组' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 2. 字段表增加 group_id
-- ----------------------------
ALTER TABLE `cat_dynamic_form_field`
ADD COLUMN `group_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属分组id' AFTER `form_id`;

-- 字段表增加索引，加速按分组查询字段
ALTER TABLE `cat_dynamic_form_field`
ADD INDEX `idx_group_id`(`group_id`) USING BTREE;

-- ----------------------------
-- 3. 字段实例表增加 group_id（可选，如果详情展示需要知道字段归属分组）
-- 注：字段实例通常只关心 val，分组信息可以从字段定义反查，此列按需添加
-- ALTER TABLE `cat_dynamic_form_field_instance`
-- ADD COLUMN `group_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属分组id' AFTER `form_field_id`;
-- ----------------------------

-- ----------------------------
-- 4. 历史数据兼容说明
-- ----------------------------
-- 存量字段的 group_id 为 NULL，前端渲染时归入"默认分组"（不显示分组标题）
-- 无需执行 UPDATE，保持 NULL 即可
