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
                    <#list fieldInfos as field>
                        <el-form-item label="${field.getComment()}">
                            <el-input
                                v-model="info.${field.getFieldName()}"
                                :disabled="props.type !== 'edit'"
                                :placeholder="`请输入${field.getComment()}`"
                                autocomplete="off"
                                size="large">
                                <template #prefix>
                                    <el-icon><Document /></el-icon>
                                </template>
                            </el-input>
                        </el-form-item>
                    </#list>
                </el-form>
            </div>

            <div class="form-footer" v-if="props.type === 'edit'">
                <el-button type="primary" size="large" @click="save" class="save-button" :loading="loading">
                    <el-icon><Check /></el-icon>
                    <span>保存修改</span>
                </el-button>
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

const queryInfo = async () => {
    if (!props.id) return
    loading.value = true
    try {
        info.value = await http.post('/${tableNameDown}/info', undefined, { params: { id: props.id } })
    } finally {
        loading.value = false
    }
}

const save = async () => {
    loading.value = true
    try {
        const result = await http.post('/${tableNameDown}/update', info.value, { raw: true })
        alert(result.msg, 'success')
        await queryInfo()
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    if (!props.id) return;
    queryInfo()
})
</script>

<style scoped lang="scss">
.detail-container {
    padding: 24px;
    background: var(--bg-page);

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
            background: var(--brand-gradient);
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
                color: var(--text-primary);
            }

            p {
                margin: 0;
                font-size: 14px;
                color: var(--text-secondary);
            }
        }
    }

    .detail-form {
        :deep(.el-form-item__label) {
            font-weight: 500;
            color: var(--text-regular);
            padding-bottom: 8px;
        }

        :deep(.el-input__wrapper) {
            border-radius: 10px;
        }
    }

    .form-footer {
        display: flex;
        justify-content: center;
        margin-top: 24px;
        padding-top: 20px;
        border-top: 1px solid var(--border-light);

        .save-button {
            min-width: 200px;
            height: 46px;
            font-size: 16px;
            font-weight: 500;
            border-radius: 12px;
            background: var(--brand-gradient);
            border: none;
            transition: all 0.3s;

            &:hover {
                transform: translateY(-2px);
                box-shadow: var(--shadow-glow-strong);
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

        .form-footer {
            .save-button {
                width: 100%;
            }
        }
    }
}
</style>
