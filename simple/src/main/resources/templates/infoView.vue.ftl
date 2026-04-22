<template>
    <div class="detail-container">
        <div v-loading="loading" element-loading-text="加载中...">
            <div class="content-wrapper">
                <div class="form-header">
                    <div class="header-icon">
                        <el-icon><Document /></el-icon>
                    </div>
                    <div class="header-content">
                        <h3>{{ props.type === 'view' ? '${tableNameUp}详情' : '编辑${tableNameUp}' }}</h3>
                        <p>{{ props.type === 'view' ? '查看${tableNameUp}详细信息' : '修改${tableNameUp}信息' }}</p>
                    </div>
                </div>

                <el-form label-position="top" class="detail-form">
                    <el-row :gutter="24">
                        <#list fieldInfos as field>
                            <el-col :xs="24" :sm="24" :md="24" :lg="24">
                                <el-form-item label="${field.getComment()}">
                                    <el-input
                                        v-model="info.${field.getFieldName()}"
                                        :disabled="props.type !== 'edit'"
                                        :placeholder="`请输入${field.getComment()}`"
                                        size="large">
                                        <template #prefix>
                                            <el-icon><Document /></el-icon>
                                        </template>
                                    </el-input>
                                </el-form-item>
                            </el-col>
                        </#list>
                    </el-row>
                </el-form>

                <div class="action-bar" v-if="props.type === 'edit'">
                    <el-button type="primary" size="large" @click="save" class="save-button">
                        <el-icon><Check /></el-icon>
                        <span>保存修改</span>
                    </el-button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { Document, Check } from '@element-plus/icons-vue'
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
        },
        complete() {
            loading.value = false
        }
    })
}

onMounted(() => {
    if (!props.id) return;
    queryInfo()
})
</script>

<style scoped lang="scss">
.detail-container {
    padding: 24px;
    background: var(--el-bg-color-page);

    .content-wrapper {
        max-width: 900px;
        margin: 0 auto;
    }

    .form-header {
        display: flex;
        align-items: center;
        gap: 16px;
        margin-bottom: 28px;

        .header-icon {
            width: 56px;
            height: 56px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border-radius: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;

            .el-icon {
                font-size: 26px;
                color: white;
            }
        }

        .header-content {
            flex: 1;

            h3 {
                margin: 0 0 6px 0;
                font-size: 20px;
                font-weight: 600;
                color: var(--el-text-color-primary);
            }

            p {
                margin: 0;
                font-size: 14px;
                color: var(--el-text-color-secondary);
            }
        }
    }

    .detail-form {
        :deep(.el-form-item__label) {
            font-weight: 500;
            color: var(--el-text-color-regular);
            padding-bottom: 8px;
        }

        :deep(.el-input__wrapper) {
            border-radius: 10px;
        }
    }

    .action-bar {
        display: flex;
        justify-content: center;
        margin-top: 24px;
        padding-top: 20px;
        border-top: 1px solid var(--el-border-color-lighter);

        .save-button {
            min-width: 200px;
            height: 46px;
            font-size: 16px;
            font-weight: 500;
            border-radius: 12px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border: none;
            transition: all 0.3s;

            &:hover {
                transform: translateY(-2px);
                box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
            }
        }
    }
}

@media (max-width: 768px) {
    .detail-container {
        padding: 16px;

        .form-header {
            flex-direction: column;
            text-align: center;
        }

        .action-bar {
            .save-button {
                width: 100%;
            }
        }
    }
}
</style>
