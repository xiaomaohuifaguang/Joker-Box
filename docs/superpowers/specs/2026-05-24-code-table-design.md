# 码表功能设计方案

## 1. 背景

动态表单已支持通过 `optionSource` 从同源 API 加载远程候选项。为了让常见下拉、单选、多选、级联等字段更容易复用稳定选项，需要新增一套全局码表能力，提供码表管理、码项管理，以及符合远程选项数据源约定的 options 查询接口。

第一版码表不作为新的动态表单字段类型，也不扩展 `optionSource.type`。码表只提供标准 API，动态表单继续使用现有 `optionSource.type = API` 接入。

## 2. 目标

- 支持全局共享码表。
- 支持平铺码表和树形码表。
- 支持码表完整 CRUD。
- 支持码项完整 CRUD。
- 提供动态表单可直接配置的 options 查询接口。
- 返回结构兼容 `DynamicFormOption` 映射约定。
- 不改变动态表单远程选项数据源第一版边界。

## 3. 不做范围

第一版不做：

- 按组织或租户隔离。
- 码表版本管理。
- 码表导入导出。
- 码表缓存。
- 码表值被表单引用时的强约束删除。
- 发布表单时校验 `params.code` 是否存在。
- 提交表单时校验值是否存在于码表。
- 新增 `optionSource.type = CODE_TABLE`。

## 4. 动态表单接入方式

动态表单字段仍使用现有 API 远程选项配置。

普通下拉示例：

```json
{
  "type": "SELECT",
  "title": "项目类型",
  "optionSource": {
    "type": "API",
    "url": "/api/code-table/options",
    "method": "GET",
    "params": {
      "code": "project_type"
    },
    "mapping": {
      "listPath": "data",
      "labelPath": "label",
      "valuePath": "value"
    }
  },
  "options": []
}
```

级联示例：

```json
{
  "type": "CASCADER",
  "title": "地区",
  "optionSource": {
    "type": "API",
    "url": "/api/code-table/options",
    "method": "GET",
    "params": {
      "code": "area"
    },
    "mapping": {
      "listPath": "data",
      "labelPath": "label",
      "valuePath": "value",
      "childrenPath": "children"
    }
  },
  "options": []
}
```

后续字段模板可以预置上述配置，减少前端手工配置成本。

## 5. 数据库设计

用户已在 MySQL 中按以下设计建表。

### 5.1 码表主表

```sql
CREATE TABLE `cat_code_table` (
  `id` varchar(32) NOT NULL COMMENT '主键',
  `code` varchar(64) NOT NULL COMMENT '码表编码，全局唯一',
  `name` varchar(64) NOT NULL COMMENT '码表名称',
  `tree` char(1) NOT NULL DEFAULT '0' COMMENT '是否树形：0否 1是',
  `status` char(1) NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `deleted` char(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除 1已删除',
  `create_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_table_code_deleted` (`code`, `deleted`),
  KEY `idx_code_table_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='码表';
```

### 5.2 码项表

```sql
CREATE TABLE `cat_code_item` (
  `id` varchar(32) NOT NULL COMMENT '主键',
  `table_id` varchar(32) NOT NULL COMMENT '码表ID',
  `parent_id` varchar(32) DEFAULT NULL COMMENT '父级码项ID，树形码表使用',
  `label` varchar(128) NOT NULL COMMENT '显示文本',
  `value` varchar(128) NOT NULL COMMENT '提交值，同一码表内唯一',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` char(1) NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `deleted` char(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除 1已删除',
  `create_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_item_table_value_deleted` (`table_id`, `value`, `deleted`),
  KEY `idx_code_item_table_parent_sort` (`table_id`, `parent_id`, `sort`),
  KEY `idx_code_item_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='码项';
```

### 5.3 数据规则

- `cat_code_table.code` 全局唯一，作为动态表单配置里的 `params.code`。
- `cat_code_item.value` 在同一个码表内唯一，作为表单提交值。
- 平铺码表：`tree = '0'`，码项 `parent_id = null`。
- 树形码表：`tree = '1'`，根节点 `parent_id = null`，子节点 `parent_id = 上级码项 id`。
- 停用码表不允许被 options 查询接口返回。
- 停用码项不出现在 options 查询结果中。
- 删除使用逻辑删除。

## 6. 后端模块设计

### 6.1 common 模块

新增：

```text
common/src/main/java/com/cat/common/entity/codeTable/CodeTable.java
common/src/main/java/com/cat/common/entity/codeTable/CodeItem.java
common/src/main/java/com/cat/common/entity/codeTable/CodeTablePageParam.java
common/src/main/java/com/cat/common/entity/codeTable/CodeItemQueryParam.java
common/src/main/java/com/cat/common/entity/codeTable/CodeOption.java
```

职责：

- `CodeTable`：映射 `cat_code_table`。
- `CodeItem`：映射 `cat_code_item`。
- `CodeTablePageParam`：码表分页查询条件。
- `CodeItemQueryParam`：码项列表和树查询条件。
- `CodeOption`：`/api/code-table/options` 的返回节点。

`CodeOption` 结构：

```java
public class CodeOption {
    private String label;
    private String value;
    private List<CodeOption> children;
}
```

### 6.2 simple 模块

新增：

```text
simple/src/main/java/com/cat/simple/codeTable/controller/CodeTableController.java
simple/src/main/java/com/cat/simple/codeTable/controller/CodeItemController.java
simple/src/main/java/com/cat/simple/codeTable/mapper/CodeTableMapper.java
simple/src/main/java/com/cat/simple/codeTable/mapper/CodeItemMapper.java
simple/src/main/java/com/cat/simple/codeTable/service/CodeTableService.java
simple/src/main/java/com/cat/simple/codeTable/service/impl/CodeTableServiceImpl.java
```

职责：

- `CodeTableController`：码表分页、新增、更新、删除、详情、options 查询。
- `CodeItemController`：码项列表、新增、更新、删除、树查询。
- `CodeTableMapper` / `CodeItemMapper`：MyBatis-Plus 基础 CRUD。
- `CodeTableService`：聚合码表和码项业务规则。

## 7. 接口设计

### 7.1 码表管理接口

```text
POST /api/code-table/page
POST /api/code-table/add
POST /api/code-table/update
POST /api/code-table/delete
POST /api/code-table/detail
```

### 7.2 码项管理接口

```text
POST /api/code-item/list
POST /api/code-item/add
POST /api/code-item/update
POST /api/code-item/delete
POST /api/code-item/tree
```

### 7.3 动态表单远程选项接口

```text
GET /api/code-table/options?code=project_type
```

平铺码表返回：

```json
{
  "code": 200,
  "data": [
    {
      "label": "研发项目",
      "value": "dev"
    },
    {
      "label": "实施项目",
      "value": "delivery"
    }
  ]
}
```

树形码表返回：

```json
{
  "code": 200,
  "data": [
    {
      "label": "中国",
      "value": "cn",
      "children": [
        {
          "label": "北京",
          "value": "beijing"
        }
      ]
    }
  ]
}
```

叶子节点不输出 `children`。

## 8. 校验规则

### 8.1 码表校验

- `code` 必填，长度不超过 64。
- `code` 只允许字母、数字、下划线、中划线、点，且以字母开头。
- `code` 正则：`^[a-zA-Z][a-zA-Z0-9_.-]{0,63}$`。
- `code` 全局唯一，逻辑删除数据不占用唯一性。
- `name` 必填，长度不超过 64。
- `tree` 只能是 `0` 或 `1`。
- `status` 只能是 `0` 或 `1`。
- 删除码表时，同步逻辑删除该码表下所有码项。

### 8.2 码项校验

- `tableId` 必填，且码表必须存在。
- `label` 必填，长度不超过 128。
- `value` 必填，长度不超过 128。
- `value` 在同一个码表内唯一，逻辑删除数据不占用唯一性。
- 平铺码表的 `parentId` 必须为空。
- 树形码表的 `parentId` 可以为空；不为空时必须属于同一个码表。
- 更新码项时不能把自己设置为自己的父级。
- 更新码项时不能把自己的子孙节点设置为父级。
- `sort` 为空时默认 0。
- `status` 只能是 `0` 或 `1`。
- 删除树形码项时，同步逻辑删除当前码项及所有子孙码项。

### 8.3 options 查询校验

`GET /api/code-table/options?code=xxx`：

- `code` 必填。
- 找不到码表时返回业务错误：`码表不存在: xxx`。
- 码表已停用时返回业务错误：`码表已停用: xxx`。
- 只返回 `status = '1'` 且 `deleted = '0'` 的码项。
- 按 `sort ASC, create_time ASC` 排序。
- 平铺码表返回一维数组。
- 树形码表返回树形数组。
- 叶子节点不输出 `children`。

## 9. 与动态表单校验的关系

- 动态表单发布时仍只校验 `optionSource` 配置结构。
- 后端不在发布时校验 `params.code` 对应码表是否存在。
- 表单提交时仍只校验 API 模式值结构。
- 后端不在提交时校验提交值是否存在于码表。

原因：码表可能随时间调整，历史表单配置和历史提交值不应因为码表变化变成非法。

## 10. 测试方案

新增测试：

```text
simple/src/test/java/com/cat/simple/codeTable/service/impl/CodeTableServiceImplTest.java
```

覆盖：

1. 新增码表：
   - `code` / `name` / `tree` / `status` 校验。
   - `code` 唯一性校验。
2. 新增码项：
   - `tableId` 必须存在。
   - 平铺码表不能有 `parentId`。
   - 树形码表 `parentId` 必须属于同一码表。
   - `value` 同码表唯一。
3. 更新码项：
   - 禁止 `parentId = 自己`。
   - 禁止 `parentId = 子孙节点`。
4. 删除：
   - 删除码表同步逻辑删除码项。
   - 删除树形码项同步删除子孙节点。
5. options 查询：
   - 平铺返回一维数组。
   - 树形返回嵌套数组。
   - 停用码表报错。
   - 停用码项不返回。
   - 叶子节点不输出 `children`。

## 11. 实现顺序建议

1. 新增 common 实体和参数对象。
2. 新增 mapper 和 service 接口。
3. 实现码表 CRUD。
4. 实现码项 CRUD 和树构建。
5. 实现 options 查询接口。
6. 补充服务层测试。
7. 补充动态表单配置示例文档。

## 12. 结论

第一版采用：

```text
全局码表 + 码项管理 + 标准 options 查询 API + 动态表单 optionSource API 接入
```

该方案不改变动态表单远程选项核心模型，能同时支持普通选项字段和级联字段，也方便后续通过字段模板预置常用码表配置。
