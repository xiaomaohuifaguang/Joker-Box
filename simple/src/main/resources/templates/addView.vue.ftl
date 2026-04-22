<template>
    <div class="add-container">
        <div class="content-wrapper">
            <div class="form-header">
                <div class="header-icon">
                    <el-icon><Plus /></el-icon>
                </div>
                <div class="header-content">
                    <h3>添加${tableNameUp}</h3>
                    <p>添加新的${tableNameUp}并填写信息</p>
                </div>
            </div>

            <el-form label-position="top" class="add-form">
                <el-row :gutter="24">
                    <#list fieldInfos as field>
                        <el-col :xs="24" :sm="24" :md="24" :lg="24">
                            <el-form-item label="${field.getComment()}" prop="${field.getFieldName()}">
                                <el-input
                                    v-model="info.${field.getFieldName()}"
                                    :placeholder="`请输入${field.getComment()}`"
                                    clearable
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

            <div class="action-bar">
                <el-button type="primary" size="large" @click="add" class="add-button">
                    <el-icon><Check /></el-icon>
                    <span>确认添加</span>
                </el-button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { Plus, Document, Check } from '@element-plus/icons-vue'
import { alert, http } from '@/utils';
import { ref } from 'vue';

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
        }
    })
}
</script>

<style scoped lang="scss">
.add-container {
    padding: 24px;
    background: var(--el-bg-color-page);

    .content-wrapper {
        max-width: 800px;
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

    .add-form {
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

        .add-button {
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
    .add-container {
        padding: 16px;

        .form-header {
            flex-direction: column;
            text-align: center;
        }

        .action-bar {
            .add-button {
                width: 100%;
            }
        }
    }
}
</style>
