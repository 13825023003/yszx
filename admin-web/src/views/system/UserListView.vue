<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { userApi } from '@/api/system'
import type { AdminUserDetail, AdminUserItem, RoleOption } from '@/types/system'
import { useSettingStore } from '@/store/setting'

const settingStore = useSettingStore()

const loading = ref(false)
const list = ref<AdminUserItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const selection = ref<AdminUserItem[]>([])
const roleOptions = ref<RoleOption[]>([])

const filters = reactive<{
  keyword: string
  roleId: number | undefined
  status: number | undefined
}>({
  keyword: '',
  roleId: undefined,
  status: undefined,
})

const selectedIds = computed(() => selection.value.map((item) => item.id))
const assignableRoles = computed(() => roleOptions.value.filter((item) => item.assignable))

const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const editingBuiltin = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  mobile: '',
  email: '',
  roleId: undefined as number | undefined,
  status: 1,
  password: '',
})

const formRules = computed<FormRules>(() => ({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度需为 2~50 个字符', trigger: 'blur' },
  ],
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: formMode.value === 'create'
    ? [
        { required: true, message: '请输入登录密码', trigger: 'blur' },
        { min: 6, max: 72, message: '密码长度需为 6~72 个字符', trigger: 'blur' },
      ]
    : [],
}))

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AdminUserDetail | null>(null)

function isBuiltin(row: AdminUserItem): boolean {
  return row.isBuiltin === 1
}

function avatarSrc(row: { avatar: string | null }): string {
  return row.avatar || settingStore.defaultAvatar || ''
}

function statusText(status: number): string {
  return status === 1 ? '已启用' : '已禁用'
}

function roleTagType(code: string): 'primary' | 'success' | 'info' | 'warning' {
  if (code === 'super_admin') return 'primary'
  if (code === 'content_reviewer') return 'info'
  if (code === 'operator') return 'success'
  return 'warning'
}

async function load() {
  loading.value = true
  try {
    const data = await userApi.list({
      keyword: filters.keyword.trim() || undefined,
      roleId: filters.roleId,
      status: filters.status,
      page: page.value,
      pageSize: pageSize.value,
    })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadRoleOptions() {
  roleOptions.value = await userApi.roleOptions()
}

function search() {
  page.value = 1
  load()
}

function resetFilters() {
  filters.keyword = ''
  filters.roleId = undefined
  filters.status = undefined
  page.value = 1
  load()
}

function handleSelectionChange(rows: AdminUserItem[]) {
  selection.value = rows
}

function selectable(row: AdminUserItem): boolean {
  return !isBuiltin(row)
}

function openCreate() {
  formMode.value = 'create'
  editingId.value = null
  editingBuiltin.value = false
  Object.assign(form, {
    username: '',
    mobile: '',
    email: '',
    roleId: undefined,
    status: 1,
    password: '',
  })
  formVisible.value = true
}

function openEdit(row: AdminUserItem) {
  formMode.value = 'edit'
  editingId.value = row.id
  editingBuiltin.value = isBuiltin(row)
  Object.assign(form, {
    username: row.username,
    mobile: row.mobile ?? '',
    email: row.email ?? '',
    roleId: row.roleId,
    status: row.status,
    password: '',
  })
  formVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (formMode.value === 'create') {
      await userApi.create({
        username: form.username.trim(),
        password: form.password,
        mobile: form.mobile.trim(),
        email: form.email.trim() || null,
        roleId: form.roleId as number,
        status: form.status,
      })
      ElMessage.success('用户已创建')
    } else if (editingId.value) {
      await userApi.update(editingId.value, {
        username: form.username.trim(),
        mobile: form.mobile.trim(),
        email: form.email.trim() || null,
        roleId: form.roleId as number,
        status: form.status,
      })
      ElMessage.success('用户已更新')
    }
    formVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function openDetail(row: AdminUserItem) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await userApi.detail(row.id)
  } finally {
    detailLoading.value = false
  }
}

async function toggleStatus(row: AdminUserItem) {
  const nextStatus = row.status === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '禁用'
  const confirmed = await ElMessageBox.confirm(
    `确定${action}用户「${row.username}」吗？`,
    `${action}用户`,
    { confirmButtonText: `确认${action}`, cancelButtonText: '取消', type: 'warning' },
  ).catch(() => false)
  if (!confirmed) return
  await userApi.updateStatus([row.id], nextStatus)
  ElMessage.success(`用户已${action}`)
  await load()
}

async function resetPassword(row: AdminUserItem) {
  const confirmed = await ElMessageBox.confirm(
    `确定将用户「${row.username}」的密码重置为部署配置的默认密码吗？重置后请及时通知本人修改。`,
    '重置密码',
    { confirmButtonText: '确认重置', cancelButtonText: '取消', type: 'warning' },
  ).catch(() => false)
  if (!confirmed) return
  const data = await userApi.resetPassword(row.id)
  await ElMessageBox.alert(
    `密码已重置为：${data.password}\n请及时通知该用户登录后修改密码。`,
    '重置成功',
    { confirmButtonText: '知道了', type: 'success' },
  )
  await load()
}

async function removeUser(row: AdminUserItem) {
  const confirmed = await ElMessageBox.confirm(
    `确定删除后台用户「${row.username}」吗？删除后该账号将无法登录管理后台。`,
    '删除用户',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' },
  ).catch(() => false)
  if (!confirmed) return
  await userApi.remove(row.id)
  ElMessage.success('用户已删除')
  await load()
}

async function batchStatus(status: number) {
  if (!selectedIds.value.length) return
  const action = status === 1 ? '启用' : '禁用'
  const confirmed = await ElMessageBox.confirm(
    `确定批量${action}选中的 ${selectedIds.value.length} 个用户吗？`,
    `批量${action}`,
    { confirmButtonText: `确认${action}`, cancelButtonText: '取消', type: 'warning' },
  ).catch(() => false)
  if (!confirmed) return
  await userApi.updateStatus(selectedIds.value, status)
  ElMessage.success(`已${action} ${selectedIds.value.length} 个用户`)
  await load()
}

async function batchRemove() {
  if (!selectedIds.value.length) return
  const confirmed = await ElMessageBox.confirm(
    `确定删除选中的 ${selectedIds.value.length} 个后台用户吗？该操作不可恢复。`,
    '批量删除用户',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' },
  ).catch(() => false)
  if (!confirmed) return
  await userApi.batchRemove(selectedIds.value)
  ElMessage.success(`已删除 ${selectedIds.value.length} 个用户`)
  await load()
}

onMounted(async () => {
  await Promise.all([load(), loadRoleOptions(), settingStore.load()])
})
</script>

<template>
  <div class="user-page">
    <PageHeader title="用户管理" description="管理后台内部账号，超级管理员为系统内置唯一账号。">
      <template #actions>
        <el-button v-permission="'user:add'" type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          <span>新增用户</span>
        </el-button>
      </template>
    </PageHeader>

    <section class="panel">
      <div class="toolbar">
        <el-input
          v-model="filters.keyword"
          class="keyword-input"
          clearable
          placeholder="搜索用户名 / 手机号"
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select
          v-model="filters.roleId"
          class="filter-select"
          clearable
          placeholder="全部角色"
          @change="search"
        >
          <el-option
            v-for="role in roleOptions"
            :key="role.id"
            :label="role.name"
            :value="role.id"
          />
        </el-select>
        <el-select
          v-model="filters.status"
          class="filter-select"
          clearable
          placeholder="全部状态"
          @change="search"
        >
          <el-option label="已启用" :value="1" />
          <el-option label="已禁用" :value="0" />
        </el-select>
        <el-button @click="search">
          <el-icon><Search /></el-icon>
          <span>查询</span>
        </el-button>
        <el-button text @click="resetFilters">
          <el-icon><RefreshLeft /></el-icon>
          <span>重置</span>
        </el-button>
        <span class="toolbar-hint">共 {{ total }} 个后台用户</span>
      </div>

      <transition name="el-fade-in">
        <div v-if="selection.length" class="batch-bar">
          <span>已选择 <b>{{ selection.length }}</b> 个用户</span>
          <div class="batch-actions">
            <el-button v-permission="'user:edit'" size="small" plain @click="batchStatus(1)">
              <el-icon><CircleCheck /></el-icon>
              <span>批量启用</span>
            </el-button>
            <el-button v-permission="'user:edit'" size="small" plain @click="batchStatus(0)">
              <el-icon><CircleClose /></el-icon>
              <span>批量禁用</span>
            </el-button>
            <el-button
              v-permission="'user:disable'"
              size="small"
              type="danger"
              plain
              @click="batchRemove"
            >
              <el-icon><Delete /></el-icon>
              <span>批量删除</span>
            </el-button>
          </div>
        </div>
      </transition>

      <el-table
        v-loading="loading"
        :data="list"
        row-key="id"
        empty-text="没有找到匹配的用户"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="46" :selectable="selectable" />
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="36" :src="avatarSrc(row)" class="user-avatar">
                {{ row.username.slice(0, 1).toUpperCase() }}
              </el-avatar>
              <div class="user-meta">
                <div class="user-name">
                  {{ row.username }}
                  <el-tag v-if="isBuiltin(row)" size="small" effect="plain">内置</el-tag>
                </div>
                <div class="user-sub">{{ row.email || '未填写邮箱' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="mobile" label="手机号" width="130" />
        <el-table-column label="角色" width="115">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.roleCode)" effect="light">{{ row.roleName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="status-dot" :class="{ off: row.status !== 1 }" />
            {{ statusText(row.status) }}
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="160">
          <template #default="{ row }">
            <span class="muted-text">{{ row.lastLoginAt || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="375" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="primary" @click="openDetail(row)">
                <el-icon><View /></el-icon>
                <span>详情</span>
              </el-button>
              <el-button v-permission="'user:edit'" link type="primary" @click="openEdit(row)">
                <el-icon><Edit /></el-icon>
                <span>编辑</span>
              </el-button>
              <el-button
                v-permission="'user:edit'"
                link
                type="primary"
                @click="resetPassword(row)"
              >
                <el-icon><Key /></el-icon>
                <span>重置密码</span>
              </el-button>
              <template v-if="!isBuiltin(row)">
                <el-button
                  v-permission="'user:edit'"
                  link
                  :type="row.status === 1 ? 'warning' : 'success'"
                  @click="toggleStatus(row)"
                >
                  <el-icon>
                    <Lock v-if="row.status === 1" />
                    <Unlock v-else />
                  </el-icon>
                  <span>{{ row.status === 1 ? '禁用' : '启用' }}</span>
                </el-button>
                <el-button
                  v-permission="'user:disable'"
                  link
                  type="danger"
                  @click="removeUser(row)"
                >
                  <el-icon><Delete /></el-icon>
                  <span>删除</span>
                </el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="search"
        />
      </div>
    </section>

    <el-dialog
      v-model="formVisible"
      :title="formMode === 'create' ? '新增用户' : '编辑用户'"
      width="540px"
      destroy-on-close
    >
      <p class="dialog-tip">
        {{ editingBuiltin ? '内置超级管理员账号，角色与状态不可修改。' : '后台账号用于登录运营后台，请妥善分配角色。' }}
      </p>
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" maxlength="50" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="手机号" prop="mobile">
            <el-input v-model="form.mobile" maxlength="11" placeholder="11 位手机号" />
          </el-form-item>
        </div>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" maxlength="100" placeholder="选填" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="角色" prop="roleId">
            <el-select v-model="form.roleId" :disabled="editingBuiltin" placeholder="请选择角色">
              <el-option
                v-for="role in editingBuiltin ? roleOptions.filter((item) => !item.assignable) : assignableRoles"
                :key="role.id"
                :label="role.name"
                :value="role.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <div class="status-switch">
              <el-switch
                v-model="form.status"
                :active-value="1"
                :inactive-value="0"
                :disabled="editingBuiltin"
              />
              <span>{{ form.status === 1 ? '已启用' : '已禁用' }}</span>
            </div>
          </el-form-item>
        </div>
        <el-form-item v-if="formMode === 'create'" label="登录密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            maxlength="72"
            placeholder="至少 6 位"
            autocomplete="new-password"
          />
        </el-form-item>
        <p v-else class="dialog-hint">如需修改密码，请在列表中对该用户执行「重置密码」。</p>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">
          <el-icon><Close /></el-icon>
          <span>取消</span>
        </el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          <el-icon><Check /></el-icon>
          <span>保存</span>
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="用户详情" width="620px" destroy-on-close>
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-head">
            <el-avatar :size="56" :src="avatarSrc(detail)" class="user-avatar">
              {{ detail.username.slice(0, 1).toUpperCase() }}
            </el-avatar>
            <div class="detail-head-main">
              <div class="detail-title">
                {{ detail.username }}
                <el-tag :type="roleTagType(detail.roleCode)" effect="light">{{ detail.roleName }}</el-tag>
                <el-tag :type="detail.status === 1 ? 'success' : 'danger'" effect="light">
                  {{ statusText(detail.status) }}
                </el-tag>
                <el-tag v-if="isBuiltin(detail)" effect="plain">系统内置</el-tag>
              </div>
              <div class="user-sub">最后登录 {{ detail.lastLoginAt || '—' }}</div>
            </div>
          </div>

          <div class="detail-grid">
            <div class="detail-item"><span>用户名</span><b>{{ detail.username }}</b></div>
            <div class="detail-item"><span>手机号</span><b>{{ detail.mobile || '—' }}</b></div>
            <div class="detail-item"><span>邮箱</span><b>{{ detail.email || '—' }}</b></div>
            <div class="detail-item"><span>创建时间</span><b>{{ detail.createdAt || '—' }}</b></div>
          </div>

          <div class="recent-block">
            <div class="block-label">最近操作</div>
            <div v-if="detail.recentLogs.length" class="log-list">
              <div v-for="log in detail.recentLogs" :key="log.id" class="log-item">
                <span class="log-dot" />
                <div class="log-main">
                  <div><b>{{ log.action }}</b> · {{ log.targetName }}</div>
                  <div class="log-sub">{{ log.createdAt }} · {{ log.ip }}</div>
                </div>
              </div>
            </div>
            <el-empty v-else :image-size="54" description="该用户暂无操作记录" />
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">
          <el-icon><Close /></el-icon>
          <span>关闭</span>
        </el-button>
        <el-button
          v-if="detail && !isBuiltin(detail)"
          v-permission="'user:edit'"
          type="primary"
          @click="detailVisible = false; openEdit(detail)"
        >
          <el-icon><Edit /></el-icon>
          <span>编辑用户</span>
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel {
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--surface);
}

.toolbar {
  display: flex;
  margin-bottom: 16px;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.keyword-input {
  width: 240px;
}

.filter-select {
  width: 150px;
}

.toolbar-hint {
  margin-left: auto;
  color: var(--muted);
  font-size: 12.5px;
}

.batch-bar {
  display: flex;
  margin-bottom: 14px;
  padding: 10px 14px;
  border: 1px solid #dcdafa;
  border-radius: 10px;
  align-items: center;
  background: var(--brand-soft);
  color: var(--text-2);
  font-size: 13px;
}

.batch-bar b {
  color: var(--brand);
}

.batch-actions {
  display: flex;
  margin-left: auto;
  gap: 8px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 11px;
}

.user-avatar {
  flex: none;
  background: var(--grad);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.user-meta {
  min-width: 0;
}

.user-name {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13.5px;
  font-weight: 650;
}

.user-sub {
  margin-top: 2px;
  overflow: hidden;
  color: var(--muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  margin-right: 6px;
  border-radius: 50%;
  background: var(--success);
}

.status-dot.off {
  background: var(--danger);
}

.muted-text {
  color: var(--muted);
  font-size: 12.5px;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 2px 8px;
  white-space: nowrap;
}

.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.pager {
  display: flex;
  margin-top: 16px;
  justify-content: flex-end;
}

.dialog-tip {
  margin: 0 0 18px;
  color: var(--muted);
  font-size: 12.5px;
  line-height: 1.7;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.status-switch {
  display: flex;
  height: 32px;
  align-items: center;
  gap: 10px;
  color: var(--muted);
  font-size: 13px;
}

.dialog-hint {
  margin: 0;
  color: var(--muted-2);
  font-size: 12px;
}

.detail-body {
  min-height: 120px;
}

.detail-head {
  display: flex;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line);
  align-items: center;
  gap: 14px;
}

.detail-head-main {
  min-width: 0;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 16px;
  font-weight: 700;
}

.detail-grid {
  display: grid;
  margin: 18px 0;
  grid-template-columns: 1fr 1fr;
  gap: 12px 24px;
}

.detail-item {
  display: flex;
  padding: 10px 12px;
  border-radius: 9px;
  justify-content: space-between;
  gap: 14px;
  background: var(--surface-2);
  font-size: 12.5px;
}

.detail-item span {
  color: var(--muted);
}

.detail-item b {
  overflow: hidden;
  color: var(--text-2);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.block-label {
  margin-bottom: 8px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 650;
  letter-spacing: 0.03em;
}

.log-list {
  max-height: 240px;
  overflow-y: auto;
}

.log-item {
  display: flex;
  padding: 9px 0;
  border-bottom: 1px dashed var(--line);
  align-items: flex-start;
  gap: 10px;
  font-size: 12.5px;
}

.log-item:last-child {
  border-bottom: 0;
}

.log-dot {
  width: 6px;
  height: 6px;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--brand);
  flex: none;
}

.log-main {
  min-width: 0;
}

.log-sub {
  margin-top: 3px;
  color: var(--muted-2);
  font-size: 11.5px;
}

@media (max-width: 760px) {
  .form-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .keyword-input,
  .filter-select {
    width: 100%;
  }

  .toolbar-hint {
    width: 100%;
    margin-left: 0;
  }
}
</style>
