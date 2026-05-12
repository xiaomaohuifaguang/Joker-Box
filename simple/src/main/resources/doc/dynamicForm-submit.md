# 动态表单 - 提交数据接口

> 作者：小猫会发光
> 日期：2026-05-09

## 基本信息

| 项 | 值 |
| --- | --- |
| 接口路径 | `/dynamicForm/submit` |
| 请求方式 | `POST` |
| Content-Type | `application/json`（`@RequestBody`） |
| Service | `DynamicFormService#submit` |
| 事务 | `@Transactional(rollbackFor = Exception.class)` |

> 用户填写表单后提交数据，后端执行联动规则判定 + 字段级校验，保存表单实例和字段值。

---

## 请求参数（FormData）

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| formId | String | 是 | 表单ID |
| version | String | 否 | 版本号，不传使用表单最新版本 |
| formInstanceId | String | 否 | 实例ID，传则更新，不传则新建 |
| data | Map<String, Object> | 是 | 表单数据，**key 为 fieldId** |

### data 格式说明

| 字段类型 | value 格式 | 示例 |
|----------|-----------|------|
| INPUT / TEXTAREA | String | `"张三"` |
| NUMBER | Number / String | `25` 或 `"25"` |
| SELECT / RADIO | String（选项 value） | `"tech"` |
| CHECKBOX / MULTISELECT | List<String> | `["opt1", "opt2"]` |
| DATE / DATETIME / TIME | String | `"2026-05-09"` |
| SWITCH | Boolean / String | `true` 或 `"true"` |
| CASCADER | List<String>（路径） | `["province", "city", "district"]` |
| UPLOAD | String / List（文件URL） | `["http://xxx/file.pdf"]` |

### 请求示例

```http
POST /dynamicForm/submit
Content-Type: application/json

{
  "formId": "abc123",
  "version": "2",
  "formInstanceId": "inst-001",
  "data": {
    "field_name": "张三",
    "field_dept": "tech",
    "field_type": "sick",
    "field_days": 2,
    "field_hospital": "市人民医院"
  }
}
```

---

## 业务流程

1. 校验表单存在且已发布（status=1）
2. 获取对应版本的表单项和联动规则
3. 执行联动规则计算字段最终状态（显隐/必填/禁用/选项/值）
4. 字段级校验：
   - 被 `HIDE` 或 `DISABLED` 的字段**跳过必填校验**
   - 必填校验（required=1 且非隐藏/禁用时）
   - 长度校验（minLength / maxLength）
   - 数值范围校验（min / max，仅 NUMBER 类型）
   - 正则校验（pattern）
5. 新建或更新表单实例（DynamicFormInstance）
6. 保存或更新各字段值（DynamicFormFieldInstance）

---

## 响应数据（HttpResult<Void>）

---

## 异常情况

| 触发条件 | 异常 |
| --- | --- |
| 表单不存在 | `IllegalArgumentException("表单不存在: {id}")` |
| 表单未发布 | `IllegalStateException("表单未发布, 无法提交: {id}")` |
| 表单模板为空 | `IllegalStateException("表单模板为空, 无法提交: {id}")` |
| 必填字段为空 | `IllegalArgumentException("{title} 必填")` |
| 长度不足 | `IllegalArgumentException("{title} 长度不能小于 {n}")` |
| 长度超限 | `IllegalArgumentException("{title} 长度不能大于 {n}")` |
| 数值过小 | `IllegalArgumentException("{title} 不能小于 {n}")` |
| 数值过大 | `IllegalArgumentException("{title} 不能大于 {n}")` |
| 非数字 | `IllegalArgumentException("{title} 必须为数字")` |
| 正则不匹配 | `IllegalArgumentException("{patternTips}")` 或 `"{title} 格式不正确"` |
| 未登录 | `IllegalStateException("当前未登录")` |
