CREATE TABLE IF NOT EXISTS `cat_process_instance_form` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `process_instance_id` INT           NOT NULL COMMENT '流程实例ID',
    `node_id`            VARCHAR(64)    NULL     COMMENT '节点ID，null=主表单',
    `form_id`            VARCHAR(64)    NOT NULL COMMENT '表单ID',
    `form_version`       VARCHAR(16)    NOT NULL COMMENT '表单版本（启动时快照）',
    `form_instance_id`   VARCHAR(64)    NOT NULL COMMENT '表单实例ID',
    `create_by`          VARCHAR(64)    NULL     COMMENT '创建人',
    `create_time`        DATETIME       NULL     COMMENT '创建时间',
    UNIQUE KEY `uk_instance_node` (`process_instance_id`, `node_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程实例表单关联' ROW_FORMAT = Dynamic;