<script setup lang="ts">
import AuthShell from '@/components/AuthShell.vue'
</script>

<template>
  <AuthShell title="找回密码" subtitle="找回密码骨架已就绪，验证码校验与重置流程将在认证模块实现后接入。">
    <el-form label-position="top" disabled>
      <el-form-item label="手机号">
        <el-input placeholder="请输入注册手机号" size="large" />
      </el-form-item>
      <el-form-item label="短信验证码">
        <el-input placeholder="请输入 6 位验证码" size="large" />
      </el-form-item>
      <el-button type="primary" size="large" style="width: 100%">下一步</el-button>
    </el-form>
    <div class="auth-footer">
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
