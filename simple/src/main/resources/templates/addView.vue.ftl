<template>
    <div>
        <el-row :gutter="20">
            <el-col :span="24">
                <el-form label-position="left" label-width="auto">
                    <#list fieldInfos as field>
                        <el-form-item label="${field.getComment()}">
                            <el-input v-model="info.${field.getFieldName()}" autocomplete="off"  />
                        </el-form-item>
                    </#list>
                </el-form>
            </el-col>
        </el-row>
        <el-divider />
        <div style="display: flex;justify-content: center;">
            <el-button type="primary" plain @click="add" size="large">添加</el-button>
        </div>
    </div>

</template>

<script setup lang='ts'>
    import { alert, http } from '@/utils';
    import { onMounted, ref } from 'vue';

    const props = defineProps({
        id: String
    })

    const emit = defineEmits(['success']);

    const info = ref({
        <#list fieldInfos as field>
        ${field.getFieldName()}: '',
        </#list>
    })


    const add = () => {
        http.result({
            url: '/${tableNameDown}/add',
            method: 'POST',
            data: info.value,
            success(result) {
                alert(result.msg, 'success')
                emit('success');
                // queryInfo()
            }
        })
    }

    onMounted(() => {
    })

</script>

<style scoped>
    .el-col {
        margin-top: 1rem;
    }
</style>