<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { subjectApi } from '@/api/subject'
import type { StageItem, SubjectItem, SubjectStats } from '@/types/subject'

const COLORS = ['#6366f1', '#8b5cf6', '#0ea5e9', '#14b8a6', '#10b981', '#f59e0b', '#ec4899', '#f43f5e']

const loading = ref(false)
const sorting = ref(false)
const subjects = ref<SubjectItem[]>([])
const stats = ref<SubjectStats>({
  totalSubjects: 0,
  subjectsWithStages: 0,
  totalStages: 0,
  totalQuestions: 0,
})

const statCards = computed(() => [
  {
    label: '总学科数',
    value: stats.value.totalSubjects,
    icon: 'Collection',
    accent: '#4f46e5',
    hint: '内容体系根节点',
  },
  {
    label: '有阶段的学科',
    value: stats.value.subjectsWithStages,
    icon: 'Grid',
    accent: '#0ea5e9',
    hint: '已配置学习阶段',
  },
  {
    label: '阶段总数',
    value: stats.value.totalStages,
    icon: 'Guide',
    accent: '#7c3aed',
    hint: '全部学科阶段',
  },
  {
    label: '题目总数',
    value: stats.value.totalQuestions,
    icon: 'EditPen',
    accent: '#12b76a',
    hint: '题库实时统计',
  },
])

const subjectDialogVisible = ref(false)
const subjectMode = ref<'create' | 'edit'>('create')
const editingSubjectId = ref<number | null>(null)
const subjectSubmitting = ref(false)
const subjectFormRef = ref<FormInstance>()
const subjectForm = reactive({
  name: '',
  icon: '📘',
  description: '',
  sortOrder: 1,
  themeColor: COLORS[0],
})
const subjectRules: FormRules = {
  name: [
    { required: true, message: '请输入学科名称', trigger: 'blur' },
    { max: 50, message: '学科名称不能超过 50 个字符', trigger: 'blur' },
  ],
  icon: [
    { required: true, message: '请输入学科图标', trigger: 'blur' },
    { max: 50, message: '学科图标长度不正确', trigger: 'blur' },
  ],
  description: [
    { required: true, message: '请输入学科描述', trigger: 'blur' },
    { max: 255, message: '学科描述不能超过 255 个字符', trigger: 'blur' },
  ],
  sortOrder: [{ required: true, message: '请输入排序号', trigger: 'blur' }],
  themeColor: [{ required: true, message: '请选择主题颜色', trigger: 'change' }],
}

const stageDialogVisible = ref(false)
const stageMode = ref<'create' | 'edit'>('create')
const stageSubject = ref<SubjectItem | null>(null)
const editingStageId = ref<number | null>(null)
const stageSubmitting = ref(false)
const stageFormRef = ref<FormInstance>()
const stageForm = reactive({
  name: '',
  icon: '',
  sortOrder: 1,
})
const stageRules: FormRules = {
  name: [
    { required: true, message: '请输入阶段名称', trigger: 'blur' },
    { max: 50, message: '阶段名称不能超过 50 个字符', trigger: 'blur' },
  ],
}

async function load() {
  loading.value = true
  try {
    const [statsData, listData] = await Promise.all([subjectApi.stats(), subjectApi.list()])
    stats.value = statsData
    subjects.value = listData
  } finally {
    loading.value = false
  }
}

function subjectSoftColor(color: string): string {
  return `${color || '#6366f1'}16`
}

function stageIcon(stage: StageItem): string {
  return stage.icon || '📘'
}

function openCreateSubject() {
  subjectMode.value = 'create'
  editingSubjectId.value = null
  Object.assign(subjectForm, {
    name: '',
    icon: '📘',
    description: '',
    sortOrder: subjects.value.length + 1,
    themeColor: COLORS[subjects.value.length % COLORS.length],
  })
  subjectDialogVisible.value = true
}

function openEditSubject(subject: SubjectItem) {
  subjectMode.value = 'edit'
  editingSubjectId.value = subject.id
  Object.assign(subjectForm, {
    name: subject.name,
    icon: subject.icon,
    description: subject.description,
    sortOrder: subject.sortOrder,
    themeColor: subject.themeColor || COLORS[0],
  })
  subjectDialogVisible.value = true
}

async function submitSubject() {
  if (!subjectFormRef.value) return
  const valid = await subjectFormRef.value.validate().catch(() => false)
  if (!valid) return
  subjectSubmitting.value = true
  try {
    const payload = {
      name: subjectForm.name.trim(),
      icon: subjectForm.icon.trim(),
      description: subjectForm.description.trim(),
      sortOrder: subjectForm.sortOrder,
      themeColor: subjectForm.themeColor,
    }
    if (subjectMode.value === 'create') {
      await subjectApi.create(payload)
      ElMessage.success('学科已创建')
    } else if (editingSubjectId.value) {
      await subjectApi.update(editingSubjectId.value, payload)
      ElMessage.success('学科已更新')
    }
    subjectDialogVisible.value = false
    await load()
  } finally {
    subjectSubmitting.value = false
  }
}

async function moveSubject(subject: SubjectItem, delta: number) {
  if (sorting.value) return
  const index = subjects.value.findIndex((item) => item.id === subject.id)
  const target = index + delta
  if (index < 0 || target < 0 || target >= subjects.value.length) return
  const ids = subjects.value.map((item) => item.id)
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  sorting.value = true
  try {
    await subjectApi.sort(ids)
    await load()
  } finally {
    sorting.value = false
  }
}

function subjectDeleteMessage(subject: SubjectItem): string {
  const extras: string[] = []
  if (subject.courseCount > 0) extras.push(`${subject.courseCount} 门课程`)
  if (subject.routeCount > 0) extras.push(`${subject.routeCount} 条学习路线`)
  if (subject.interviewQuestionCount > 0) {
    extras.push(`${subject.interviewQuestionCount} 道面试题`)
  }

  let message: string
  if (subject.stageCount > 0 && subject.questionCount > 0) {
    message = `学科「${subject.name}」下有 ${subject.stageCount} 个阶段、${subject.questionCount} 道题目`
  } else if (subject.stageCount > 0) {
    message = `学科「${subject.name}」下仅有 ${subject.stageCount} 个阶段、暂无题目`
  } else {
    message = `学科「${subject.name}」下没有阶段与题目`
  }
  if (extras.length > 0) {
    message += `，并关联 ${extras.join('、')}`
  }
  message += subject.stageCount > 0 || extras.length > 0
    ? '。存在关联内容时系统将阻止删除，请先清理关联内容。'
    : '，删除后不可恢复。'
  return message
}

async function removeSubject(subject: SubjectItem) {
  const confirmed = await ElMessageBox.confirm(
    subjectDeleteMessage(subject),
    '删除学科',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' },
  ).catch(() => false)
  if (!confirmed) return
  await subjectApi.remove(subject.id)
  ElMessage.success('学科已删除')
  await load()
}

function openCreateStage(subject: SubjectItem) {
  stageMode.value = 'create'
  stageSubject.value = subject
  editingStageId.value = null
  Object.assign(stageForm, {
    name: '',
    icon: '',
    sortOrder: subject.stages.length + 1,
  })
  stageDialogVisible.value = true
}

function openEditStage(subject: SubjectItem, stage: StageItem) {
  stageMode.value = 'edit'
  stageSubject.value = subject
  editingStageId.value = stage.id
  Object.assign(stageForm, {
    name: stage.name,
    icon: stage.icon ?? '',
    sortOrder: stage.sortOrder,
  })
  stageDialogVisible.value = true
}

async function submitStage() {
  if (!stageFormRef.value || !stageSubject.value) return
  const valid = await stageFormRef.value.validate().catch(() => false)
  if (!valid) return
  stageSubmitting.value = true
  try {
    const payload = {
      name: stageForm.name.trim(),
      icon: stageForm.icon.trim() || null,
      sortOrder: stageForm.sortOrder,
    }
    if (stageMode.value === 'create') {
      await subjectApi.createStage(stageSubject.value.id, payload)
      ElMessage.success('阶段已添加')
    } else if (editingStageId.value) {
      await subjectApi.updateStage(stageSubject.value.id, editingStageId.value, payload)
      ElMessage.success('阶段已更新')
    }
    stageDialogVisible.value = false
    await load()
  } finally {
    stageSubmitting.value = false
  }
}

async function moveStage(subject: SubjectItem, stage: StageItem, delta: number) {
  if (sorting.value) return
  const index = subject.stages.findIndex((item) => item.id === stage.id)
  const target = index + delta
  if (index < 0 || target < 0 || target >= subject.stages.length) return
  const ids = subject.stages.map((item) => item.id)
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  sorting.value = true
  try {
    await subjectApi.sortStages(subject.id, ids)
    await load()
  } finally {
    sorting.value = false
  }
}

async function toggleStage(subject: SubjectItem, stage: StageItem) {
  const nextStatus = stage.status === 1 ? 0 : 1
  if (nextStatus === 0) {
    const confirmed = await ElMessageBox.confirm(
      `停用后，学员前台将不再展示阶段「${stage.name}」及其题目。确定停用吗？`,
      '停用阶段',
      { confirmButtonText: '确认停用', cancelButtonText: '取消', type: 'warning' },
    ).catch(() => false)
    if (!confirmed) return
  }
  await subjectApi.updateStageStatus(subject.id, stage.id, nextStatus)
  ElMessage.success(nextStatus === 1 ? '阶段已启用' : '阶段已停用')
  await load()
}

async function removeStage(subject: SubjectItem, stage: StageItem) {
  const message = stage.questionCount > 0
    ? `阶段「${stage.name}」下关联 ${stage.questionCount} 道题目，系统将阻止删除，请先删除或迁移题目。`
    : `阶段「${stage.name}」下暂无题目，删除后不可恢复。`
  const confirmed = await ElMessageBox.confirm(
    message,
    '删除阶段',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' },
  ).catch(() => false)
  if (!confirmed) return
  await subjectApi.removeStage(subject.id, stage.id)
  ElMessage.success('阶段已删除')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="subject-page">
    <PageHeader
      title="学科管理"
      description="维护前台展示的学科与学习阶段，排序号越小越靠前；删除前需先清理关联内容。"
    >
      <template #actions>
        <el-button v-permission="'subject:add'" type="primary" @click="openCreateSubject">
          <el-icon><Plus /></el-icon>
          <span>新增学科</span>
        </el-button>
      </template>
    </PageHeader>

    <section class="stats-grid">
      <article v-for="card in statCards" :key="card.label" class="stat-card">
        <span
          class="stat-icon"
          :style="{ background: `${card.accent}14`, color: card.accent }"
        >
          <el-icon :size="21"><component :is="card.icon" /></el-icon>
        </span>
        <div class="stat-meta">
          <span class="stat-label">{{ card.label }}</span>
          <strong class="stat-value">{{ card.value }}</strong>
          <span class="stat-hint">{{ card.hint }}</span>
        </div>
      </article>
    </section>

    <div v-loading="loading" class="subject-list">
      <article
        v-for="(subject, index) in subjects"
        :key="subject.id"
        class="subject-card"
      >
        <header class="subject-head">
          <span
            class="subject-icon"
            :style="{
              background: subjectSoftColor(subject.themeColor),
              color: subject.themeColor,
            }"
          >
            {{ subject.icon || '📘' }}
          </span>
          <div class="subject-main">
            <div class="subject-title">
              <h2>{{ subject.name }}</h2>
              <el-tag size="small" effect="plain">排序 #{{ subject.sortOrder }}</el-tag>
              <el-tag
                v-if="subject.stageCount > subject.enabledStageCount"
                size="small"
                type="info"
                effect="plain"
              >
                含已停用阶段
              </el-tag>
            </div>
            <p class="subject-desc">{{ subject.description }}</p>
            <div class="subject-metrics">
              <span>课程 <b>{{ subject.courseCount }}</b></span>
              <span>学习路线 <b>{{ subject.routeCount }}</b></span>
              <span>阶段 <b>{{ subject.stageCount }}</b></span>
              <span>题目 <b>{{ subject.questionCount }}</b></span>
            </div>
          </div>
          <div class="subject-actions">
            <el-tooltip content="上移学科" placement="top">
              <el-button
                text
                class="sort-button"
                :disabled="index === 0 || sorting"
                @click="moveSubject(subject, -1)"
              >
                <el-icon><ArrowUp /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="下移学科" placement="top">
              <el-button
                text
                class="sort-button"
                :disabled="index === subjects.length - 1 || sorting"
                @click="moveSubject(subject, 1)"
              >
                <el-icon><ArrowDown /></el-icon>
              </el-button>
            </el-tooltip>
            <el-button
              v-permission="'subject:edit'"
              size="small"
              plain
              @click="openEditSubject(subject)"
            >
              <el-icon><Edit /></el-icon>
              <span>编辑</span>
            </el-button>
            <el-button
              v-permission="'subject:delete'"
              size="small"
              type="danger"
              plain
              @click="removeSubject(subject)"
            >
              <el-icon><Delete /></el-icon>
              <span>删除</span>
            </el-button>
          </div>
        </header>

        <div class="stage-panel">
          <div class="stage-head">
            <span class="stage-title">学习阶段</span>
            <span class="stage-summary">
              {{ subject.stageCount }} 个阶段 · {{ subject.enabledStageCount }} 个启用
            </span>
          </div>

          <div v-if="subject.stages.length" class="stage-list">
            <div
              v-for="(stage, stageIndex) in subject.stages"
              :key="stage.id"
              class="stage-row"
              :class="{ disabled: stage.status !== 1 }"
            >
              <span class="stage-icon">{{ stageIcon(stage) }}</span>
              <div class="stage-name">
                <span>{{ stage.name }}</span>
                <el-tag
                  size="small"
                  :type="stage.status === 1 ? 'success' : 'info'"
                  effect="light"
                >
                  {{ stage.status === 1 ? '已启用' : '已停用' }}
                </el-tag>
              </div>
              <span class="stage-meta">
                {{ stage.questionCount }} 题 · 排序 {{ stage.sortOrder }}
              </span>
              <div class="stage-actions">
                <el-tooltip content="上移阶段" placement="top">
                  <el-button
                    link
                    class="stage-sort-button"
                    :disabled="stageIndex === 0 || sorting"
                    @click="moveStage(subject, stage, -1)"
                  >
                    <el-icon><ArrowUp /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="下移阶段" placement="top">
                  <el-button
                    link
                    class="stage-sort-button"
                    :disabled="stageIndex === subject.stages.length - 1 || sorting"
                    @click="moveStage(subject, stage, 1)"
                  >
                    <el-icon><ArrowDown /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-button
                  v-permission="'subject:edit'"
                  link
                  :type="stage.status === 1 ? 'warning' : 'success'"
                  @click="toggleStage(subject, stage)"
                >
                  <el-icon>
                    <Lock v-if="stage.status === 1" />
                    <Unlock v-else />
                  </el-icon>
                  <span>{{ stage.status === 1 ? '停用' : '启用' }}</span>
                </el-button>
                <el-button
                  v-permission="'subject:edit'"
                  link
                  type="primary"
                  @click="openEditStage(subject, stage)"
                >
                  <el-icon><Edit /></el-icon>
                  <span>编辑</span>
                </el-button>
                <el-button
                  v-permission="'subject:delete'"
                  link
                  type="danger"
                  @click="removeStage(subject, stage)"
                >
                  <el-icon><Delete /></el-icon>
                  <span>删除</span>
                </el-button>
              </div>
            </div>
          </div>
          <div v-else class="stage-empty">
            该学科还没有学习阶段，添加阶段后才能录入题目
          </div>

          <el-button
            v-permission="'subject:add'"
            text
            type="primary"
            class="add-stage-button"
            @click="openCreateStage(subject)"
          >
            <el-icon><Plus /></el-icon>
            <span>添加阶段</span>
          </el-button>
        </div>
      </article>

      <el-empty
        v-if="!loading && subjects.length === 0"
        class="subject-empty"
        description="暂未创建学科，点击右上角「新增学科」创建第一个学科"
      />
    </div>

    <el-dialog
      v-model="subjectDialogVisible"
      :title="subjectMode === 'create' ? '新增学科' : '编辑学科'"
      width="580px"
      destroy-on-close
    >
      <p class="dialog-tip">学科将展示在学员前台的题库与学习路线中。</p>
      <el-form
        ref="subjectFormRef"
        :model="subjectForm"
        :rules="subjectRules"
        label-position="top"
      >
        <div class="form-grid">
          <el-form-item label="学科名称" prop="name">
            <el-input v-model="subjectForm.name" maxlength="50" placeholder="如：AI应用与智能体开发" />
          </el-form-item>
          <el-form-item label="学科图标（Emoji）" prop="icon">
            <el-input v-model="subjectForm.icon" maxlength="8" placeholder="如：🤖" />
          </el-form-item>
        </div>
        <el-form-item label="学科描述" prop="description">
          <el-input
            v-model="subjectForm.description"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="一句话说明该学科的学习内容与目标"
          />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="排序号" prop="sortOrder">
            <el-input-number
              v-model="subjectForm.sortOrder"
              :min="1"
              :max="999999"
              controls-position="right"
              class="full-width"
            />
            <div class="form-hint">数字越小越靠前</div>
          </el-form-item>
          <el-form-item label="主题颜色" prop="themeColor">
            <div class="color-palette">
              <button
                v-for="color in COLORS"
                :key="color"
                type="button"
                class="color-dot"
                :class="{ active: subjectForm.themeColor === color }"
                :style="{ background: color }"
                :aria-label="`选择主题色 ${color}`"
                @click="subjectForm.themeColor = color"
              >
                <el-icon v-if="subjectForm.themeColor === color"><Check /></el-icon>
              </button>
            </div>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="subjectDialogVisible = false">
          <el-icon><Close /></el-icon>
          <span>取消</span>
        </el-button>
        <el-button type="primary" :loading="subjectSubmitting" @click="submitSubject">
          <el-icon><Check /></el-icon>
          <span>保存</span>
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="stageDialogVisible"
      :title="stageMode === 'create' ? '添加阶段' : '编辑阶段'"
      width="520px"
      destroy-on-close
    >
      <p class="dialog-tip">所属学科：{{ stageSubject?.name }}</p>
      <el-form ref="stageFormRef" :model="stageForm" :rules="stageRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="阶段名称" prop="name">
            <el-input v-model="stageForm.name" maxlength="50" placeholder="如：RAG 知识库构建" />
          </el-form-item>
          <el-form-item label="阶段图标">
            <el-input v-model="stageForm.icon" maxlength="8" placeholder="留空默认 📘" />
          </el-form-item>
        </div>
        <el-form-item label="排序号">
          <el-input-number
            v-model="stageForm.sortOrder"
            :min="1"
            :max="999999"
            controls-position="right"
            class="full-width"
          />
          <div class="form-hint">数字越小越靠前</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stageDialogVisible = false">
          <el-icon><Close /></el-icon>
          <span>取消</span>
        </el-button>
        <el-button type="primary" :loading="stageSubmitting" @click="submitStage">
          <el-icon><Check /></el-icon>
          <span>保存</span>
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.subject-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 14px;
}

.stat-card {
  display: flex;
  padding: 18px 20px;
  border: 1px solid var(--line);
  border-radius: 15px;
  align-items: center;
  gap: 14px;
  background: var(--surface);
}

.stat-icon {
  display: grid;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  flex: none;
  place-items: center;
}

.stat-meta {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.stat-label {
  color: var(--muted);
  font-size: 12.5px;
}

.stat-value {
  margin: 1px 0;
  font-size: 25px;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.stat-hint {
  color: var(--muted-2);
  font-size: 11.5px;
}

.subject-list {
  display: flex;
  min-height: 160px;
  flex-direction: column;
  gap: 16px;
}

.subject-card {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--surface);
}

.subject-head {
  display: flex;
  padding: 20px;
  align-items: flex-start;
  gap: 15px;
}

.subject-icon {
  display: grid;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  flex: none;
  place-items: center;
  font-size: 25px;
}

.subject-main {
  min-width: 0;
  flex: 1;
}

.subject-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.subject-title h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
}

.subject-desc {
  margin: 5px 0 10px;
  color: var(--muted);
  font-size: 12.5px;
  line-height: 1.65;
}

.subject-metrics {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  color: var(--muted);
  font-size: 12.5px;
}

.subject-metrics b {
  color: var(--text-2);
  font-weight: 650;
}

.subject-actions {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.sort-button {
  width: 28px;
  height: 28px;
  padding: 0;
  color: var(--muted);
}

.stage-panel {
  padding: 14px 20px 16px;
  border-top: 1px solid var(--line);
  background: #fbfcfe;
}

.stage-head {
  display: flex;
  margin-bottom: 9px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.stage-title {
  color: var(--text-2);
  font-size: 12.5px;
  font-weight: 650;
}

.stage-summary {
  color: var(--muted-2);
  font-size: 11.5px;
}

.stage-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stage-row {
  display: flex;
  min-height: 44px;
  padding: 7px 10px;
  border: 1px solid var(--line);
  border-radius: 10px;
  align-items: center;
  gap: 10px;
  background: #fff;
  transition:
    border-color 0.16s,
    opacity 0.16s;
}

.stage-row:hover {
  border-color: #dcdafa;
}

.stage-row.disabled {
  opacity: 0.66;
}

.stage-icon {
  display: grid;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  flex: none;
  place-items: center;
  background: var(--surface-2);
  font-size: 15px;
}

.stage-name {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 600;
}

.stage-meta {
  color: var(--muted);
  font-size: 11.5px;
  white-space: nowrap;
}

.stage-actions {
  display: flex;
  margin-left: auto;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.stage-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.stage-sort-button {
  width: 22px;
  height: 22px;
  padding: 0;
  color: var(--muted);
}

.stage-empty {
  padding: 14px;
  border: 1px dashed var(--line);
  border-radius: 10px;
  background: #fff;
  color: var(--muted-2);
  font-size: 12px;
  text-align: center;
}

.add-stage-button {
  margin-top: 8px;
  padding-left: 2px;
}

.subject-empty {
  border: 1px dashed var(--line);
  border-radius: 16px;
  background: var(--surface);
}

.dialog-tip {
  margin: 0 0 18px;
  color: var(--muted);
  font-size: 12.5px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.full-width {
  width: 100%;
}

.form-hint {
  margin-top: 5px;
  color: var(--muted-2);
  font-size: 11.5px;
}

.color-palette {
  display: flex;
  padding-top: 4px;
  gap: 7px;
  flex-wrap: wrap;
}

.color-dot {
  display: grid;
  width: 25px;
  height: 25px;
  padding: 0;
  border: 2px solid #fff;
  border-radius: 50%;
  place-items: center;
  color: #fff;
  box-shadow: 0 0 0 1px var(--line);
  cursor: pointer;
  transition:
    transform 0.15s,
    box-shadow 0.15s;
}

.color-dot.active {
  box-shadow: 0 0 0 2px var(--text-2);
  transform: scale(1.08);
}

@media (max-width: 900px) {
  .subject-head {
    flex-wrap: wrap;
  }

  .subject-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .stage-row {
    flex-wrap: wrap;
  }

  .stage-actions {
    width: 100%;
    margin-left: 40px;
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
