<template>
    <div class="add-container">
        <div v-loading="loading" element-loading-text="加载中...">
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
                    <#list fieldInfos as field>
                        <el-form-item label="${field.getComment()}" prop="${field.getFieldName()}">
                            <el-input
                                v-model="info.${field.getFieldName()}"
                                :placeholder="`请输入${field.getComment()}`"
                                autocomplete="off"
                                clearable
                                size="large">
                                <template #prefix>
                                    <el-icon><Document /></el-icon>
                                </template>
                            </el-input>
                        </el-form-item>
                    </#list>
                </el-form>

                <div class="form-footer">
                    <el-button type="primary" size="large" @click="add" class="save-button" :loading="loading">
                        <el-icon><Check /></el-icon>
                        <span>确认添加</span>
                    </el-button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { Plus, Document, Check } from '@element-plus/icons-vue'
import { alert, http } from '@/utils';
import { ref } from 'vue';

const emit = defineEmits(['success']);

const loading = ref(false)

const info = ref({
    <#list fieldInfos as field>
    ${field.getFieldName()}: '',
    </#list>
})

const add = async () => {
    loading.value = true
    try {
        await http.post('/${tableNameDown}/add', info.value)
        info.value = {
            <#list fieldInfos as field>
            ${field.getFieldName()}: '',
            </#list>
        }
        alert('添加成功', 'success')
        emit('success');
    } finally {
        loading.value = false
    }
}
</script>

<style scoped lang="scss">
.add-container {
    padding: 24px;
    background: var(--bg-page);

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

    .add-form {
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
    .add-container {
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
