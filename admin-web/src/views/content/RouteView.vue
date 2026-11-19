<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { routeApi } from '@/api/route'
import { courseApi } from '@/api/course'
import { subjectApi } from '@/api/subject'
import type { CourseItem } from '@/types/course'
import type { SubjectItem } from '@/types/subject'
import type { RouteCourseItem, RouteDetail, RouteItem } from '@/types/route'

interface PickedCourse {
  id: number
  name: string
  durationMinutes: number
  bvid: string | null
  missing: boolean
}

const loading = ref(false)
const routes = ref<RouteItem[]>([])
const courses = ref<CourseItem[]>([])
const subjects = ref<SubjectItem[]>([])
const courseMap = computed(() => new Map(courses.value.map((course) => [course.id, course])))
const subjectMap = computed(() => new Map(subjects.value.map((subject) => [subject.id, subject])))

const viewMode = ref<'list' | 'detail'>('list')
const detail = ref<RouteDetail | null>(null)
const detailLoading = ref(false)

const routeStats = computed(() => {
  const enabled = routes.value.filter((route) => route.status === 1).length
  const subjectCount = new Set(routes.value.map((route) => route.subjectId)).size
  const courseRefs = routes.value.reduce((total, route) => total + route.courseCount, 0)
  return [
    {
      label: '路线总数',
      value: routes.value.length,
      icon: 'Guide',
      accent: '#4f46e5',
      hint: '全部学习路线',
    },
    {
      label: '已启用',
      value: enabled,
      icon: 'CircleCheck',
      accent: '#12b76a',
      hint: '学员前台可见',
    },
    {
      label: '覆盖学科',
      value: subjectCount,
      icon: 'Collection',
      accent: '#0ea5e9',
      hint: '已编排路线的学科',
    },
    {
      label: '关联课程次',
      value: courseRefs,
      icon: 'VideoCamera',
      accent: '#7c3aed',
      hint: '路线课程引用总数',
    },
  ]
})

const routeDialogVisible = ref(false)
const routeMode = ref<'create' | 'edit'>('create')
const editingRouteId = ref<number | null>(null)
const routeSubmitting = ref(false)
const routeOpening = ref(false)
const routeFormRef = ref<FormInstance>()
const routeForm = reactive({
  name: '',
  subjectId: 0,
  targetEducation: '',
  icon: '🗺',
  description: '',
  status: 1,
})
const pickedIds = ref<number[]>([])
const originalCourseIds = ref<number[]>([])
const availableSearch = ref('')

const routeRules: FormRules = {
  name: [
    { required: true, message: '请输入路线名称', trigger: 'blur' },
    { max: 100, message: '路线名称不能超过 100 个字符', trigger: 'blur' },
  ],
  subjectId: [{ required: true, message: '请选择关联学科', trigger: 'change' }],
  targetEducation: [
    { max: 100, message: '适合学历不能超过 100 个字符', trigger: 'blur' },
  ],
  description: [
    { max: 1000, message: '路线描述不能超过 1000 个字符', trigger: 'blur' },
  ],
}

const availableCourses = computed(() => {
  const keyword = availableSearch.value.trim().toLowerCase()
  return courses.value
    .filter((course) => course.primarySubject?.id === routeForm.subjectId)
    .filter((course) => !keyword || course.name.toLowerCase().includes(keyword))
})

const pickedCourses = computed<PickedCourse[]>(() =>
  pickedIds.value.map((id) => {
    const course = courseMap.value.get(id)
    if (!course) {
      return { id, name: '课程已删除', durationMinutes: 0, bvid: null, missing: true }
    }
    return {
      id,
      name: course.name,
      durationMinutes: course.durationMinutes,
      bvid: course.bvid,
      missing: false,
    }
  }),
)

const addDialogVisible = ref(false)
const addSearch = ref('')
const addSelectedIds = ref<number[]>([])
const addSubmitting = ref(false)

const addCourseGroups = computed(() => {
  const subjectId = detail.value?.subjectId
  const addedIds = new Set(detail.value?.courses.map((course) => course.id) ?? [])
  const keyword = addSearch.value.trim().toLowerCase()
  const local: Array<CourseItem & { added: boolean }> = []
  const other: Array<CourseItem & { added: boolean }> = []
  courses.value.forEach((course) => {
    if (keyword && !course.name.toLowerCase().includes(keyword)) {
      return
    }
    const view = { ...course, added: addedIds.has(course.id) }
    if (course.primarySubject?.id === subjectId) {
      local.push(view)
    } else {
      other.push(view)
    }
  })
  return { local, other }
})

const previewVisible = ref(false)
const previewDetail = ref<RouteDetail | null>(null)
const previewLoading = ref(false)

function durationText(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (hours === 0) {
    return `${rest} 分钟`
  }
  return rest === 0 ? `${hours} 小时` : `${hours} 小时 ${rest} 分`
}

function durationTableText(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return `${hours}:${String(rest).padStart(2, '0')}`
}

function subjectColor(subjectId: number): string {
  return subjectMap.value.get(subjectId)?.themeColor || '#6366f1'
}

function routeIcon(route: { icon: string | null; subjectId: number }): string {
  return route.icon || subjectMap.value.get(route.subjectId)?.icon || '🗺'
}

function courseLink(course: RouteCourseItem): string | null {
  if (course.link) {
    return course.link
  }
  return course.bvid ? `https://www.bilibili.com/video/${course.bvid}` : null
}

function courseSubjectNames(course: RouteCourseItem): string {
  const names: string[] = []
  if (course.primarySubject) names.push(course.primarySubject.name)
  course.subjects.forEach((subject) => names.push(subject.name))
  return names.length > 0 ? names.join('、') : '其他学科'
}

async function loadRoutes() {
  loading.value = true
  try {
    routes.value = await routeApi.list()
  } finally {
    loading.value = false
  }
}

async function loadCourses() {
  courses.value = await courseApi.listAll()
}

async function loadSubjects() {
  subjects.value = await subjectApi.list()
}

async function loadDetail(id: number) {
  detailLoading.value = true
  try {
    detail.value = await routeApi.detail(id)
  } finally {
    detailLoading.value = false
  }
}

async function refreshDetailAndRoutes() {
  await loadRoutes()
  if (detail.value) {
    await loadDetail(detail.value.id)
  }
}

async function openDetail(route: RouteItem) {
  viewMode.value = 'detail'
  await loadDetail(route.id)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function backToList() {
  viewMode.value = 'list'
  detail.value = null
  loadRoutes()
}

function openCreateRoute() {
  routeMode.value = 'create'
  editingRouteId.value = null
  pickedIds.value = []
  originalCourseIds.value = []
  availableSearch.value = ''
  Object.assign(routeForm, {
    name: '',
    subjectId: subjects.value[0]?.id ?? 0,
    targetEducation: '',
    icon: '🗺',
    description: '',
    status: 1,
  })
  routeDialogVisible.value = true
}

async function openEditRoute(route: RouteItem) {
  routeOpening.value = true
  try {
    const data = await routeApi.detail(route.id)
    routeMode.value = 'edit'
    editingRouteId.value = route.id
    Object.assign(routeForm, {
      name: data.name,
      subjectId: data.subjectId,
      targetEducation: data.targetEducation ?? '',
      icon: data.icon ?? '🗺',
      description: data.description ?? '',
      status: data.status,
    })
    originalCourseIds.value = data.courses.map((course) => course.id)
    pickedIds.value = [...originalCourseIds.value]
    availableSearch.value = ''
    routeDialogVisible.value = true
  } finally {
    routeOpening.value = false
  }
}

function isPicked(courseId: number): boolean {
  return pickedIds.value.includes(courseId)
}

function togglePicked(courseId: number, checked: boolean) {
  if (checked) {
    if (!pickedIds.value.includes(courseId)) {
      pickedIds.value.push(courseId)
    }
  } else {
    pickedIds.value = pickedIds.value.filter((id) => id !== courseId)
  }
}

function movePicked(index: number, delta: number) {
  const target = index + delta
  if (target < 0 || target >= pickedIds.value.length) {
    return
  }
  const ids = [...pickedIds.value]
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  pickedIds.value = ids
}

function removePicked(courseId: number) {
  pickedIds.value = pickedIds.value.filter((id) => id !== courseId)
}

async function submitRoute() {
  if (!routeFormRef.value) return
  const valid = await routeFormRef.value.validate().catch(() => false)
  if (!valid) return
  routeSubmitting.value = true
  try {
    const payload = {
      name: routeForm.name.trim(),
      subjectId: routeForm.subjectId,
      targetEducation: routeForm.targetEducation.trim() || null,
      icon: routeForm.icon.trim() || null,
      description: routeForm.description.trim() || null,
      status: routeForm.status,
    }
    if (routeMode.value === 'create') {
      const created = await routeApi.create(payload)
      if (pickedIds.value.length > 0) {
        await routeApi.addCourses(created.id, pickedIds.value)
      }
      ElMessage.success('学习路线已创建')
    } else if (editingRouteId.value) {
      await routeApi.update(editingRouteId.value, payload)
      const removed = originalCourseIds.value.filter((id) => !pickedIds.value.includes(id))
      const added = pickedIds.value.filter((id) => !originalCourseIds.value.includes(id))
      for (const courseId of removed) {
        await routeApi.removeCourse(editingRouteId.value, courseId)
      }
      if (added.length > 0) {
        await routeApi.addCourses(editingRouteId.value, added)
      }
      if (pickedIds.value.length > 0) {
        await routeApi.sortCourses(editingRouteId.value, pickedIds.value)
      }
      ElMessage.success('学习路线已更新')
    }
    const currentDetailId = detail.value?.id
    routeDialogVisible.value = false
    await loadRoutes()
    if (currentDetailId) {
      await loadDetail(currentDetailId)
    }
  } finally {
    routeSubmitting.value = false
  }
}

async function handleStatusChange(route: RouteItem, enabled: boolean) {
  const targetStatus = enabled ? 1 : 0
  if (targetStatus === 0) {
    const confirmed = await ElMessageBox.confirm(
      `下架后，学员前台将不再展示路线「${route.name}」。确定下架吗？`,
      '下架路线',
      { confirmButtonText: '确认下架', cancelButtonText: '取消', type: 'warning' },
    ).catch(() => false)
    if (!confirmed) return
  }
  await routeApi.updateStatus(route.id, targetStatus)
  ElMessage.success(targetStatus === 1 ? '路线已启用' : '路线已下架')
  await refreshDetailAndRoutes()
}

async function removeRoute(route: RouteItem) {
  const confirmed = await ElMessageBox.confirm(
    `确定删除路线「${route.name}」吗？删除后学员前台将不再展示该路线，且不可恢复。`,
    '删除学习路线',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' },
  ).catch(() => false)
  if (!confirmed) return
  await routeApi.remove(route.id)
  ElMessage.success('学习路线已删除')
  if (detail.value?.id === route.id) {
    viewMode.value = 'list'
    detail.value = null
  }
  await loadRoutes()
}

async function openPreview(route: { id: number }) {
  previewVisible.value = true
  previewLoading.value = true
  previewDetail.value = null
  try {
    previewDetail.value = await routeApi.detail(route.id)
  } finally {
    previewLoading.value = false
  }
}

async function moveDetailCourse(course: RouteCourseItem, delta: number) {
  if (!detail.value) return
  const index = detail.value.courses.findIndex((item) => item.id === course.id)
  const target = index + delta
  if (index < 0 || target < 0 || target >= detail.value.courses.length) return
  const ids = detail.value.courses.map((item) => item.id)
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  await routeApi.sortCourses(detail.value.id, ids)
  await refreshDetailAndRoutes()
}

async function removeDetailCourse(course: RouteCourseItem) {
  if (!detail.value) return
  const confirmed = await ElMessageBox.confirm(
    `确定从路线中移除课程「${course.name}」吗？`,
    '移除课程',
    { confirmButtonText: '确认移除', cancelButtonText: '取消', type: 'warning' },
  ).catch(() => false)
  if (!confirmed) return
  await routeApi.removeCourse(detail.value.id, course.id)
  ElMessage.success('课程已从路线移除')
  await refreshDetailAndRoutes()
}

async function openAddCourses() {
  addSelectedIds.value = []
  addSearch.value = ''
  addDialogVisible.value = true
  await loadCourses()
}

function toggleAddCourse(courseId: number, checked: boolean) {
  if (checked) {
    if (!addSelectedIds.value.includes(courseId)) {
      addSelectedIds.value.push(courseId)
    }
  } else {
    addSelectedIds.value = addSelectedIds.value.filter((id) => id !== courseId)
  }
}

async function confirmAddCourses() {
  if (!detail.value || addSelectedIds.value.length === 0) return
  addSubmitting.value = true
  try {
    await routeApi.addCourses(detail.value.id, addSelectedIds.value)
    ElMessage.success(`已添加 ${addSelectedIds.value.length} 门课程`)
    addDialogVisible.value = false
    await refreshDetailAndRoutes()
  } finally {
    addSubmitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadRoutes(), loadCourses(), loadSubjects()])
})
</script>

<template>
  <div class="route-page">
    <template v-if="viewMode === 'list'">
      <PageHeader
        title="学习路线"
        description="为每个学科编排 B 站课程学习路径，支持启用/禁用与学员视角预览。"
      >
        <template #actions>
          <el-button v-permission="'route:add'" type="primary" @click="openCreateRoute">
            <el-icon><Plus /></el-icon>
            <span>新建路线</span>
          </el-button>
        </template>
      </PageHeader>

      <section class="stats-grid">
        <article v-for="card in routeStats" :key="card.label" class="stat-card">
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

      <div v-loading="loading" class="route-grid">
        <article v-for="route in routes" :key="route.id" class="route-card">
          <header class="route-card-head">
            <span
              class="route-icon"
              :style="{
                background: `${subjectColor(route.subjectId)}16`,
                color: subjectColor(route.subjectId),
              }"
            >
              {{ routeIcon(route) }}
            </span>
            <div class="route-title-area">
              <h2>{{ route.name }}</h2>
              <div class="route-subject">{{ route.subjectName }}</div>
            </div>
            <el-switch
              :model-value="route.status === 1"
              @change="handleStatusChange(route, Boolean($event))"
            />
          </header>

          <div class="route-tags">
            <el-tag :type="route.status === 1 ? 'success' : 'info'" size="small" effect="light">
              {{ route.status === 1 ? '已启用' : '已禁用' }}
            </el-tag>
            <el-tag size="small" effect="plain">
              {{ durationText(route.totalDurationMinutes) }}
            </el-tag>
            <el-tag size="small" effect="plain">{{ route.courseCount }} 门课程</el-tag>
            <el-tag v-if="route.targetEducation" size="small" effect="plain">
              {{ route.targetEducation }}
            </el-tag>
          </div>

          <p class="route-desc">{{ route.description || '暂无路线描述' }}</p>

          <div class="route-preview-courses">
            <div
              v-for="(course, index) in route.previewCourses"
              :key="course.id"
              class="preview-course"
            >
              <span class="preview-no">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="preview-name">{{ course.name }}</span>
              <span class="preview-duration">{{ course.durationMinutes }} 分</span>
            </div>
            <div v-if="route.courseCount === 0" class="preview-empty">暂无课程</div>
            <div v-if="route.courseCount > route.previewCourses.length" class="preview-more">
              还有 {{ route.courseCount - route.previewCourses.length }} 门课程
            </div>
          </div>

          <footer class="route-card-foot">
            <el-button size="small" plain @click="openDetail(route)">
              <el-icon><Setting /></el-icon>
              <span>课程管理</span>
            </el-button>
            <el-button size="small" plain @click="openPreview(route)">
              <el-icon><View /></el-icon>
              <span>预览</span>
            </el-button>
            <el-button
              v-permission="'route:edit'"
              size="small"
              plain
              :loading="routeOpening"
              @click="openEditRoute(route)"
            >
              <el-icon><Edit /></el-icon>
              <span>编辑</span>
            </el-button>
            <el-button
              v-permission="'route:delete'"
              size="small"
              text
              type="danger"
              @click="removeRoute(route)"
            >
              <el-icon><Delete /></el-icon>
              <span>删除</span>
            </el-button>
          </footer>
        </article>

        <el-empty
          v-if="!loading && routes.length === 0"
          class="route-empty"
          description="暂无学习路线，点击上方新建路线创建一条"
        />
      </div>
    </template>

    <template v-else>
      <div class="detail-topbar">
        <el-button text @click="backToList">
          <el-icon><Back /></el-icon>
          <span>返回路线列表</span>
        </el-button>
        <div class="topbar-spacer" />
        <el-button v-if="detail" plain @click="openPreview(detail)">
          <el-icon><View /></el-icon>
          <span>学员预览</span>
        </el-button>
        <el-button v-if="detail" v-permission="'route:edit'" type="primary" @click="openAddCourses">
          <el-icon><Plus /></el-icon>
          <span>从课程库添加</span>
        </el-button>
      </div>

      <div v-loading="detailLoading">
        <template v-if="detail">
          <PageHeader
            :title="`${detail.name} - 课程管理`"
            :description="`${detail.subjectName} · ${detail.status === 1 ? '已启用' : '已禁用'} · 共 ${detail.courseCount} 门课程`"
          />

          <section class="detail-card">
            <div class="detail-grid">
              <div class="detail-item">
                <span>关联学科</span><b>{{ detail.subjectName }}</b>
              </div>
              <div class="detail-item">
                <span>状态</span>
                <b>
                  <el-tag :type="detail.status === 1 ? 'success' : 'info'" size="small" effect="light">
                    {{ detail.status === 1 ? '已启用' : '已禁用' }}
                  </el-tag>
                </b>
              </div>
              <div class="detail-item">
                <span>课程数</span><b>{{ detail.courseCount }} 门</b>
              </div>
              <div class="detail-item">
                <span>总时长</span><b>{{ durationText(detail.totalDurationMinutes) }}</b>
              </div>
              <div class="detail-item">
                <span>适合学历</span><b>{{ detail.targetEducation || '—' }}</b>
              </div>
              <div class="detail-item">
                <span>路线图标</span><b>{{ detail.icon || '🗺' }}</b>
              </div>
              <div class="detail-item detail-item-full">
                <span>路线描述</span><b>{{ detail.description || '暂无描述' }}</b>
              </div>
            </div>
          </section>

          <section class="course-panel">
            <header class="panel-head">
              <div>
                <h2>路线课程</h2>
                <p>共 {{ detail.courseCount }} 门课程，总计时长 {{ durationText(detail.totalDurationMinutes) }}</p>
              </div>
            </header>

            <el-table
              :data="detail.courses"
              empty-text="暂无课程，点击右上角从课程库添加"
            >
              <el-table-column label="序号" width="62">
                <template #default="{ $index }">
                  <span class="row-index">{{ $index + 1 }}</span>
                </template>
              </el-table-column>
              <el-table-column label="BV 号" width="132">
                <template #default="{ row }">
                  <span class="muted-text">{{ row.bvid || '—' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="课程名称" min-width="300">
                <template #default="{ row }">
                  <div class="detail-course-name">{{ row.name }}</div>
                  <div class="course-sub">{{ courseSubjectNames(row) }}</div>
                </template>
              </el-table-column>
              <el-table-column label="时长" width="110">
                <template #default="{ row }">
                  <div class="duration-main">{{ durationTableText(row.durationMinutes) }}</div>
                  <div class="course-sub">{{ row.durationMinutes }} 分钟</div>
                </template>
              </el-table-column>
              <el-table-column label="排序" width="100">
                <template #default="{ row, $index }">
                  <div class="sort-actions">
                    <el-button
                      v-permission="'route:edit'"
                      link
                      :disabled="$index === 0"
                      @click="moveDetailCourse(row, -1)"
                    >
                      <el-icon><ArrowUp /></el-icon>
                    </el-button>
                    <el-button
                      v-permission="'route:edit'"
                      link
                      :disabled="$index === detail.courses.length - 1"
                      @click="moveDetailCourse(row, 1)"
                    >
                      <el-icon><ArrowDown /></el-icon>
                    </el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90">
                <template #default="{ row }">
                  <el-button
                    v-permission="'route:edit'"
                    link
                    type="danger"
                    @click="removeDetailCourse(row)"
                  >
                    <el-icon><Delete /></el-icon>
                    <span>移除</span>
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </template>
        <el-empty v-else-if="!detailLoading" description="路线不存在或已被删除" />
      </div>
    </template>

    <el-dialog
      v-model="routeDialogVisible"
      :title="routeMode === 'create' ? '新建学习路线' : '编辑学习路线'"
      width="920px"
      class="route-dialog"
      destroy-on-close
    >
      <p class="dialog-tip">填写基本信息，并按关联学科从课程库勾选课程；已选课程可调整学习顺序。</p>
      <el-form ref="routeFormRef" :model="routeForm" :rules="routeRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="路线名称" prop="name">
            <el-input v-model="routeForm.name" maxlength="100" placeholder="如：AI 应用开发工程师成长路线" />
          </el-form-item>
          <el-form-item label="关联学科" prop="subjectId">
            <el-select v-model="routeForm.subjectId" class="full-width">
              <el-option
                v-for="subject in subjects"
                :key="subject.id"
                :label="subject.name"
                :value="subject.id"
              />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-grid form-grid-three">
          <el-form-item label="路线图标">
            <el-input v-model="routeForm.icon" maxlength="8" placeholder="如：🗺" />
          </el-form-item>
          <el-form-item label="适合学历">
            <el-input v-model="routeForm.targetEducation" maxlength="100" placeholder="如：零基础转行 / 本科" />
          </el-form-item>
          <el-form-item label="状态">
            <div class="status-switch">
              <el-switch
                v-model="routeForm.status"
                :active-value="1"
                :inactive-value="0"
              />
              <span>{{ routeForm.status === 1 ? '启用' : '禁用' }}</span>
            </div>
          </el-form-item>
        </div>
        <el-form-item label="路线描述">
          <el-input
            v-model="routeForm.description"
            type="textarea"
            :rows="2"
            maxlength="1000"
            show-word-limit
            placeholder="介绍这条路线的学习目标与路径"
          />
        </el-form-item>

        <div class="picker-label">路线课程</div>
        <div class="course-picker">
          <div class="picker-panel">
            <div class="picker-head">
              <span>可选课程</span>
              <span class="picker-hint">{{ availableCourses.length }} 门</span>
            </div>
            <el-input
              v-model="availableSearch"
              size="small"
              clearable
              placeholder="搜索本学科课程"
              class="picker-search"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <div class="picker-list">
              <label
                v-for="course in availableCourses"
                :key="course.id"
                class="picker-course"
                :class="{ picked: isPicked(course.id) }"
              >
                <el-checkbox
                  :model-value="isPicked(course.id)"
                  @change="togglePicked(course.id, Boolean($event))"
                />
                <div class="picker-course-main">
                  <div class="picker-course-title">{{ course.name }}</div>
                  <div class="picker-course-meta">
                    {{ durationTableText(course.durationMinutes) }}
                    <span v-if="course.bvid"> · {{ course.bvid }}</span>
                  </div>
                </div>
              </label>
              <div v-if="availableCourses.length === 0" class="picker-empty">
                该学科下暂无可选课程，请先在课程库添加课程
              </div>
            </div>
          </div>

          <div class="picker-panel">
            <div class="picker-head">
              <span>已选课程</span>
              <span class="picker-hint">{{ pickedCourses.length }} 门</span>
            </div>
            <div class="picker-list picked-list">
              <div
                v-for="(course, index) in pickedCourses"
                :key="course.id"
                class="picked-course"
              >
                <span class="picked-no">{{ index + 1 }}</span>
                <div class="picker-course-main">
                  <div class="picker-course-title">{{ course.name }}</div>
                  <div class="picker-course-meta">
                    {{ course.missing ? '课程已删除' : `${durationTableText(course.durationMinutes)}${course.bvid ? ` · ${course.bvid}` : ''}` }}
                  </div>
                </div>
                <el-button link :disabled="index === 0" @click="movePicked(index, -1)">
                  <el-icon><ArrowUp /></el-icon>
                </el-button>
                <el-button
                  link
                  :disabled="index === pickedCourses.length - 1"
                  @click="movePicked(index, 1)"
                >
                  <el-icon><ArrowDown /></el-icon>
                </el-button>
                <el-button link type="danger" @click="removePicked(course.id)">
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
              <div v-if="pickedCourses.length === 0" class="picker-empty">
                从左侧勾选课程加入路线，建议按学习先后顺序添加
              </div>
            </div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="routeDialogVisible = false">
          <el-icon><Close /></el-icon>
          <span>取消</span>
        </el-button>
        <el-button type="primary" :loading="routeSubmitting" @click="submitRoute">
          <el-icon><Check /></el-icon>
          <span>保存路线</span>
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addDialogVisible" title="从课程库添加" width="760px" destroy-on-close>
      <p class="dialog-tip">按学科分组展示课程，已添加到当前路线的课程不可重复选择。</p>
      <el-input
        v-model="addSearch"
        clearable
        placeholder="搜索课程名称"
        class="add-search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>

      <div class="add-course-groups">
        <section
          v-for="group in [
            { title: '本学科课程', courses: addCourseGroups.local, cross: false },
            { title: '其他学科课程（跨学科补充）', courses: addCourseGroups.other, cross: true },
          ]"
          :key="group.title"
          class="add-course-group"
        >
          <div v-if="group.courses.length" class="add-group-title">
            {{ group.title }} <span>{{ group.courses.length }} 门</span>
          </div>
          <label
            v-for="course in group.courses"
            :key="course.id"
            class="add-course-row"
            :class="{ added: course.added }"
          >
            <el-checkbox
              :model-value="addSelectedIds.includes(course.id)"
              :disabled="course.added"
              @change="toggleAddCourse(course.id, Boolean($event))"
            />
            <div class="add-course-main">
              <div class="add-course-title">{{ course.name }}</div>
              <div class="add-course-meta">
                <span v-if="group.cross">{{ course.primarySubject?.name || '其他学科' }} · </span>
                {{ durationTableText(course.durationMinutes) }}
                <span v-if="course.bvid"> · {{ course.bvid }}</span>
              </div>
            </div>
            <el-tag v-if="course.added" size="small" type="info" effect="plain">已添加</el-tag>
          </label>
        </section>
        <el-empty
          v-if="addCourseGroups.local.length === 0 && addCourseGroups.other.length === 0"
          :image-size="54"
          description="没有找到匹配的课程"
        />
      </div>

      <template #footer>
        <span class="add-selected-hint">已选择 {{ addSelectedIds.length }} 门课程</span>
        <el-button @click="addDialogVisible = false">
          <el-icon><Close /></el-icon>
          <span>取消</span>
        </el-button>
        <el-button
          type="primary"
          :loading="addSubmitting"
          :disabled="addSelectedIds.length === 0"
          @click="confirmAddCourses"
        >
          <el-icon><Check /></el-icon>
          <span>完成添加</span>
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="学员视角预览" width="760px" destroy-on-close>
      <div v-loading="previewLoading" class="preview-body">
        <template v-if="previewDetail">
          <div
            class="preview-hero"
            :style="{
              background: `linear-gradient(135deg, ${subjectColor(previewDetail.subjectId)}, #7c3aed)`,
            }"
          >
            <span class="preview-hero-icon">{{ routeIcon(previewDetail) }}</span>
            <div class="preview-kicker">{{ previewDetail.subjectName }} · 学习路线</div>
            <h2>{{ previewDetail.name }}</h2>
            <p>{{ previewDetail.description || '暂无路线描述' }}</p>
            <div class="preview-hero-meta">
              <span>👥 {{ previewDetail.targetEducation || '所有学员' }}</span>
              <span>🎬 {{ previewDetail.courseCount }} 门课程</span>
              <span>⏱ {{ durationText(previewDetail.totalDurationMinutes) }}</span>
            </div>
          </div>
          <div class="preview-list-head">
            <span>课程清单</span>
            <el-tag :type="previewDetail.status === 1 ? 'success' : 'info'" size="small" effect="light">
              {{ previewDetail.status === 1 ? '已启用' : '已禁用' }}
            </el-tag>
          </div>
          <div v-if="previewDetail.courses.length" class="preview-course-list">
            <div
              v-for="(course, index) in previewDetail.courses"
              :key="course.id"
              class="preview-course-row"
            >
              <span class="preview-course-no">{{ String(index + 1).padStart(2, '0') }}</span>
              <div class="preview-course-main">
                <div class="preview-course-title">{{ course.name }}</div>
                <div class="preview-course-meta">
                  {{ course.bvid || '—' }} · {{ course.durationMinutes }} 分钟
                </div>
              </div>
              <a
                v-if="courseLink(course)"
                :href="courseLink(course) || undefined"
                target="_blank"
                rel="noopener noreferrer"
                class="preview-course-link"
              >
                <el-icon><VideoPlay /></el-icon>
                <span>去 B 站学习</span>
              </a>
            </div>
          </div>
          <el-empty v-else :image-size="54" description="该路线还没有配置课程" />
        </template>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">
          <el-icon><Close /></el-icon>
          <span>关闭</span>
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.route-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

:deep(.route-dialog .el-dialog__body) {
  max-height: calc(100vh - 210px);
  overflow-y: auto;
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
  font-size: 25px;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.stat-hint {
  color: var(--muted-2);
  font-size: 11.5px;
}

.route-grid {
  display: grid;
  min-height: 180px;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.route-card {
  display: flex;
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 16px;
  flex-direction: column;
  background: var(--surface);
}

.route-card-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.route-icon {
  display: grid;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  flex: none;
  place-items: center;
  font-size: 21px;
}

.route-title-area {
  min-width: 0;
  flex: 1;
}

.route-title-area h2 {
  margin: 0;
  overflow: hidden;
  font-size: 16px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-subject {
  margin-top: 3px;
  color: var(--muted);
  font-size: 11.5px;
}

.route-tags {
  display: flex;
  margin: 12px 0 9px;
  gap: 6px;
  flex-wrap: wrap;
}

.route-desc {
  display: -webkit-box;
  min-height: 38px;
  margin: 0 0 12px;
  overflow: hidden;
  color: var(--text-2);
  font-size: 12.5px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.route-preview-courses {
  display: flex;
  min-height: 94px;
  padding: 9px 11px;
  border-radius: 10px;
  flex-direction: column;
  background: var(--surface-2);
}

.preview-course {
  display: flex;
  padding: 4px 0;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.preview-no {
  width: 22px;
  color: var(--muted-2);
  font-size: 10.5px;
  font-weight: 650;
  flex: none;
}

.preview-name {
  overflow: hidden;
  min-width: 0;
  flex: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-duration {
  color: var(--muted-2);
  font-size: 10.5px;
  flex: none;
}

.preview-empty,
.preview-more {
  padding: 20px 0;
  color: var(--muted-2);
  font-size: 11.5px;
  text-align: center;
}

.preview-more {
  padding: 4px 0;
  text-align: left;
}

.route-card-foot {
  display: flex;
  margin-top: 14px;
  padding-top: 13px;
  border-top: 1px solid var(--line);
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.route-card-foot :deep(.el-button + .el-button) {
  margin-left: 0;
}

.route-empty {
  grid-column: 1 / -1;
  border: 1px dashed var(--line);
  border-radius: 16px;
  background: var(--surface);
}

.detail-topbar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.topbar-spacer {
  flex: 1;
}

.detail-card {
  margin-bottom: 16px;
  padding: 18px 20px;
  border: 1px solid var(--line);
  border-radius: 15px;
  background: var(--surface);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px 20px;
}

.detail-item {
  display: flex;
  padding: 9px 11px;
  border-radius: 9px;
  justify-content: space-between;
  gap: 12px;
  background: var(--surface-2);
  font-size: 12.5px;
}

.detail-item span {
  color: var(--muted);
}

.detail-item b {
  color: var(--text-2);
  font-weight: 600;
}

.detail-item-full {
  grid-column: 1 / -1;
}

.course-panel {
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--surface);
}

.panel-head {
  margin-bottom: 14px;
}

.panel-head h2 {
  margin: 0 0 4px;
  font-size: 15px;
}

.panel-head p {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
}

.row-index {
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}

.muted-text {
  color: var(--muted);
  font-size: 12px;
}

.detail-course-name {
  overflow: hidden;
  font-size: 13px;
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

.sort-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.sort-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.dialog-tip {
  margin: 0 0 16px;
  color: var(--muted);
  font-size: 12.5px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.form-grid-three {
  grid-template-columns: 1fr 1.4fr 0.7fr;
}

.full-width {
  width: 100%;
}

.status-switch {
  display: flex;
  height: 32px;
  align-items: center;
  gap: 9px;
  color: var(--muted);
  font-size: 12.5px;
}

.picker-label {
  margin-bottom: 8px;
  color: var(--text-2);
  font-size: 13px;
  font-weight: 600;
}

.course-picker {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.picker-panel {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fcfcfe;
}

.picker-head {
  display: flex;
  padding: 10px 13px;
  border-bottom: 1px solid var(--line);
  align-items: center;
  justify-content: space-between;
  background: #f7f8fc;
  color: var(--text-2);
  font-size: 12.5px;
  font-weight: 650;
}

.picker-hint {
  color: var(--muted-2);
  font-size: 11.5px;
  font-weight: 400;
}

.picker-search {
  margin: 8px;
  width: calc(100% - 16px);
}

.picker-list {
  height: 280px;
  padding: 4px 8px 8px;
  overflow-y: auto;
}

.picked-list {
  padding-top: 8px;
}

.picker-course {
  display: flex;
  padding: 7px 8px;
  border-radius: 8px;
  align-items: flex-start;
  gap: 8px;
  cursor: pointer;
}

.picker-course:hover {
  background: #f5f6fb;
}

.picker-course.picked {
  background: var(--brand-soft);
}

.picker-course-main {
  min-width: 0;
  flex: 1;
}

.picker-course-title {
  overflow: hidden;
  font-size: 12.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.picker-course-meta {
  margin-top: 2px;
  color: var(--muted);
  font-size: 11px;
}

.picked-course {
  display: flex;
  padding: 7px 6px;
  border-bottom: 1px dashed var(--line);
  align-items: center;
  gap: 6px;
}

.picked-course:last-child {
  border-bottom: 0;
}

.picked-no {
  display: grid;
  width: 20px;
  height: 20px;
  border-radius: 5px;
  flex: none;
  place-items: center;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 10.5px;
  font-weight: 700;
}

.picked-course :deep(.el-button) {
  padding: 2px 4px;
}

.picker-empty {
  padding: 30px 14px;
  color: var(--muted-2);
  font-size: 12px;
  line-height: 1.8;
  text-align: center;
}

.add-search {
  margin-bottom: 12px;
}

.add-course-groups {
  max-height: 52vh;
  padding-right: 3px;
  overflow-y: auto;
}

.add-course-group {
  margin-bottom: 14px;
}

.add-group-title {
  margin-bottom: 6px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 700;
}

.add-group-title span {
  color: var(--muted-2);
  font-weight: 400;
}

.add-course-row {
  display: flex;
  padding: 8px 10px;
  border-radius: 9px;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.add-course-row:hover {
  background: var(--surface-2);
}

.add-course-row.added {
  opacity: 0.62;
  cursor: default;
}

.add-course-main {
  min-width: 0;
  flex: 1;
}

.add-course-title {
  overflow: hidden;
  font-size: 12.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.add-course-meta {
  margin-top: 2px;
  color: var(--muted);
  font-size: 11px;
}

.add-selected-hint {
  margin-right: auto;
  color: var(--muted);
  font-size: 12px;
}

.preview-body {
  min-height: 120px;
  max-height: 68vh;
  overflow-y: auto;
}

.preview-hero {
  position: relative;
  overflow: hidden;
  padding: 24px;
  border-radius: 14px;
  color: #fff;
}

.preview-hero-icon {
  display: grid;
  width: 44px;
  height: 44px;
  margin-bottom: 11px;
  border-radius: 12px;
  place-items: center;
  background: rgba(255, 255, 255, 0.16);
  font-size: 22px;
}

.preview-kicker {
  opacity: 0.75;
  font-size: 11px;
  letter-spacing: 0.08em;
}

.preview-hero h2 {
  margin: 5px 0 0;
  font-size: 21px;
  font-weight: 800;
}

.preview-hero p {
  max-width: 560px;
  margin: 7px 0 0;
  opacity: 0.82;
  font-size: 12.5px;
  line-height: 1.75;
}

.preview-hero-meta {
  display: flex;
  margin-top: 14px;
  gap: 18px;
  flex-wrap: wrap;
  opacity: 0.9;
  font-size: 12px;
}

.preview-list-head {
  display: flex;
  margin: 18px 0 8px;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 650;
}

.preview-course-row {
  display: flex;
  padding: 11px 6px;
  border-bottom: 1px dashed var(--line);
  align-items: center;
  gap: 12px;
}

.preview-course-row:last-child {
  border-bottom: 0;
}

.preview-course-no {
  display: grid;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  flex: none;
  place-items: center;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 11px;
  font-weight: 800;
}

.preview-course-main {
  min-width: 0;
  flex: 1;
}

.preview-course-title {
  overflow: hidden;
  font-size: 12.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-course-meta {
  margin-top: 2px;
  color: var(--muted);
  font-size: 11px;
}

.preview-course-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--brand);
  font-size: 11.5px;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .course-picker {
    grid-template-columns: 1fr;
  }

  .detail-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .route-grid {
    grid-template-columns: 1fr;
  }

  .form-grid,
  .form-grid-three,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
