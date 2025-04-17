<template>
    <div class="detail-container">
        <div v-loading="loading" element-loading-text="加载中..." element-loading-background="rgba(255, 255, 255, 0.7)">
            <el-card class="detail-card" shadow="never">
                <template #header>
                    <div class="card-header">
                        <span>{{ props.type === 'view' ? '${tableNameUp}详情' : '编辑${tableNameUp}' }}</span>
                    </div>
                </template>

                <el-form label-position="top" class="detail-form">
                    <el-row :gutter="24">
                        <#list fieldInfos as field>
                            <el-col :xs="24" :sm="24" :md="24" :lg="24">
                                <el-form-item label="${field.getComment()}">
                                    <el-input
                                            v-model="info.${field.getFieldName()}"
                                            :disabled="props.type !== 'edit'"
                                            :placeholder="`请输入${field.getComment()}`"
                                    />
                                </el-form-item>
                            </el-col>
                        </#list>
                    </el-row>
                </el-form>

                <el-divider class="form-divider" />

                <div class="action-bar" v-if="props.type === 'edit'">
                    <el-button
                            type="primary"
                            size="large"
                            @click="save"
                            class="save-button"
                    >
                        <el-icon><Check /></el-icon>
                        <span>保存修改</span>
                    </el-button>
                </div>
            </el-card>
        </div>
    </div>
</template>

<script setup lang="ts">
    import { Check } from '@element-plus/icons-vue'
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
        padding: 20px;
        background-color: var(--el-bg-color-page);
    }

    .detail-card {
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

    .detail-form {
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
        margin-top: 20px;

        .save-button {
            width: 200px;
            height: 40px;
            font-size: 16px;
            font-weight: 500;
            border-radius: 8px;
        }
    }

    @media (max-width: 768px) {
        .detail-form {
            :deep(.el-col) {
                width: 100%;
                margin-bottom: 0;
            }
        }

        .action-bar {
            .save-button {
                width: 100%;
            }
        }
    }
</style>