<template>
    <div class="add-container">
        <el-card class="add-card" shadow="never">
            <template #header>
                <div class="card-header">
                    <span>添加${tableNameUp}</span>
                </div>
            </template>

            <el-form label-position="top" class="add-form">
                <el-row :gutter="24">
                    <#list fieldInfos as field>
                        <el-col :xs="24" :sm="24" :md="24" :lg="24">
                            <el-form-item label="${field.getComment()}" prop="${field.getFieldName()}">
                                <el-input
                                        v-model="info.${field.getFieldName()}"
                                        :placeholder="`请输入${field.getComment()}`"
                                        clearable
                                />
                            </el-form-item>
                        </el-col>
                    </#list>
                </el-row>
            </el-form>

            <el-divider class="form-divider" />

            <div class="action-bar">
                <el-button
                        type="primary"
                        size="large"
                        @click="add"
                        class="add-button"
                >
                    <el-icon><Plus /></el-icon>
                    <span>确认添加</span>
                </el-button>
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
    import { Plus } from '@element-plus/icons-vue'
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
        padding: 20px;
        background-color: var(--el-bg-color-page);
    }

    .add-card {
        border-radius: 8px;

        :deep(.el-card__header) {
            border-bottom: 1px solid var(--el-border-color-light);
            padding: 16px 20px;

            .card-header {
                font-size: 18px;
                font-weight: 600;
                color: var(--el-text-color-primary);
            }
        }
    }

    .add-form {
        :deep(.el-form-item__label) {
            font-weight: 500;
            color: var(--el-text-color-regular);
            margin-bottom: 6px;
        }

        :deep(.el-input__inner) {
            height: 40px;
            line-height: 40px;
            border-radius: 8px;
        }
    }

    .form-divider {
        margin: 20px 0;
    }

    .action-bar {
        display: flex;
        justify-content: center;

        .add-button {
            width: 200px;
            height: 40px;
            font-size: 16px;
            font-weight: 500;
            border-radius: 8px;
        }
    }

    @media (max-width: 768px) {
        .add-form {
            :deep(.el-col) {
                width: 100%;
                margin-bottom: 0;
            }
        }

        .action-bar {
            .add-button {
                width: 100%;
            }
        }
    }
</style>