<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>菜单管理</span>
          <div class="header-actions">
            <el-button @click="fetchTree">刷新</el-button>
            <el-button type="primary" @click="openCreateRoot">新增一级菜单</el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="tree"
        v-loading="loading"
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="menuName" label="名称" min-width="180" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.menuType === 0 ? 'info' : row.menuType === 1 ? 'success' : 'warning'">
              {{ row.menuType === 0 ? '目录' : row.menuType === 1 ? '菜单' : '按钮' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路径" min-width="200" />
        <el-table-column prop="component" label="组件" min-width="200" />
        <el-table-column prop="permission" label="权限标识" min-width="180" />
        <el-table-column prop="icon" label="图标" width="120" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="可见" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.visible === 1 || row.visible === null || row.visible === undefined ? 'success' : 'info'">
              {{ row.visible === 0 ? '否' : '是' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreateChild(row)">新增子项</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增菜单' : '编辑菜单'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="父菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentTree"
            :props="{ value: 'id', label: 'menuName', children: 'children' }"
            check-strictly
            clearable
            placeholder="选择父菜单（不选则为一级）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio :value="0">目录</el-radio>
            <el-radio :value="1">菜单</el-radio>
            <el-radio :value="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路径" prop="path" v-if="form.menuType !== 2">
          <el-input v-model="form.path" placeholder="例如 /system/accounts" />
        </el-form-item>
        <el-form-item label="组件" prop="component" v-if="form.menuType === 1">
          <el-input v-model="form.component" placeholder="例如 pages/system/Accounts" />
        </el-form-item>
        <el-form-item label="权限" prop="permission">
          <el-input v-model="form.permission" placeholder="例如 system:accounts" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="Element Plus Icon 名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width: 160px" />
        </el-form-item>
        <el-form-item label="可见" prop="visible" v-if="dialogMode === 'edit'">
          <el-radio-group v-model="form.visible">
            <el-radio :value="1">是</el-radio>
            <el-radio :value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api/client'

const loading = ref(false)
const saving = ref(false)
const tree = ref([])

const dialogVisible = ref(false)
const dialogMode = ref('create')
const formRef = ref()
const form = reactive({
  id: null,
  parentId: 0,
  menuName: '',
  menuType: 0,
  path: '',
  component: '',
  permission: '',
  icon: '',
  sortOrder: 0,
  visible: 1
})

const rules = {
  menuName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function normalizeParentId(value) {
  if (value === null || value === undefined || value === '') return 0
  return value
}

function resetForm() {
  form.id = null
  form.parentId = 0
  form.menuName = ''
  form.menuType = 0
  form.path = ''
  form.component = ''
  form.permission = ''
  form.icon = ''
  form.sortOrder = 0
  form.visible = 1
}

const parentTree = computed(() => {
  return tree.value || []
})

async function fetchTree() {
  loading.value = true
  try {
    const resp = await api.get('/api/system/menus/tree')
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '加载失败')
    tree.value = body.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreateRoot() {
  dialogMode.value = 'create'
  resetForm()
  form.parentId = 0
  dialogVisible.value = true
}

function openCreateChild(row) {
  dialogMode.value = 'create'
  resetForm()
  form.parentId = row.id
  dialogVisible.value = true
}

function openEdit(row) {
  dialogMode.value = 'edit'
  resetForm()
  form.id = row.id
  form.parentId = row.parentId ?? 0
  form.menuName = row.menuName || ''
  form.menuType = row.menuType ?? 0
  form.path = row.path || ''
  form.component = row.component || ''
  form.permission = row.permission || ''
  form.icon = row.icon || ''
  form.sortOrder = row.sortOrder ?? 0
  form.visible = row.visible ?? 1
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      parentId: normalizeParentId(form.parentId),
      menuName: form.menuName,
      menuType: form.menuType,
      path: form.menuType === 2 ? null : form.path,
      component: form.menuType === 1 ? form.component : null,
      permission: form.permission || null,
      icon: form.icon || null,
      sortOrder: form.sortOrder
    }

    if (dialogMode.value === 'create') {
      const resp = await api.post('/api/system/menus', payload)
      const body = resp.data
      if (body.code !== 200) throw new Error(body.msg || '创建失败')
      ElMessage.success('创建成功')
    } else {
      const resp = await api.put(`/api/system/menus/${form.id}`, {
        ...payload,
        visible: form.visible
      })
      const body = resp.data
      if (body.code !== 200) throw new Error(body.msg || '更新失败')
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    await fetchTree()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除菜单【${row.menuName}】吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const resp = await api.delete(`/api/system/menus/${row.id}`)
    const body = resp.data
    if (body.code !== 200) throw new Error(body.msg || '删除失败')
    ElMessage.success('删除成功')
    await fetchTree()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(e.message || '删除失败')
  }
}

onMounted(() => {
  fetchTree()
})
</script>

<style scoped>
.page {
  padding: 16px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
