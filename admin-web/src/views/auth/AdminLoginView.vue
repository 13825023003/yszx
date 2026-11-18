<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { ApiError } from '@/api/request'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const form = reactive({
  account: '',
  password: '',
})
const rules: FormRules = {
  account: [{ required: true, message: '请输入用户名或手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}
const loading = ref(false)
const fieldMessage = ref('')

async function submit() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  fieldMessage.value = ''
  try {
    await userStore.login({
      account: form.account.trim(),
      password: form.password,
    })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.replace(redirect.startsWith('/') ? redirect : '/dashboard')
  } catch (error) {
    if (error instanceof ApiError && error.fieldErrors?.length) {
      fieldMessage.value = error.fieldErrors.map((item) => item.reason).join('；')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-glow login-glow--a" aria-hidden="true" />
    <div class="login-glow login-glow--b" aria-hidden="true" />
    <div class="login-grid" aria-hidden="true" />

    <div class="login-shell">
      <div class="login-brand">
        <span class="brand-mark">云</span>
        <span class="brand-name">云枢智学</span>
        <span class="brand-tag">运营后台</span>
      </div>

      <section class="login-card">
        <header class="login-head">
          <span class="eyebrow">ADMIN CONSOLE</span>
          <h1>欢迎回来</h1>
          <p>请使用后台账号登录，继续你的运营工作。</p>
        </header>

        <el-alert
          v-if="fieldMessage"
          :title="fieldMessage"
          type="error"
          show-icon
          :closable="false"
          class="form-alert"
        />

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="login-form"
          @submit.prevent="submit"
        >
          <el-form-item label="账号" prop="account">
            <el-input
              v-model="form.account"
              size="large"
              placeholder="用户名 / 手机号"
              :prefix-icon="User"
              clearable
              autocomplete="username"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              size="large"
              placeholder="请输入登录密码"
              :prefix-icon="Lock"
              show-password
              autocomplete="current-password"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            class="submit-button"
            :loading="loading"
            @click="submit"
          >
            <el-icon><Unlock /></el-icon>
            <span>登 录</span>
          </el-button>
        </el-form>

        <div class="login-foot">
          <span class="foot-dot" aria-hidden="true" />
          后台账号由超级管理员统一分配，如需重置密码请联系管理员。
        </div>
      </section>

      <p class="login-copyright">© 2026 云枢智学 · 运营后台</p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  display: grid;
  min-height: 100%;
  padding: 40px 24px;
  place-items: center;
  overflow: hidden;
  background:
    radial-gradient(circle at 16% 18%, rgba(124, 58, 237, 0.26), transparent 42%),
    radial-gradient(circle at 84% 82%, rgba(79, 70, 229, 0.32), transparent 46%),
    linear-gradient(145deg, #0a0f1f 0%, #101828 46%, #141033 100%);
}

.login-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.07) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: radial-gradient(ellipse at center, #000 20%, transparent 72%);
  pointer-events: none;
}

.login-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(12px);
  pointer-events: none;
}

.login-glow--a {
  top: -160px;
  right: -120px;
  width: 420px;
  height: 420px;
  background: radial-gradient(circle, rgba(124, 58, 237, 0.34), transparent 68%);
}

.login-glow--b {
  bottom: -200px;
  left: -140px;
  width: 480px;
  height: 480px;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.28), transparent 70%);
}

.login-shell {
  position: relative;
  z-index: 1;
  display: flex;
  width: min(430px, 100%);
  flex-direction: column;
  align-items: center;
}

.login-brand {
  display: flex;
  margin-bottom: 26px;
  align-items: center;
  gap: 12px;
  color: #fff;
}

.brand-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 13px;
  background: var(--grad);
  box-shadow: 0 10px 24px rgba(79, 70, 229, 0.42);
  font-size: 19px;
  font-weight: 700;
}

.brand-name {
  font-size: 18px;
  font-weight: 650;
  letter-spacing: 1.5px;
}

.brand-tag {
  padding: 3px 9px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.68);
  font-size: 11px;
  letter-spacing: 1px;
}

.login-card {
  position: relative;
  width: 100%;
  padding: 36px 36px 30px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.985);
  box-shadow:
    0 32px 80px rgba(2, 6, 23, 0.48),
    0 2px 10px rgba(2, 6, 23, 0.12);
  animation: card-rise 0.5s ease-out both;
}

.login-card::before {
  position: absolute;
  top: 0;
  right: 36px;
  left: 36px;
  height: 3px;
  border-radius: 0 0 3px 3px;
  background: var(--grad);
  content: '';
  opacity: 0.9;
}

.login-head {
  margin-bottom: 26px;
}

.eyebrow {
  display: inline-block;
  margin-bottom: 10px;
  color: var(--brand);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 2.4px;
}

.login-head h1 {
  margin: 0 0 8px;
  color: var(--text);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.login-head p {
  margin: 0;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.7;
}

.form-alert {
  margin-bottom: 18px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-form-item__label) {
  padding-bottom: 7px;
  color: var(--text-2);
  font-size: 13px;
  font-weight: 600;
}

.login-form :deep(.el-input__wrapper) {
  height: 46px;
  padding: 0 14px;
  border-radius: 12px;
  background: #f8fafc;
  box-shadow: 0 0 0 1px #e4e7ec inset;
  transition:
    background 0.2s,
    box-shadow 0.2s;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #cbd5e1 inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow:
    0 0 0 1px var(--brand) inset,
    0 0 0 4px rgba(79, 70, 229, 0.1);
}

.login-form :deep(.el-input__prefix-inner .el-icon) {
  color: var(--muted-2);
  font-size: 17px;
}

.login-form :deep(.el-input__inner) {
  font-size: 14px;
}

.submit-button.el-button--primary {
  width: 100%;
  height: 46px;
  margin-top: 6px;
  border: 0;
  border-radius: 12px;
  background: var(--grad);
  box-shadow: 0 10px 24px rgba(79, 70, 229, 0.3);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  transition:
    transform 0.2s,
    box-shadow 0.2s,
    filter 0.2s;
}

.submit-button.el-button--primary:hover,
.submit-button.el-button--primary:focus {
  background: var(--grad);
  box-shadow: 0 14px 30px rgba(79, 70, 229, 0.38);
  filter: brightness(1.05);
  transform: translateY(-1px);
}

.submit-button.el-button--primary:active {
  transform: translateY(0);
}

.submit-button.el-button--primary.is-loading {
  letter-spacing: 1px;
}

.login-foot {
  display: flex;
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
  align-items: flex-start;
  gap: 8px;
  color: var(--muted-2);
  font-size: 12px;
  line-height: 1.6;
}

.foot-dot {
  width: 6px;
  height: 6px;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--brand);
  flex: none;
  opacity: 0.7;
}

.login-copyright {
  margin: 24px 0 0;
  color: rgba(203, 213, 225, 0.4);
  font-size: 12px;
  letter-spacing: 0.6px;
}

@keyframes card-rise {
  from {
    opacity: 0;
    transform: translateY(14px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 520px) {
  .login-page {
    padding: 28px 18px;
  }

  .login-brand {
    margin-bottom: 20px;
  }

  .brand-name {
    font-size: 16px;
  }

  .login-card {
    padding: 30px 22px 24px;
    border-radius: 18px;
  }

  .login-card::before {
    right: 22px;
    left: 22px;
  }

  .login-head h1 {
    font-size: 24px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-card {
    animation: none;
  }
}
</style>
