<template>
    <div class="page-container">
        <!-- 面包屑导航 -->
        <div class="breadcrumb-wrapper">
            <el-breadcrumb separator="/">
                <el-breadcrumb-item :to="{ path: '/console' }">
                    <el-icon><House /></el-icon>
                    <span>控制台</span>
                </el-breadcrumb-item>
                <el-breadcrumb-item>${tableNameUp}管理</el-breadcrumb-item>
            </el-breadcrumb>
        </div>

        <el-divider class="custom-divider" />

        <!-- 主内容区 -->
        <div class="content-wrapper">
            <div v-loading="loading" element-loading-text="加载中..." element-loading-background="rgba(255, 255, 255, 0.7)">
                <!-- 搜索和操作区域 -->
                <el-card class="search-card" shadow="never">
                    <el-row :gutter="20" class="search-row">
                        <el-col :xs="24" :sm="18" :md="20" :lg="20">
                            <el-input
                                    v-model="queryParam.search"
                                    placeholder="请输入搜索内容"
                                    size="large"
                                    clearable
                                    @keyup.enter="queryPage"
                                    @clear="queryPage"
                            >
                                <template #prefix>
                                    <el-icon><Search /></el-icon>
                                </template>
                            </el-input>
                        </el-col>
                        <el-col :xs="24" :sm="6" :md="4" :lg="4" class="action-col">
                            <el-button
                                    type="primary"
                                    size="large"
                                    @click="dialogAdd = true"
                                    class="add-button"
                            >
                                <el-icon><Plus /></el-icon>
                                <span>新建</span>
                            </el-button>
                        </el-col>
                    </el-row>
                </el-card>

                <!-- 数据表格 -->
                <el-card class="table-card" shadow="never">
                    <el-table
                            :data="tableData"
                            stripe
                            border
                            style="width: 100%"
                            @selection-change="handleSelectionChange"
                            @sort-change="handleSortChange"
                            :default-sort="{ prop: 'createTime', order: 'descending' }"
                    >
                        <el-table-column type="selection" width="50" align="center" />
                        <#list fieldInfos as field>
                            <el-table-column prop="${field.getFieldName()}" label="${field.getComment()}" min-width="150" />
                        </#list>
                        <el-table-column label="操作" fixed="right" width="220" align="center">
                            <template #default="scope">
                                <el-button
                                        type="primary"
                                        link
                                        size="small"
                                        @click="openDialog(scope.row.id, 'view')"
                                >
                                    <el-icon><View /></el-icon>
                                    <span>详情</span>
                                </el-button>
                                <el-button
                                        type="primary"
                                        link
                                        size="small"
                                        @click="openDialog(scope.row.id, 'edit')"
                                >
                                    <el-icon><Edit /></el-icon>
                                    <span>编辑</span>
                                </el-button>
                                <el-button
                                        type="danger"
                                        link
                                        size="small"
                                        @click="confirmDelete(scope.row.id)"
                                >
                                    <el-icon><Delete /></el-icon>
                                    <span>删除</span>
                                </el-button>
                            </template>
                        </el-table-column>
                    </el-table>

                    <!-- 分页 -->
                    <div class="pagination-wrapper">
                        <el-pagination
                                v-model:current-page="pageInfo.current"
                                :page-size="pageInfo.size"
                                :total="pageInfo.total"
                                layout="total, sizes, prev, pager, next, jumper"
                                :page-sizes="[10, 20, 50, 100]"
                                @size-change="handleSizeChange"
                                @current-change="handleCurrentChange"
                        />
                    </div>
                </el-card>
            </div>
        </div>

        <!-- 详情/编辑对话框 -->
        <el-dialog
                v-model="dialogEdit.open"
                :title="dialogEdit.title"
                width="800px"
                center
                destroy-on-close
                @closed="closeDialog"
        >
            <${tableNameUp}InfoView v-model:id="dialogEdit.id" v-model:type="dialogEdit.type" :key="dialogEdit.id" />
        </el-dialog>

        <!-- 添加对话框 -->
        <el-dialog
                v-model="dialogAdd"
                title="添加${tableNameUp}"
                width="400px"
                center
                destroy-on-close
                @closed="queryPage"
        >
            <${tableNameUp}AddView @success="handleAddSuccess" />
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
    import { House, Search, Plus, View, Edit, Delete } from '@element-plus/icons-vue'
    import { http, alert, confirm } from '@/utils';
    import { onMounted, ref } from 'vue';
    import ${tableNameUp}InfoView from './${tableNameUp}InfoView.vue';
    import ${tableNameUp}AddView from './${tableNameUp}AddView.vue';

    const loading = ref(false)
    const multipleSelection = ref([])
    const tableData = ref([])

    const queryParam = ref({
        search: '',
    })

    const pageInfo = ref({
        current: 1,
        size: 10,
        total: 0,
        pages: 0
    })

    const dialogEdit = ref({
        open: false,
        title: '',
        id: '',
        type: 'view',
    })

    const dialogAdd = ref(false)

    const handleSelectionChange = (val: any) => {
        multipleSelection.value = val
    }

    const handleSortChange = (column: any) => {
        console.log(column)
    }

    const handleSizeChange = (size: number) => {
        pageInfo.value.size = size
        queryPage()
    }

    const handleCurrentChange = (val: number) => {
        queryPage()
    }

    const queryPage = () => {
        loading.value = true
        http.result({
            url: '/${tableNameDown}/queryPage',
            method: 'POST',
            data: {
                current: pageInfo.value.current,
                size: pageInfo.value.size,
                search: queryParam.value.search,
            },
            success(result) {
                tableData.value = result.data.records
                pageInfo.value.current = result.data.current
                pageInfo.value.size = result.data.size
                pageInfo.value.total = result.data.total
                pageInfo.value.pages = result.data.pages
                loading.value = false
            }
        })
    }

    const remove = (id: any) => {
        http.result({
            url: '/${tableNameDown}/remove',
            method: 'POST',
            data: {
                id: id
            },
            success(result) {
                if (result.code == '200') {
                    alert('删除成功', 'success')
                }
                queryPage()
            }
        })
    }

    const openDialog = (id: string, type: string) => {
        dialogEdit.value = {
            open: true,
            title: type === 'view' ? '${tableNameUp}详情' : '编辑${tableNameUp}',
            id,
            type
        }
    }

    const closeDialog = () => {
        dialogEdit.value = {
            open: false,
            title: '',
            id: '',
            type: 'view'
        }
        queryPage()
    }

    const handleAddSuccess = () => {
        dialogAdd.value = false
        queryPage()
    }

    const confirmDelete = (id: string) => {
        confirm('提示', '确定删除该${tableNameUp}吗？', () => {
            remove(id)
        })
    }

    onMounted(() => {
        queryPage()
    })
</script>

<style scoped lang="scss">
    .page-container {
        padding: 20px;
        background-color: var(--el-bg-color-page);
    }

    .breadcrumb-wrapper {
        padding: 0 0 10px 0;

        .el-breadcrumb {
            font-size: 14px;

            :deep(.el-breadcrumb__inner) {
                display: flex;
                align-items: center;

                .el-icon {
                    margin-right: 5px;
                }
            }
        }
    }

    .custom-divider {
        margin: 10px 0 20px 0;
    }

    .content-wrapper {
        background-color: var(--el-bg-color-overlay);
        border-radius: 4px;
        box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
    }

    .search-card {
        margin-bottom: 20px;
        border-radius: 4px;

        .search-row {
            display: flex;
            align-items: center;

            .action-col {
                display: flex;
                justify-content: flex-end;
            }

            .add-button {
                width: 100%;
            }
        }
    }

    .table-card {
        border-radius: 4px;

        :deep(.el-table) {
            .el-table__header th {
                background-color: var(--el-fill-color-light);
                font-weight: 600;
            }
        }
    }

    .pagination-wrapper {
        display: flex;
        justify-content: flex-end;
        margin-top: 20px;
        padding-top: 20px;
    }

    @media (max-width: 768px) {
        .search-card .search-row {
            flex-direction: column;

            .el-col {
                width: 100%;
                margin-bottom: 15px;

                &:last-child {
                    margin-bottom: 0;
                }
            }

            .action-col {
                justify-content: flex-start;
            }
        }

        .el-table {
            :deep(.el-table__cell) {
                padding: 8px 0;
            }
        }
    }
</style>