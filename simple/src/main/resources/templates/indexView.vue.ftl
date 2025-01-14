<template>
    <el-divider />
    <div v-loading="loading">
        <el-card>
            <el-row :gutter="20">
                <el-col>
                    <el-input v-model="queryParam.search" style="width: 100%" size="large" placeholder="搜索"
                              @keyup.enter="queryPage" :prefix-icon="Search">
                    </el-input>
                </el-col>
                <el-col>
                    <el-button type="primary" plain @click="dialogAdd = true">新建</el-button>
                </el-col>
            </el-row>
        </el-card>
        <el-table :data="tableData" style="width: 100%;min-height: 30vh;" stripe border
                  @selection-change="handleSelectionChange" @sort-change="handleSortChange"
                  :default-sort="{ prop: 'date', order: 'descending' }">
            <el-table-column type="selection" />
            <#list fieldInfos as field>
                <el-table-column prop="${field.getFieldName()}" label="${field.getComment()}" />
            </#list>
            <el-table-column fixed="right" label="操作" min-width="120">
                <template #default="scope">
                    <el-button link type="primary" size="small"
                               @click="() => { dialogEdit.open = true; dialogEdit.title = '详情'; dialogEdit.id = scope.row.id; dialogEdit.type = 'view'; }">
                        详情
                    </el-button>
                    <el-button link type="primary" size="small"
                               @click="() => { dialogEdit.open = true; dialogEdit.title = '编辑'; dialogEdit.id = scope.row.id; dialogEdit.type = 'edit'; }">
                        编辑
                    </el-button>
                    <el-button link type="danger" size="small" @click="() => {
                        confirm('提示', '确定删除吗', () => {
                            remove(scope.row.id)
                        })
                    }">
                        删除
                    </el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination layout="prev, pager, next, jumper, total" :total="pageInfo.total" :size="pageInfo.size"
                       v-model:current-page="pageInfo.current" @current-change="handleCurrentChange" />
    </div>
    <el-dialog v-model="dialogEdit.open" :title="dialogEdit.title" width="800" center
               @closed="() => { dialogEdit.id = ''; queryPage() }">
        <${tableNameUp}InfoView v-model:id="dialogEdit.id" v-model:type="dialogEdit.type" :key="dialogEdit.id" />
    </el-dialog>
    <el-dialog v-model="dialogAdd" title="添加" width="400" center @closed="() => { queryPage(); }">
        <${tableNameUp}AddView @success="dialogAdd = false; queryPage();" />
    </el-dialog>

</template>

<script setup lang='ts'>
    import { http, alert, confirm } from '@/utils';
    import { onMounted, ref } from 'vue';
    import { Search } from '@element-plus/icons-vue'
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
        console.log(val)
    }

    const handleSortChange = (column: any) => {
        console.log(column)
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


    onMounted(() => {

        queryPage()
    })

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

</script>

<style scoped>
    .el-col {
        margin-top: 1rem;
    }
</style>