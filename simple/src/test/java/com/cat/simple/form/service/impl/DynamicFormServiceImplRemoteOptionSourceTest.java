package com.cat.simple.form.service.impl;

import com.cat.common.entity.dynamicForm.DynamicFormField;
import com.cat.common.entity.dynamicForm.DynamicFormFieldType;
import com.cat.common.entity.dynamicForm.DynamicFormLinkageRule;
import com.cat.common.entity.dynamicForm.DynamicFormOption;
import com.cat.common.entity.dynamicForm.DynamicFormOptionMapping;
import com.cat.common.entity.dynamicForm.DynamicFormOptionSource;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DynamicFormServiceImplRemoteOptionSourceTest {

    private final DynamicFormServiceImpl service = new DynamicFormServiceImpl();

    @Test
    void validateFieldAllowsApiSelectWithoutStaticOptions() {
        DynamicFormField field = baseField(DynamicFormFieldType.SELECT)
                .setOptionSource(apiSource())
                .setOptions(List.of())
                .setDefaultValue("archivedValue");

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateField", field));
    }

    @Test
    void validateFieldRejectsApiSourceOnUnsupportedFieldType() {
        DynamicFormField field = baseField(DynamicFormFieldType.INPUT)
                .setOptionSource(apiSource());

        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validateField", field));
    }

    @Test
    void validateFieldRejectsAbsoluteApiUrl() {
        DynamicFormField field = baseField(DynamicFormFieldType.SELECT)
                .setOptionSource(apiSource().setUrl("https://example.com/options"));

        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validateField", field));
    }

    @Test
    void validateFieldKeepsStaticSelectOptionsRequired() {
        DynamicFormField field = baseField(DynamicFormFieldType.SELECT)
                .setOptions(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validateField", field));
    }

    @Test
    void validateFieldApiCascaderDefaultOnlyChecksStructure() {
        DynamicFormField field = baseField(DynamicFormFieldType.CASCADER)
                .setOptionSource(apiSource().setMapping(apiMapping().setChildrenPath("children")))
                .setOptions(List.of())
                .setDefaultValue(List.of("history", "removed"));

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateField", field));
    }

    @Test
    void validateFormDataAllowsApiMultiSelectWithoutStaticOptions() {
        DynamicFormField field = baseField(DynamicFormFieldType.MULTISELECT)
                .setOptionSource(apiSource())
                .setOptions(List.of());
        Map<String, Object> data = Map.of(field.getFieldId(), List.of("history", 2));

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateFormData",
                List.of(field), data, List.<DynamicFormLinkageRule>of()));
    }

    @Test
    void validateFormDataRejectsApiMultiSelectNonListValue() {
        DynamicFormField field = baseField(DynamicFormFieldType.MULTISELECT)
                .setOptionSource(apiSource())
                .setOptions(List.of());
        Map<String, Object> data = Map.of(field.getFieldId(), "notList");

        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validateFormData",
                        List.of(field), data, List.<DynamicFormLinkageRule>of()));
    }

    @Test
    void validateFormDataRejectsApiCascaderNonListPath() {
        DynamicFormField field = baseField(DynamicFormFieldType.CASCADER)
                .setOptionSource(apiSource().setMapping(apiMapping().setChildrenPath("children")))
                .setOptions(List.of());
        Map<String, Object> data = Map.of(field.getFieldId(), "beijing,chaoyang");

        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validateFormData",
                        List.of(field), data, List.<DynamicFormLinkageRule>of()));
    }

    @Test
    void validateFormDataKeepsStaticMultiSelectOptionMembershipCheck() {
        DynamicFormField field = baseField(DynamicFormFieldType.MULTISELECT)
                .setOptions(List.of(new DynamicFormOption("A", "a")));
        Map<String, Object> data = Map.of(field.getFieldId(), List.of("b"));

        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validateFormData",
                        List.of(field), data, List.<DynamicFormLinkageRule>of()));
    }

    @Test
    void validateFieldRejectsApiParamsWithNonJsonValue() {
        Map<String, Object> params = new HashMap<>();
        params.put("unsupported", new Object());
        DynamicFormField field = baseField(DynamicFormFieldType.SELECT)
                .setOptionSource(apiSource().setParams(params));

        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validateField", field));
    }

    private DynamicFormField baseField(DynamicFormFieldType type) {
        return new DynamicFormField()
                .setFieldId("fieldA")
                .setTitle("字段A")
                .setType(type)
                .setRequired("0")
                .setSpan(24)
                .setOptions(List.of(new DynamicFormOption("有效", "valid")));
    }

    private DynamicFormOptionSource apiSource() {
        return new DynamicFormOptionSource()
                .setType("API")
                .setUrl("/api/common/options/project-types")
                .setMethod("GET")
                .setMapping(apiMapping());
    }

    private DynamicFormOptionMapping apiMapping() {
        return new DynamicFormOptionMapping()
                .setListPath("data.records")
                .setLabelPath("name")
                .setValuePath("code");
    }
}
