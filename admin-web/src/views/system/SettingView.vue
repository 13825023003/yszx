<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  ElMessage,
  ElMessageBox,
  type UploadRequestOptions,
} from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { fileApi, settingApi } from '@/api/system'
import type { SystemSettings } from '@/types/system'
import { useSettingStore } from '@/store/setting'

const route = useRoute()
const settingStore = useSettingStore()

const form = reactive({
  siteName: '',
  siteDescription: '',
  logoUrl: '',
  defaultAvatar: '',
})
const snapshot = ref('')
const loading = ref(false)
const saving = ref(false)
const resetting = ref(false)
const uploading = reactive({ logo: false, avatar: false })

const dirty = computed(() => JSON.stringify(form) !== snapshot.value)

function fill(data: SystemSettings) {
  form.siteName = data.siteName
  form.siteDescription = data.siteDescription ?? ''
  form.logoUrl = data.logoUrl ?? ''
  form.defaultAvatar = data.defaultAvatar ?? ''
  snapshot.value = JSON.stringify(form)
}

async function load() {
  loading.value = true
  try {
    const data = await settingApi.get()
    fill(data)
    settingStore.apply(data)
  } finally {
    loading.value = false
  }
}

async function handleUpload(options: UploadRequestOptions, type: 'logo' | 'avatar') {
  const file = options.file as File
  if (!/^image\/(jpeg|png)$/.test(file.type)) {
    ElMessage.error('仅支持 JPG / PNG 格式')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 2MB')
    return
  }
  uploading[type] = true
  try {
    const data = await fileApi.uploadImage(file)
    if (type === 'logo') {
      form.logoUrl = data.url
    } else {
      form.defaultAvatar = data.url
    }
    options.onSuccess?.(data)
    ElMessage.success('图片上传成功')
  } catch (error) {
    options.onError?.(error as Parameters<NonNullable<typeof options.onError>>[0])
  } finally {
    uploading[type] = false
  }
}

function uploadLogo(options: UploadRequestOptions) {
  return handleUpload(options, 'logo')
}

function uploadAvatar(options: UploadRequestOptions) {
  return handleUpload(options, 'avatar')
}

function removeImage(type: 'logo' | 'avatar') {
  if (type === 'logo') {
    form.logoUrl = ''
  } else {
    form.defaultAvatar = ''
  }
}

async function save() {
  if (!form.siteName.trim()) {
    ElMessage.error('平台名称不能为空')
    return
  }
  if (form.siteName.trim().length > 30) {
    ElMessage.error('平台名称不能超过 30 个字符')
    return
  }
  if (form.siteDescription.length > 200) {
    ElMessage.error('平台描述不能超过 200 个字符')
    return
  }
  saving.value = true
  try {
    const data = await settingApi.update({
      siteName: form.siteName.trim(),
      siteDescription: form.siteDescription.trim(),
      logoUrl: form.logoUrl || null,
      defaultAvatar: form.defaultAvatar || null,
    })
    fill(data)
    settingStore.apply(data)
    document.title = `${route.meta.title ?? '系统设置'} · ${data.siteName} · 运营后台`
    ElMessage.success('设置已保存')
  } finally {
    saving.value = false
  }
}

async function reset() {
  const confirmed = await ElMessageBox.confirm(
    '确定将平台名称、描述与品牌图片恢复为默认值吗？恢复后将立即生效。',
    '恢复默认设置',
    { confirmButtonText: '恢复默认', cancelButtonText: '取消', type: 'warning' },
  ).catch(() => false)
  if (!confirmed) return
  resetting.value = true
  try {
    const data = await settingApi.reset()
    fill(data)
    settingStore.apply(data)
    document.title = `${route.meta.title ?? '系统设置'} · ${data.siteName} · 运营后台`
    ElMessage.success('已恢复默认设置')
  } finally {
    resetting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="setting-page">
    <PageHeader title="系统设置" description="配置平台的基本信息与展示内容，保存后对学员前台生效。" />

    <section v-loading="loading" class="panel">
      <header class="panel-head">
        <div>
          <h2>基本信息设置</h2>
          <p>平台名称、描述与品牌图片将展示在浏览器标题、导航 Logo 与学员头像位置。</p>
        </div>
        <span v-if="dirty" class="dirty-hint">有未保存的修改</span>
      </header>

      <div class="setting-grid">
        <div class="setting-form">
          <div class="field">
            <label class="field-label required">平台名称</label>
            <el-input v-model="form.siteName" maxlength="30" show-word-limit placeholder="请输入平台名称" />
            <div class="field-hint">显示在浏览器标题与 Logo 旁，最多 30 个字符。</div>
          </div>

          <div class="field">
            <label class="field-label">平台描述</label>
            <el-input
              v-model="form.siteDescription"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="简要介绍平台的定位与价值（选填）"
            />
            <div class="field-hint">用于展示平台定位，最多 200 个字符。</div>
          </div>

          <div class="field">
            <label class="field-label">Logo</label>
            <div class="upload-row">
              <div class="upload-preview logo-preview">
                <img v-if="form.logoUrl" :src="form.logoUrl" alt="Logo" />
                <span v-else>云</span>
              </div>
              <div class="upload-actions">
                <div class="upload-buttons">
                  <el-upload
                    :show-file-list="false"
                    accept="image/jpeg,image/png"
                    :http-request="uploadLogo"
                  >
                    <el-button :loading="uploading.logo">
                      <el-icon><UploadFilled /></el-icon>
                      <span>上传 Logo</span>
                    </el-button>
                  </el-upload>
                  <el-button v-if="form.logoUrl" text type="danger" @click="removeImage('logo')">
                    <el-icon><Delete /></el-icon>
                    <span>移除</span>
                  </el-button>
                </div>
                <div class="field-hint">建议 256×256 正方形图片，支持 JPG / PNG，不超过 2MB。</div>
              </div>
            </div>
          </div>

          <div class="field">
            <label class="field-label">默认头像</label>
            <div class="upload-row">
              <div class="upload-preview avatar-preview">
                <img v-if="form.defaultAvatar" :src="form.defaultAvatar" alt="默认头像" />
                <span v-else>👤</span>
              </div>
              <div class="upload-actions">
                <div class="upload-buttons">
                  <el-upload
                    :show-file-list="false"
                    accept="image/jpeg,image/png"
                    :http-request="uploadAvatar"
                  >
                    <el-button :loading="uploading.avatar">
                      <el-icon><UploadFilled /></el-icon>
                      <span>上传默认头像</span>
                    </el-button>
                  </el-upload>
                  <el-button
                    v-if="form.defaultAvatar"
                    text
                    type="danger"
                    @click="removeImage('avatar')"
                  >
                    <el-icon><Delete /></el-icon>
                    <span>移除</span>
                  </el-button>
                </div>
                <div class="field-hint">用户未设置头像时展示，建议 256×256，支持 JPG / PNG。</div>
              </div>
            </div>
          </div>
        </div>

        <aside class="preview-panel">
          <div class="preview-label">展示效果</div>
          <div class="preview-brand">
            <span class="preview-logo">
              <img v-if="form.logoUrl" :src="form.logoUrl" alt="Logo" />
              <span v-else>云</span>
            </span>
            <div class="preview-meta">
              <div class="preview-name">{{ form.siteName || '未命名平台' }}</div>
              <div class="preview-desc">{{ form.siteDescription || '暂无平台描述' }}</div>
            </div>
          </div>

          <div class="preview-tab">
            <div class="tab-bar">
              <span /><span /><span />
              <div class="tab-title">{{ form.siteName || '未命名平台' }} - AI 智能学习平台</div>
            </div>
          </div>

          <div class="preview-avatar-row">
            <span class="preview-avatar">
              <img v-if="form.defaultAvatar" :src="form.defaultAvatar" alt="默认头像" />
              <span v-else>👤</span>
            </span>
            <div>
              <b>默认头像</b>
              <div class="preview-tip">未设置头像的学员将展示该图片</div>
            </div>
          </div>
          <p class="preview-note">保存后，浏览器标题、后台导航与学员端将同步更新。</p>
        </aside>
      </div>

      <footer class="panel-foot">
        <span v-if="dirty" class="foot-hint">当前有未保存的修改</span>
        <el-button :loading="resetting" @click="reset">
          <el-icon><RefreshLeft /></el-icon>
          <span>恢复默认</span>
        </el-button>
        <el-button type="primary" :loading="saving" @click="save">
          <el-icon><Check /></el-icon>
          <span>保存设置</span>
        </el-button>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.setting-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--surface);
}

.panel-head {
  display: flex;
  padding: 22px 24px 18px;
  border-bottom: 1px solid var(--line);
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panel-head h2 {
  margin: 0 0 5px;
  font-size: 16px;
}

.panel-head p {
  margin: 0;
  color: var(--muted);
  font-size: 12.5px;
  line-height: 1.7;
}

.dirty-hint {
  padding: 3px 10px;
  border-radius: 999px;
  background: #fffaeb;
  color: var(--warn);
  font-size: 11.5px;
  white-space: nowrap;
}

.setting-grid {
  display: grid;
  padding: 24px;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 32px;
}

.setting-form {
  display: flex;
  max-width: 640px;
  flex-direction: column;
  gap: 26px;
}

.field-label {
  display: block;
  margin-bottom: 8px;
  color: var(--text-2);
  font-size: 13px;
  font-weight: 600;
}

.field-label.required::after {
  margin-left: 3px;
  color: var(--danger);
  content: '*';
}

.field-hint {
  margin-top: 6px;
  color: var(--muted-2);
  font-size: 11.5px;
  line-height: 1.6;
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.upload-preview {
  display: grid;
  width: 76px;
  height: 76px;
  border: 1px dashed #d0d5dd;
  border-radius: 15px;
  flex: none;
  place-items: center;
  overflow: hidden;
  background: var(--surface-2);
}

.upload-preview.logo-preview {
  border-radius: 15px;
}

.upload-preview.avatar-preview {
  border-radius: 50%;
}

.upload-preview img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-preview span {
  color: var(--brand);
  font-size: 26px;
  font-weight: 800;
}

.upload-preview.avatar-preview span {
  font-size: 28px;
}

.upload-actions {
  min-width: 0;
}

.upload-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-panel {
  align-self: start;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--surface-2);
}

.preview-label {
  margin-bottom: 12px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.preview-brand {
  display: flex;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 12px;
  align-items: center;
  gap: 12px;
  background: #fff;
}

.preview-logo {
  display: grid;
  width: 44px;
  height: 44px;
  border-radius: 11px;
  flex: none;
  place-items: center;
  overflow: hidden;
  background: var(--grad);
  color: #fff;
  font-size: 19px;
  font-weight: 800;
}

.preview-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-meta {
  min-width: 0;
}

.preview-name {
  overflow: hidden;
  font-size: 15px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-desc {
  margin-top: 2px;
  overflow: hidden;
  color: var(--muted);
  font-size: 11.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-tab {
  margin-top: 12px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fff;
}

.tab-bar {
  display: flex;
  padding: 8px 10px;
  border-bottom: 1px solid var(--line);
  align-items: center;
  gap: 5px;
}

.tab-bar > span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #e4e7ec;
  flex: none;
}

.tab-title {
  margin-left: 6px;
  overflow: hidden;
  color: var(--muted);
  font-size: 10.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-avatar-row {
  display: flex;
  margin-top: 12px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 10px;
  align-items: center;
  gap: 12px;
  background: #fff;
  font-size: 12.5px;
}

.preview-avatar {
  display: grid;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  flex: none;
  place-items: center;
  overflow: hidden;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 17px;
}

.preview-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-tip {
  margin-top: 2px;
  color: var(--muted);
  font-size: 11.5px;
}

.preview-note {
  margin: 12px 0 0;
  color: var(--muted-2);
  font-size: 11px;
  line-height: 1.7;
}

.panel-foot {
  display: flex;
  padding: 15px 24px;
  border-top: 1px solid var(--line);
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  background: #fcfcfd;
}

.foot-hint {
  margin-right: auto;
  color: var(--warn);
  font-size: 12px;
}

@media (max-width: 980px) {
  .setting-grid {
    grid-template-columns: 1fr;
    gap: 22px;
  }

  .preview-panel {
    order: -1;
  }
}

@media (max-width: 640px) {
  .setting-grid {
    padding: 20px;
  }

  .upload-row {
    align-items: flex-start;
  }
}
</style>
