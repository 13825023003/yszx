<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadRequestOptions,
} from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { courseApi } from '@/api/course'
import { subjectApi } from '@/api/subject'
import type {
  CourseImportPreview,
  CourseItem,
  CourseStats,
} from '@/types/course'
import type { SubjectBrief } from '@/types/subject'

const loading = ref(false)
const courses = ref<CourseItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const failedCovers = ref<Set<number>>(new Set())
const stats = ref<CourseStats>({
  totalCourses: 0,
  totalDurationMinutes: 0,
  associatedSubjectCount: 0,
})
const subjects = ref<SubjectBrief[]>([])

const filters = reactive<{
  keyword: string
  subjectId: number | undefined
  source: number | undefined
}>({
  keyword: '',
  subjectId: undefined,
  source: undefined,
})

const subjectFilterOptions = computed(() => [
  ...subjects.value,
  { id: 0, name: '其他学科' },
])

const statCards = computed(() => [
  {
    label: '课程总数',
    value: stats.value.totalCourses,
    suffix: '门',
    icon: 'VideoCamera',
    accent: '#4f46e5',
    hint: 'B站课程库全部课程',
  },
  {
    label: '总时长',
    value: (stats.value.totalDurationMinutes / 60).toFixed(1),
    suffix: '小时',
    icon: 'Timer',
    accent: '#7c3aed',
    hint: '按课程分钟数汇总',
  },
  {
    label: '关联学科数',
    value: stats.value.associatedSubjectCount,
    suffix: '个',
    icon: 'Collection',
    accent: '#12b76a',
    hint: '不含「其他学科」口径',
  },
])

const courseDialogVisible = ref(false)
const courseMode = ref<'create' | 'edit'>('create')
const editingCourseId = ref<number | null>(null)
const courseSubmitting = ref(false)
const courseFormRef = ref<FormInstance>()
const courseForm = reactive({
  name: '',
  durationText: '',
  primarySubjectId: 1 as number,
  extraSubjectIds: [] as number[],
  source: 1,
  link: '',
  bvid: '',
  coverUrl: '',
})

const extraSubjectOptions = computed(() =>
  subjects.value.filter(
    (subject) =>
      subject.id !== courseForm.primarySubjectId
      && courseForm.primarySubjectId !== 0,
  ),
)

const coverPreviewStyle = computed(() => {
  const url = courseForm.coverUrl.trim()
  if (/^https?:\/\//.test(url) || url.startsWith('/files/')) {
    return {
      backgroundImage: `url("${url.replace(/"/g, '\\"')}")`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
    }
  }
  return { background: 'linear-gradient(135deg, #6366f1, #a855f7)' }
})

const courseRules: FormRules = {
  name: [
    { required: true, message: '请输入课程名称', trigger: 'blur' },
    { max: 100, message: '课程名称不能超过 100 个字符', trigger: 'blur' },
  ],
  durationText: [
    { required: true, message: '请输入课程时长', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const minutes = parseDurationText(String(value ?? ''))
        if (minutes == null || minutes <= 0) {
          callback(new Error('请输入大于 0 的分钟数，或「小时:分钟」格式，如 1:30'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  primarySubjectId: [{ required: true, message: '请选择所属学科', trigger: 'change' }],
  source: [{ required: true, message: '请选择来源', trigger: 'change' }],
  link: [
    {
      validator: (_rule, value, callback) => {
        if (!value || /^https?:\/\/\S+$/.test(String(value))) {
          callback()
        } else {
          callback(new Error('播放链接需以 http(s):// 开头'))
        }
      },
      trigger: 'blur',
    },
  ],
  bvid: [
    {
      validator: (_rule, value, callback) => {
        if (!value || /^BV[0-9A-Za-z]{8,18}$/.test(String(value))) {
          callback()
        } else {
          callback(new Error('BV 号格式不正确'))
        }
      },
      trigger: 'blur',
    },
  ],
  coverUrl: [
    {
      validator: (_rule, value, callback) => {
        if (!value
          || /^https?:\/\/\S+$/.test(String(value))
          || String(value).startsWith('/files/')) {
          callback()
        } else {
          callback(new Error('封面需为 http(s) 或站内 /files 链接'))
        }
      },
      trigger: 'blur',
    },
  ],
}

const importVisible = ref(false)
const previewing = ref(false)
const importing = ref(false)
const importPreview = ref<CourseImportPreview | null>(null)

function parseDurationText(text: string): number | null {
  const value = text.trim().replace(/,/g, '')
  if (/^\d+$/.test(value)) {
    const minutes = Number(value)
    return minutes > 0 ? minutes : null
  }
  const matched = value.match(/^(\d{1,3})[:：]([0-5]?\d)$/)
  if (matched) {
    return Number(matched[1]) * 60 + Number(matched[2])
  }
  return null
}

function formatDuration(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return `${hours}:${String(rest).padStart(2, '0')}`
}

function sourceText(source: number): string {
  return source === 1 ? 'B站' : '其他'
}

function sourceTagType(source: number): 'primary' | 'info' {
  return source === 1 ? 'primary' : 'info'
}

function subjectNames(course: CourseItem): string[] {
  const names: string[] = []
  if (course.primarySubject) names.push(course.primarySubject.name)
  course.subjects.forEach((subject) => names.push(subject.name))
  return names.length > 0 ? names : ['其他学科']
}

function hasCover(course: CourseItem): boolean {
  return Boolean(course.coverUrl) && !failedCovers.value.has(course.id)
}

function markCoverFailed(id: number) {
  const next = new Set(failedCovers.value)
  next.add(id)
  failedCovers.value = next
}

async function loadStats() {
  stats.value = await courseApi.stats()
}

async function loadList() {
  loading.value = true
  failedCovers.value = new Set()
  try {
    const data = await courseApi.list({
      keyword: filters.keyword.trim() || undefined,
      subjectId: filters.subjectId,
      source: filters.source,
      page: page.value,
      pageSize: pageSize.value,
    })
    courses.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadSubjects() {
  const list = await subjectApi.list()
  subjects.value = list.map((subject) => ({ id: subject.id, name: subject.name }))
}

function search() {
  page.value = 1
  loadList()
}

function resetFilters() {
  filters.keyword = ''
  filters.subjectId = undefined
  filters.source = undefined
  page.value = 1
  loadList()
}

function openCreate() {
  courseMode.value = 'create'
  editingCourseId.value = null
  Object.assign(courseForm, {
    name: '',
    durationText: '',
    primarySubjectId: subjects.value[0]?.id ?? 0,
    extraSubjectIds: [],
    source: 1,
    link: '',
    bvid: '',
    coverUrl: '',
  })
  courseDialogVisible.value = true
}

function openEdit(course: CourseItem) {
  courseMode.value = 'edit'
  editingCourseId.value = course.id
  const primaryId = course.primarySubject?.id ?? 0
  Object.assign(courseForm, {
    name: course.name,
    durationText: formatDuration(course.durationMinutes),
    primarySubjectId: primaryId,
    extraSubjectIds: course.subjects
      .map((subject) => subject.id)
      .filter((id) => id !== primaryId),
    source: course.source,
    link: course.link ?? '',
    bvid: course.bvid ?? '',
    coverUrl: course.coverUrl ?? '',
  })
  courseDialogVisible.value = true
}

function handlePrimaryChange() {
  if (courseForm.primarySubjectId === 0) {
    courseForm.extraSubjectIds = []
    return
  }
  courseForm.extraSubjectIds = courseForm.extraSubjectIds.filter(
    (id) => id !== courseForm.primarySubjectId,
  )
}

function handleSourceChange() {
  if (courseForm.source === 2) {
    courseForm.bvid = ''
  }
}

function syncBvidFromLink() {
  if (courseForm.source !== 1 || courseForm.bvid.trim()) {
    return
  }
  const matched = courseForm.link.match(/BV[0-9A-Za-z]{8,18}/)
  if (matched) {
    courseForm.bvid = matched[0]
  }
}

async function submitCourse() {
  if (!courseFormRef.value) return
  const valid = await courseFormRef.value.validate().catch(() => false)
  if (!valid) return
  const durationMinutes = parseDurationText(courseForm.durationText)
  if (durationMinutes == null) return
  courseSubmitting.value = true
  try {
    const payload = {
      name: courseForm.name.trim(),
      durationMinutes,
      subjectId: courseForm.primarySubjectId === 0 ? null : courseForm.primarySubjectId,
      subjectIds: courseForm.primarySubjectId === 0 ? [] : courseForm.extraSubjectIds,
      source: courseForm.source,
      link: courseForm.link.trim() || null,
      bvid: courseForm.source === 1 ? courseForm.bvid.trim() || null : null,
      coverUrl: courseForm.coverUrl.trim() || null,
    }
    if (courseMode.value === 'create') {
      await courseApi.create(payload)
      ElMessage.success('课程已创建')
    } else if (editingCourseId.value) {
      await courseApi.update(editingCourseId.value, payload)
      ElMessage.success('课程已更新')
    }
    courseDialogVisible.value = false
    await Promise.all([loadList(), loadStats()])
  } finally {
    courseSubmitting.value = false
  }
}

async function removeCourse(course: CourseItem) {
  const confirmed = await ElMessageBox.confirm(
    `确定删除课程「${course.name}」吗？若课程正被学习路线引用，系统将阻止删除。`,
    '删除课程',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' },
  ).catch(() => false)
  if (!confirmed) return
  await courseApi.remove(course.id)
  ElMessage.success('课程已删除')
  await Promise.all([loadList(), loadStats()])
}

function openImport() {
  importPreview.value = null
  importVisible.value = true
}

async function handleImportUpload(options: UploadRequestOptions) {
  const file = options.file as File
  if (!/\.(xlsx|xls)$/i.test(file.name)) {
    ElMessage.error('仅支持 .xlsx / .xls 文件')
    return
  }
  previewing.value = true
  importPreview.value = null
  try {
    const preview = await courseApi.previewImport(file)
    importPreview.value = preview
    options.onSuccess?.(preview)
    if (preview.invalidCount > 0) {
      ElMessage.warning(`预览完成：${preview.invalidCount} 行无效，将被跳过`)
    } else {
      ElMessage.success(`预览完成：可导入 ${preview.validCount + preview.warningCount} 门课程`)
    }
  } catch (error) {
    options.onError?.(error as Parameters<NonNullable<typeof options.onError>>[0])
  } finally {
    previewing.value = false
  }
}

async function confirmImport() {
  if (!importPreview.value?.token) return
  importing.value = true
  try {
    const result = await courseApi.confirmImport(importPreview.value.token)
    ElMessage.success(`已导入 ${result.importedCount} 门课程`)
    importVisible.value = false
    importPreview.value = null
    await Promise.all([loadList(), loadStats()])
  } finally {
    importing.value = false
  }
}

function statusTagType(status: string): 'success' | 'warning' | 'danger' {
  if (status === 'valid') return 'success'
  if (status === 'warning') return 'warning'
  return 'danger'
}

function statusText(status: string): string {
  if (status === 'valid') return '有效'
  if (status === 'warning') return '警告'
  return '无效'
}

onMounted(async () => {
  await Promise.all([loadStats(), loadList(), loadSubjects()])
})
</script>

<template>
  <div class="course-page">
    <PageHeader
      title="B站课程库"
      description="维护可复用的 B 站课程资源，供学习路线自由编排；支持 Excel 批量导入。"
    >
      <template #actions>
        <el-button v-permission="'course:add'" @click="openImport">
          <el-icon><Upload /></el-icon>
          <span>批量导入</span>
        </el-button>
        <el-button v-permission="'course:add'" type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          <span>新增课程</span>
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
          <div class="stat-value-line">
            <strong class="stat-value">{{ card.value }}</strong>
            <span class="stat-suffix">{{ card.suffix }}</span>
          </div>
          <span class="stat-hint">{{ card.hint }}</span>
        </div>
      </article>
    </section>

    <section class="panel">
      <div class="toolbar">
        <el-input
          v-model="filters.keyword"
          class="keyword-input"
          clearable
          placeholder="搜索课程名称"
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select
          v-model="filters.subjectId"
          class="filter-select"
          clearable
          placeholder="全部学科"
          @change="search"
        >
          <el-option
            v-for="subject in subjectFilterOptions"
            :key="subject.id"
            :label="subject.name"
            :value="subject.id"
          />
        </el-select>
        <el-select
          v-model="filters.source"
          class="filter-select"
          clearable
          placeholder="全部来源"
          @change="search"
        >
          <el-option label="B站" :value="1" />
          <el-option label="其他" :value="2" />
        </el-select>
        <el-button type="primary" plain @click="search">
          <el-icon><Search /></el-icon>
          <span>查询</span>
        </el-button>
        <el-button text @click="resetFilters">
          <el-icon><RefreshLeft /></el-icon>
          <span>重置</span>
        </el-button>
        <span class="toolbar-hint">共 {{ total }} 门课程</span>
      </div>

      <el-table
        v-loading="loading"
        :data="courses"
        empty-text="暂无匹配的课程，试试调整筛选条件"
        class="course-table"
      >
        <el-table-column label="序号" width="58">
          <template #default="{ $index }">
            <span class="row-index">{{ (page - 1) * pageSize + $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="封面" width="92">
          <template #default="{ row }">
            <!-- B站图床校验 Referer：no-referrer 加载原图；点击封面打开课程链接 -->
            <a
              class="cover-thumb"
              :href="row.link || undefined"
              :target="row.link ? '_blank' : undefined"
              rel="noopener noreferrer"
              :title="row.link ? '打开课程链接' : '未填写课程链接'"
            >
              <img
                v-if="hasCover(row)"
                :src="row.coverUrl || ''"
                alt=""
                loading="lazy"
                referrerpolicy="no-referrer"
                @error="markCoverFailed(row.id)"
              />
              <span v-else class="cover-fallback">▶</span>
              <span v-if="hasCover(row) && row.link" class="cover-overlay">
                <el-icon :size="18"><VideoPlay /></el-icon>
              </span>
            </a>
          </template>
        </el-table-column>
        <el-table-column label="课程名称" min-width="250">
          <template #default="{ row }">
            <div class="course-name" :title="row.name">{{ row.name }}</div>
            <div class="course-sub">
              {{ row.bvid ? `BV ${row.bvid}` : '未填写 BV 号' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="时长" width="128">
          <template #default="{ row }">
            <div class="duration-main">{{ formatDuration(row.durationMinutes) }}</div>
            <div class="course-sub">{{ row.durationMinutes }} 分钟</div>
          </template>
        </el-table-column>
        <el-table-column label="学科" width="190">
          <template #default="{ row }">
            <div class="subject-tags">
              <el-tag
                v-for="name in subjectNames(row)"
                :key="name"
                size="small"
                effect="plain"
                class="subject-tag"
              >
                {{ name }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="82">
          <template #default="{ row }">
            <el-tag :type="sourceTagType(row.source)" size="small" effect="light">
              {{ sourceText(row.source) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="138" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button
                v-permission="'course:edit'"
                link
                type="primary"
                @click="openEdit(row)"
              >
                <el-icon><Edit /></el-icon>
                <span>编辑</span>
              </el-button>
              <el-button
                v-permission="'course:delete'"
                link
                type="danger"
                @click="removeCourse(row)"
              >
                <el-icon><Delete /></el-icon>
                <span>删除</span>
              </el-button>
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
          @current-change="loadList"
          @size-change="search"
        />
      </div>
    </section>

    <el-dialog
      v-model="courseDialogVisible"
      :title="courseMode === 'create' ? '新增课程' : '编辑课程'"
      width="660px"
      destroy-on-close
    >
      <p class="dialog-tip">课程入库后可被任意学习路线引用。</p>
      <el-form ref="courseFormRef" :model="courseForm" :rules="courseRules" label-position="top">
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="courseForm.name" maxlength="100" placeholder="请输入课程名称" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="时长" prop="durationText">
            <el-input
              v-model="courseForm.durationText"
              placeholder="如 1:30 或 90"
              @blur="courseForm.durationText = courseForm.durationText.trim()"
            />
            <div class="form-hint">支持「小时:分钟」或直接填写分钟数</div>
          </el-form-item>
          <el-form-item label="来源" prop="source">
            <el-select v-model="courseForm.source" class="full-width" @change="handleSourceChange">
              <el-option label="B站" :value="1" />
              <el-option label="其他" :value="2" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="所属学科" prop="primarySubjectId">
            <el-select
              v-model="courseForm.primarySubjectId"
              class="full-width"
              @change="handlePrimaryChange"
            >
              <el-option
                v-for="subject in subjectFilterOptions"
                :key="subject.id"
                :label="subject.name"
                :value="subject.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="额外学科（选填）">
            <el-select
              v-model="courseForm.extraSubjectIds"
              class="full-width"
              multiple
              collapse-tags
              collapse-tags-tooltip
              :disabled="courseForm.primarySubjectId === 0"
              placeholder="可选多个额外学科"
            >
              <el-option
                v-for="subject in extraSubjectOptions"
                :key="subject.id"
                :label="subject.name"
                :value="subject.id"
              />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="BV 号" prop="bvid">
            <el-input
              v-model="courseForm.bvid"
              maxlength="20"
              :disabled="courseForm.source !== 1"
              placeholder="如 BV1LSs6oEnj"
            />
          </el-form-item>
          <el-form-item label="播放链接" prop="link">
            <el-input
              v-model="courseForm.link"
              maxlength="500"
              placeholder="https://www.bilibili.com/video/BV..."
              @blur="syncBvidFromLink"
            />
          </el-form-item>
        </div>
        <el-form-item label="封面图片 URL" prop="coverUrl">
          <el-input
            v-model="courseForm.coverUrl"
            maxlength="500"
            placeholder="支持 http(s) 或站内 /files 链接，留空使用默认渐变封面"
          />
        </el-form-item>
        <div class="cover-preview" :style="coverPreviewStyle">
          <span v-if="!courseForm.coverUrl">课程封面预览</span>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="courseDialogVisible = false">
          <el-icon><Close /></el-icon>
          <span>取消</span>
        </el-button>
        <el-button type="primary" :loading="courseSubmitting" @click="submitCourse">
          <el-icon><Check /></el-icon>
          <span>保存</span>
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="批量导入课程" width="780px" destroy-on-close>
      <div class="import-format">
        <div class="import-format-head">
          <span>Excel 列格式</span>
          <span>单次最多 1000 行</span>
        </div>
        <div class="format-columns">
          <span>课程名称</span>
          <span>所属学科</span>
          <span>来源</span>
          <span>时长（分钟）</span>
          <span>BV号</span>
          <span>封面</span>
          <span>播放链接</span>
        </div>
        <p>
          所属学科可填写多个，用「、」分隔，第一个作为主学科；不属于具体学科时填写「其他学科」。
          时长支持分钟数或「小时:分钟」，来源支持 B站 / 其他。
        </p>
      </div>

      <el-upload
        drag
        :show-file-list="false"
        accept=".xlsx,.xls"
        :http-request="handleImportUpload"
        :disabled="previewing || importing"
        class="import-upload"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将 Excel 文件拖到此处，或 <em>点击选择文件</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">支持 .xlsx / .xls，文件不超过 10MB</div>
        </template>
      </el-upload>

      <div v-if="previewing" class="preview-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        正在解析 Excel...
      </div>

      <template v-if="importPreview && !previewing">
        <div class="preview-summary">
          <span class="file-name">{{ importPreview.filename }}</span>
          <div class="summary-tags">
            <el-tag type="success" effect="light">有效 {{ importPreview.validCount }}</el-tag>
            <el-tag type="warning" effect="light">警告 {{ importPreview.warningCount }}</el-tag>
            <el-tag type="danger" effect="light">无效 {{ importPreview.invalidCount }}</el-tag>
            <span class="summary-total">共 {{ importPreview.totalRows }} 行</span>
          </div>
        </div>
        <div class="preview-list">
          <div
            v-for="row in importPreview.rows"
            :key="row.rowNumber"
            class="preview-row"
            :class="row.status"
          >
            <el-tag :type="statusTagType(row.status)" size="small" effect="dark">
              {{ statusText(row.status) }}
            </el-tag>
            <div class="preview-main">
              <div class="preview-title">
                第 {{ row.rowNumber }} 行 · {{ row.name }}
              </div>
              <div class="preview-meta">
                时长 {{ row.durationMinutes ?? '—' }} 分钟
                <span v-if="row.subjectText"> · {{ row.subjectText }}</span>
                <span v-if="row.bvid"> · {{ row.bvid }}</span>
              </div>
              <div v-if="row.messages.length" class="preview-message">
                {{ row.messages.join('；') }}
              </div>
            </div>
          </div>
        </div>
      </template>

      <template #footer>
        <el-button @click="importVisible = false">
          <el-icon><Close /></el-icon>
          <span>取消</span>
        </el-button>
        <el-button
          type="primary"
          :loading="importing"
          :disabled="!importPreview?.token
            || (importPreview.validCount + importPreview.warningCount) === 0"
          @click="confirmImport"
        >
          <el-icon><Check /></el-icon>
          <span>
            确认导入 {{ importPreview ? importPreview.validCount + importPreview.warningCount : 0 }} 门
          </span>
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.course-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
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

.stat-value-line {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-value {
  font-size: 25px;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.stat-suffix {
  color: var(--muted);
  font-size: 12px;
  font-weight: 600;
}

.stat-hint {
  color: var(--muted-2);
  font-size: 11.5px;
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
  width: 245px;
}

.filter-select {
  width: 155px;
}

.toolbar-hint {
  margin-left: auto;
  color: var(--muted);
  font-size: 12.5px;
}

.row-index {
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}

.cover-thumb {
  position: relative;
  display: grid;
  width: 76px;
  height: 44px;
  border-radius: 8px;
  place-items: center;
  overflow: hidden;
  background: linear-gradient(135deg, #6366f1, #a855f7);
  text-decoration: none;
}

.cover-thumb[href] {
  cursor: pointer;
}

.cover-thumb img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s;
}

.cover-thumb[href]:hover img {
  transform: scale(1.06);
}

.cover-fallback {
  color: rgba(255, 255, 255, 0.9);
  font-size: 15px;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(16, 24, 40, 0.34);
  color: #fff;
  opacity: 0;
  transition: opacity 0.18s;
}

.cover-thumb[href]:hover .cover-overlay {
  opacity: 1;
}

.course-name {
  overflow: hidden;
  font-size: 13.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-sub {
  margin-top: 3px;
  color: var(--muted);
  font-size: 11.5px;
}

.duration-main {
  color: var(--text-2);
  font-size: 13px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.subject-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.subject-tag {
  max-width: 100%;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 0 8px;
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

.cover-preview {
  display: flex;
  height: 128px;
  margin-top: 2px;
  border: 1px solid var(--line);
  border-radius: 11px;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.import-format {
  padding: 13px 15px;
  border: 1px solid var(--line);
  border-radius: 11px;
  background: var(--surface-2);
}

.import-format-head {
  display: flex;
  margin-bottom: 9px;
  align-items: center;
  justify-content: space-between;
  color: var(--text-2);
  font-size: 12.5px;
  font-weight: 650;
}

.import-format-head span:last-child {
  color: var(--muted-2);
  font-size: 11.5px;
  font-weight: 400;
}

.format-columns {
  display: flex;
  gap: 7px;
  flex-wrap: wrap;
}

.format-columns span {
  padding: 3px 8px;
  border: 1px solid #dcdafa;
  border-radius: 6px;
  background: #fff;
  color: var(--brand);
  font-size: 11.5px;
}

.import-format p {
  margin: 10px 0 0;
  color: var(--muted);
  font-size: 11.5px;
  line-height: 1.7;
}

.import-upload {
  margin-top: 14px;
}

.import-upload :deep(.el-upload-dragger) {
  padding: 22px 16px;
}

.upload-icon {
  margin-bottom: 6px;
  color: var(--brand);
  font-size: 30px;
}

.preview-loading {
  display: flex;
  margin-top: 14px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--muted);
  font-size: 12.5px;
}

.preview-summary {
  display: flex;
  margin-top: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--line);
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.file-name {
  overflow: hidden;
  max-width: 260px;
  color: var(--text-2);
  font-size: 12.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-tags {
  display: flex;
  margin-left: auto;
  align-items: center;
  gap: 7px;
}

.summary-total {
  color: var(--muted);
  font-size: 11.5px;
}

.preview-list {
  display: flex;
  max-height: 300px;
  margin-top: 8px;
  overflow-y: auto;
  flex-direction: column;
}

.preview-row {
  display: flex;
  padding: 9px 4px;
  border-bottom: 1px dashed var(--line);
  align-items: flex-start;
  gap: 10px;
}

.preview-row:last-child {
  border-bottom: 0;
}

.preview-row.invalid {
  background: #fffafa;
}

.preview-main {
  min-width: 0;
}

.preview-title {
  overflow: hidden;
  font-size: 12.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-meta {
  margin-top: 2px;
  color: var(--muted);
  font-size: 11.5px;
}

.preview-message {
  margin-top: 3px;
  color: var(--warn);
  font-size: 11.5px;
  line-height: 1.6;
}

.preview-row.invalid .preview-message {
  color: var(--danger);
}

@media (max-width: 900px) {
  .keyword-input,
  .filter-select {
    width: 100%;
  }

  .toolbar-hint {
    width: 100%;
    margin-left: 0;
  }
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
