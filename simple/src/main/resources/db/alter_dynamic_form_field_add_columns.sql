ALTER TABLE `cat_dynamic_form_field`
ADD COLUMN `columns` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '动态表格列定义（仅TABLE类型）' AFTER `options`;
