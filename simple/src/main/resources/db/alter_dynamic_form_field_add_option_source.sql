ALTER TABLE `cat_dynamic_form_field`
ADD COLUMN `option_source` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '选项远程数据源配置' AFTER `options`;
