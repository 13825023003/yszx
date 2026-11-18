<script setup lang="ts">
import AuthShell from '@/components/AuthShell.vue'
</script>

<template>
  <AuthShell title="注册账号" subtitle="注册页骨架已就绪，验证码发送与注册接口将在认证模块实现后接入。">
    <el-form label-position="top" disabled>
      <el-form-item label="手机号">
        <el-input placeholder="请输入手机号" size="large" />
      </el-form-item>
      <el-form-item label="短信验证码">
        <el-input placeholder="请输入 6 位验证码" size="large" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input placeholder="6~20 位密码" type="password" size="large" show-password />
      </el-form-item>
      <el-button type="primary" size="large" style="width: 100%">注册</el-button>
    </el-form>
    <div class="auth-footer">
      已有账号？
      <router-link to="/login" class="link">返回登录</router-link>
    </div>
  </AuthShell>
</template>

<style scoped>
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
