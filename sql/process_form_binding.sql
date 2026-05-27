-- 流程定义-表单绑定表
CREATE TABLE IF NOT EXISTS cat_process_definition_form (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    process_definition_id   INT NOT NULL COMMENT '自建流程定义ID（逻辑关联，无外键）',
    version                 VARCHAR(32) NOT NULL COMMENT 'DRAFT / 1 / 2 / 3 ...',
    form_id                 VARCHAR(64) NOT NULL COMMENT '表单ID',
    form_version            VARCHAR(32) NOT NULL COMMENT '绑定的表单版本号（快照锁定）',
    bind_type               VARCHAR(32) NOT NULL COMMENT 'GLOBAL-全局默认 / NODE-节点绑定',
    node_id                 VARCHAR(64) COMMENT 'BPMN节点ID，GLOBAL时为空',
    inherit_main_form       VARCHAR(1) DEFAULT '0' COMMENT '节点是否继承主表单字段：0-否 / 1-是',
    create_by               VARCHAR(64),
    create_time             DATETIME,
    UNIQUE KEY uk_def_version_node (process_definition_id, version, node_id)
) COMMENT '流程定义-表单绑定（按版本隔离）';

-- 节点字段权限表
CREATE TABLE IF NOT EXISTS cat_process_node_field_permission (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    process_definition_id   INT NOT NULL COMMENT '自建流程定义ID（逻辑关联，无外键）',
    version                 VARCHAR(32) NOT NULL COMMENT 'DRAFT / 1 / 2 / 3 ...',
    node_id                 VARCHAR(64) NOT NULL COMMENT 'BPMN节点ID',
    field_key               VARCHAR(128) NOT NULL COMMENT '字段标识（对应表单 field.key）',
    permission              VARCHAR(32) NOT NULL COMMENT 'VISIBLE / READONLY / HIDDEN / EDITABLE / REQUIRED',
    create_by               VARCHAR(64),
    create_time             DATETIME,
    UNIQUE KEY uk_def_version_node_field (process_definition_id, version, node_id, field_key)
) COMMENT '节点字段权限（按版本隔离）';
