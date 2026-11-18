<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { roleApi } from '@/api/system'
import type { PermissionGroup, RoleItem } from '@/types/system'

const loading = ref(false)
const roles = ref<RoleItem[]>([])

const createVisible = ref(false)
const submitting = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  name: '',
  code: '',
  description: '',
})
const createRules: FormRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]{1,49}$/,
      message: '以小写字母开头，仅含小写字母、数字与下划线',
      trigger: 'blur',
    },
  ],
}

const permissionVisible = ref(false)
const treeLoading = ref(false)
const savingPermissions = ref(false)
const activeRole = ref<RoleItem | null>(null)
const groups = ref<PermissionGroup[]>([])
const checkedIds = ref<number[]>([])
const totalPermissions = ref(0)

const selectedCount = computed(() => checkedIds.value.length)
const isSuperRole = computed(
  () => activeRole.value?.code === 'super_admin',
)

function roleIcon(code: string): string {
  if (code === 'super_admin') return '🛡'
  if (code === 'content_reviewer') return '🔍'
  if (code === 'operator') return '⚙️'
  return '🧩'
}

function roleTheme(code: string): string {
  if (code === 'super_admin') return 'super'
  if (code === 'content_reviewer') return 'review'
  if (code === 'operator') return 'operate'
  return 'custom'
}

async function load() {
  loading.value = true
  try {
    roles.value = await roleApi.list()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(createForm, { name: '', code: '', description: '' })
  createVisible.value = true
}

async function submitCreate() {
  if (!createFormRef.value) return
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await roleApi.create({
      name: createForm.name.trim(),
      code: createForm.code.trim(),
      description: createForm.description.trim() || undefined,
    })
    ElMessage.success('角色已创建，请为其配置权限')
    createVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function openPermissions(role: RoleItem) {
  if (role.code === 'super_admin') {
    ElMessage.info('超级管理员默认拥有全部权限，无需配置')
    return
  }
  activeRole.value = role
  permissionVisible.value = true
  treeLoading.value = true
  groups.value = []
  checkedIds.value = []
  try {
    const tree = await roleApi.tree(role.id)
    groups.value = tree.groups
    totalPermissions.value = tree.total
    checkedIds.value = tree.groups.flatMap((group) =>
      group.permissions.filter((item) => item.checked).map((item) => item.id),
    )
  } finally {
    treeLoading.value = false
  }
}

function groupIds(group: PermissionGroup): number[] {
  return group.permissions.map((item) => item.id)
}

function groupAllChecked(group: PermissionGroup): boolean {
  const ids = groupIds(group)
  return ids.length > 0 && ids.every((id) => checkedIds.value.includes(id))
}

function groupIndeterminate(group: PermissionGroup): boolean {
  const selected = groupIds(group).filter((id) => checkedIds.value.includes(id)).length
  return selected > 0 && selected < group.permissions.length
}

function toggleGroup(group: PermissionGroup, checked: boolean) {
  const ids = groupIds(group)
  if (checked) {
    checkedIds.value = Array.from(new Set([...checkedIds.value, ...ids]))
  } else {
    checkedIds.value = checkedIds.value.filter((id) => !ids.includes(id))
  }
}

function togglePermission(id: number, checked: boolean) {
  if (checked) {
    if (!checkedIds.value.includes(id)) checkedIds.value.push(id)
  } else {
    checkedIds.value = checkedIds.value.filter((item) => item !== id)
  }
}

function onGroupChange(group: PermissionGroup, value: string | number | boolean) {
  toggleGroup(group, Boolean(value))
}

function onPermissionChange(id: number, value: string | number | boolean) {
  togglePermission(id, Boolean(value))
}

async function savePermissions() {
  if (!activeRole.value) return
  savingPermissions.value = true
  try {
    await roleApi.savePermissions(activeRole.value.id, checkedIds.value)
    ElMessage.success(`已保存「${activeRole.value.name}」的权限配置`)
    permissionVisible.value = false
    await load()
  } finally {
    savingPermissions.value = false
  }
}

async function removeRole(role: RoleItem) {
  if (role.code === 'super_admin' || role.userCount > 0) return
  const confirmed = await ElMessageBox.confirm(
    `确定删除角色「${role.name}」吗？该角色下的权限配置将一并清除。`,
    '删除角色',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' },
  ).catch(() => false)
  if (!confirmed) return
  await roleApi.remove(role.id)
  ElMessage.success('角色已删除')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="role-page">
    <PageHeader title="角色管理" description="配置后台角色及其权限点，保存后对角色下全部用户立即生效。">
      <template #actions>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          <span>新增角色</span>
        </el-button>
      </template>
    </PageHeader>

    <div v-loading="loading" class="role-grid">
      <article
        v-for="role in roles"
        :key="role.id"
        class="role-card"
        :class="roleTheme(role.code)"
        @click="openPermissions(role)"
      >
        <div class="role-top">
          <span class="role-icon">{{ roleIcon(role.code) }}</span>
          <el-tag v-if="role.code === 'super_admin'" effect="plain" size="small">系统内置</el-tag>
        </div>
        <h3>{{ role.name }}</h3>
        <div class="role-code">{{ role.code }} · {{ role.userCount }} 个用户</div>
        <p class="role-desc">{{ role.description || '暂无角色描述' }}</p>
        <div class="role-tags">
          <el-tag
            v-for="label in role.permissionLabels.slice(0, 4)"
            :key="label"
            size="small"
            effect="plain"
            class="perm-tag"
          >
            {{ label }}
          </el-tag>
          <el-tag v-if="role.permissionLabels.length > 4" size="small" effect="plain" class="perm-tag">
            +{{ role.permissionLabels.length - 4 }}
          </el-tag>
        </div>
        <div class="role-foot" @click.stop>
          <el-button
            size="small"
            plain
            :disabled="role.code === 'super_admin'"
            @click="openPermissions(role)"
          >
            <el-icon><Setting /></el-icon>
            <span>编辑权限</span>
          </el-button>
          <el-tooltip
            :disabled="role.code !== 'super_admin' && role.userCount === 0"
            :content="role.code === 'super_admin' ? '系统内置角色不可删除' : `该角色下还有 ${role.userCount} 个用户，无法删除`"
            placement="top"
          >
            <span class="delete-wrap">
              <el-button
                size="small"
                text
                type="danger"
                :disabled="role.code === 'super_admin' || role.userCount > 0"
                @click="removeRole(role)"
              >
                <el-icon><Delete /></el-icon>
                <span>删除</span>
              </el-button>
            </span>
          </el-tooltip>
        </div>
      </article>

      <el-empty
        v-if="!loading && roles.length === 0"
        class="role-empty"
        description="暂无角色数据，点击右上角新增角色"
      />
    </div>

    <el-dialog v-model="createVisible" title="新增角色" width="500px" destroy-on-close>
      <p class="dialog-tip">角色创建后可在权限编辑中配置权限点。</p>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="createForm.name" maxlength="50" placeholder="如：内容运营" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="createForm.code" maxlength="50" placeholder="如：content_operator" />
          <div class="form-hint">用于系统识别，需保持唯一，仅支持小写字母、数字与下划线。</div>
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="简要说明该角色的职责范围（选填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">
          <el-icon><Close /></el-icon>
          <span>取消</span>
        </el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">
          <el-icon><Check /></el-icon>
          <span>创建角色</span>
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="permissionVisible"
      :title="`编辑权限 - ${activeRole?.name ?? ''}`"
      width="760px"
      destroy-on-close
    >
      <div class="perm-head">
        <span>勾选权限点，保存后全量覆盖该角色原有权限</span>
        <span class="perm-count">已选 {{ selectedCount }} 项 / 共 {{ totalPermissions }} 项</span>
      </div>
      <div v-loading="treeLoading" class="perm-tree">
        <section v-for="group in groups" :key="group.id" class="perm-group">
          <header class="perm-group-head">
            <el-checkbox
              :model-value="groupAllChecked(group)"
              :indeterminate="groupIndeterminate(group)"
              @change="onGroupChange(group, $event)"
            >
              <b>{{ group.name }}</b>
            </el-checkbox>
            <span>{{ group.permissions.length }} 项</span>
          </header>
          <div class="perm-points">
            <el-checkbox
              v-for="item in group.permissions"
              :key="item.id"
              :model-value="checkedIds.includes(item.id)"
              @change="onPermissionChange(item.id, $event)"
            >
              {{ item.name }}
            </el-checkbox>
          </div>
        </section>
      </div>
      <template #footer>
        <el-button @click="permissionVisible = false">
          <el-icon><Close /></el-icon>
          <span>取消</span>
        </el-button>
        <el-button
          type="primary"
          :loading="savingPermissions"
          :disabled="isSuperRole"
          @click="savePermissions"
        >
          <el-icon><Check /></el-icon>
          <span>保存权限</span>
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.role-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.role-grid {
  display: grid;
  min-height: 180px;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.role-card {
  display: flex;
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 16px;
  flex-direction: column;
  background: var(--surface);
  cursor: pointer;
  transition:
    border-color 0.18s,
    box-shadow 0.18s,
    transform 0.18s;
}

.role-card:hover {
  border-color: #c7d2fe;
  box-shadow: 0 14px 32px rgba(79, 70, 229, 0.1);
  transform: translateY(-2px);
}

.role-card.super {
  background:
    linear-gradient(180deg, rgba(79, 70, 229, 0.07), transparent 42%),
    var(--surface);
}

.role-top {
  display: flex;
  min-height: 34px;
  align-items: flex-start;
  justify-content: space-between;
}

.role-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 12px;
  background: var(--brand-soft);
  font-size: 19px;
}

.role-card.review .role-icon {
  background: #f0f9ff;
}

.role-card.operate .role-icon {
  background: #ecfdf5;
}

.role-card.custom .role-icon {
  background: #fff7ed;
}

.role-card h3 {
  margin: 14px 0 4px;
  font-size: 17px;
}

.role-code {
  color: var(--muted);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
}

.role-desc {
  display: -webkit-box;
  min-height: 38px;
  margin: 12px 0;
  overflow: hidden;
  color: var(--text-2);
  font-size: 13px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.role-tags {
  display: flex;
  min-height: 48px;
  align-content: flex-start;
  gap: 6px;
  flex-wrap: wrap;
}

.perm-tag {
  max-width: 100%;
}

.role-foot {
  display: flex;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
  align-items: center;
  justify-content: space-between;
}

.delete-wrap {
  display: inline-flex;
}

.role-empty {
  grid-column: 1 / -1;
  border: 1px dashed var(--line);
  border-radius: 16px;
  background: var(--surface);
}

.dialog-tip {
  margin: 0 0 18px;
  color: var(--muted);
  font-size: 12.5px;
}

.form-hint {
  margin-top: 5px;
  color: var(--muted-2);
  font-size: 11.5px;
  line-height: 1.6;
}

.perm-head {
  display: flex;
  margin-bottom: 14px;
  align-items: center;
  justify-content: space-between;
  color: var(--muted);
  font-size: 12.5px;
}

.perm-count {
  color: var(--brand);
  font-weight: 650;
}

.perm-tree {
  display: grid;
  max-height: 58vh;
  padding-right: 4px;
  overflow-y: auto;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.perm-group {
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--surface-2);
}

.perm-group-head {
  display: flex;
  padding-bottom: 8px;
  border-bottom: 1px dashed var(--line);
  align-items: center;
  justify-content: space-between;
  color: var(--muted);
  font-size: 12px;
}

.perm-points {
  display: flex;
  padding-top: 8px;
  flex-direction: column;
  gap: 2px;
}

.perm-points :deep(.el-checkbox) {
  height: 28px;
}

@media (max-width: 720px) {
  .perm-tree {
    grid-template-columns: 1fr;
  }
}
</style>
