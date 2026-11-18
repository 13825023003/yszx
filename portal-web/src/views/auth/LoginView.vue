<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AuthShell from '@/components/AuthShell.vue'
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
  account: [{ required: true, message: '请输入手机号或邮箱', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6~20 位', trigger: 'blur' },
  ],
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
    await userStore.loginByPassword({
      account: form.account.trim(),
      password: form.password,
    })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect.startsWith('/') ? redirect : '/')
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
  <AuthShell title="登录云枢智学" subtitle="使用手机号或邮箱登录，继续你的学习与面试训练。">
    <el-alert
      v-if="fieldMessage"
      :title="fieldMessage"
      type="error"
      show-icon
      :closable="false"
      class="form-alert"
    />

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
      <el-form-item label="手机号 / 邮箱" prop="account">
        <el-input
          v-model="form.account"
          placeholder="请输入手机号或邮箱"
          size="large"
          clearable
          autocomplete="username"
        />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入密码（6~20 位）"
          size="large"
          show-password
          autocomplete="current-password"
          @keyup.enter="submit"
        />
      </el-form-item>
      <div class="form-extra">
        <router-link to="/forgot-password" class="link">忘记密码？</router-link>
      </div>
      <el-button
        type="primary"
        size="large"
        class="submit-button"
        :loading="loading"
        @click="submit"
      >
        登录
      </el-button>
    </el-form>

    <div class="auth-footer">
      还没有账号？
      <router-link to="/register" class="link">立即注册</router-link>
    </div>
  </AuthShell>
</template>

<style scoped>
.form-alert {
  margin-bottom: 18px;
}

.form-extra {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 18px;
}

.submit-button {
  width: 100%;
}

.auth-footer {
  margin-top: 22px;
  color: var(--muted);
  font-size: 14px;
  text-align: center;
}

.link {
  color: var(--brand);
  font-weight: 600;
}
</style>
