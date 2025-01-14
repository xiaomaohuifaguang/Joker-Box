<template>
    <div v-loading="loading">
        <el-row :gutter="20">
            <el-col :span="24">
                <el-form label-position="left" label-width="auto">
                    <#list fieldInfos as field>
                        <el-form-item label="${field.getComment()}">
                            <el-input v-model="info.${field.getFieldName()}" autocomplete="off" :disabled="props.type != 'edit'"  />
                        </el-form-item>
                    </#list>
                </el-form>
            </el-col>
        </el-row>
        <el-divider />
        <div style="display: flex;justify-content: center;" v-if="props.type == 'edit'">
            <el-button type="primary" plain @click="save" size="large">保存</el-button>
        </div>
    </div>

</template>

<script setup lang='ts'>
    import { alert, http } from '@/utils';
    import { onMounted, ref } from 'vue';

    const props = defineProps({
        id: String,
        type: String
    })

    const loading = ref(false)

    const info = ref({
    <#list fieldInfos as field>
        ${field.getFieldName()}: '',
    </#list>
    })

    const queryInfo = () => {
        loading.value = true
        console.log(props.id)
        http.result({
            url: '/${tableNameDown}/info',
            method: 'POST',
            data: {
                id: props.id
            },
            success(result) {
                info.value = result.data
                loading.value = false
            }
        })
    }

    const save = () => {
        loading.value = true
        http.result({
            url: '/${tableNameDown}/update',
            method: 'POST',
            data: info.value,
            success(result) {
                alert(result.msg, 'success')
                queryInfo()
            }
        })
    }

    onMounted(() => {
        if (props.id == '') return;
        queryInfo()
    })

</script>

<style scoped>
    .el-col {
        margin-top: 1rem;
    }
</style>