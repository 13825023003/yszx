<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const features = [
  {
    title: '题库练习',
    description: '按学科、阶段、题型与难度筛选，顺序或随机练习，客观题即时判分，简答题 AI 批改。',
    path: '/questions',
    action: '进入题库',
  },
  {
    title: '错题复盘',
    description: '答错自动归集，按错误次数分级，连续答对 3 次标记已掌握，支持批量处理。',
    path: '/wrong-book',
    action: '查看错题本',
  },
  {
    title: '学习路线',
    description: '跟随推荐路线逐课学习，勾选完成进度，把零散知识点串成体系。',
    path: '/routes',
    action: '查看路线',
  },
  {
    title: 'AI 模拟面试',
    description: '按技术方向与难度出题，逐题流式评分与建议，结束后生成六维能力报告。',
    path: '/interview',
    action: '开始面试',
  },
]
</script>

<template>
  <div class="page home">
    <section class="hero">
      <div class="hero-copy">
        <el-tag effect="plain" class="hero-tag">AI 赋能 · 智能面试训练</el-tag>
        <h1>
          把每一次练习，<br />
          变成面试场上的<span class="brand-gradient-text">底气</span>
        </h1>
        <p class="hero-desc">
          题库练习、错题复盘、学习路线与 AI 模拟面试，一站式完成技术能力训练与面试准备。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="router.push('/questions')">
            开始刷题
          </el-button>
          <el-button size="large" @click="router.push('/interview')">体验 AI 面试</el-button>
        </div>
      </div>

      <aside class="hero-panel">
        <div class="hero-panel__badge">工程骨架</div>
        <h2>基础能力已就绪</h2>
        <ul>
          <li>Vue 3 + TypeScript + Vite</li>
          <li>Pinia 登录态与路由守卫</li>
          <li>统一请求封装与令牌自动续期</li>
          <li>Element Plus + ECharts</li>
          <li>SSE 流式事件消费工具</li>
        </ul>
      </aside>
    </section>

    <section class="feature-grid">
      <article v-for="item in features" :key="item.title" class="feature-card">
        <h3>{{ item.title }}</h3>
        <p>{{ item.description }}</p>
        <el-button text type="primary" @click="router.push(item.path)">
          {{ item.action }} →
        </el-button>
      </article>
    </section>
  </div>
</template>

<style scoped>
.home {
  padding-top: 40px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 1fr);
  gap: 28px;
  align-items: stretch;
}

.hero-copy {
  padding: 36px 32px;
  border: 1px solid var(--line);
  border-radius: 22px;
  background:
    radial-gradient(circle at 88% 8%, rgba(124, 58, 237, 0.12), transparent 34%),
    radial-gradient(circle at 8% 92%, rgba(79, 70, 229, 0.1), transparent 30%),
    var(--surface);
}

.hero-tag {
  margin-bottom: 16px;
}

.hero h1 {
  margin: 0;
  font-size: clamp(30px, 4vw, 46px);
  line-height: 1.25;
  letter-spacing: -0.5px;
}

.hero-desc {
  max-width: 560px;
  margin: 18px 0 26px;
  color: var(--muted);
  font-size: 16px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero-panel {
  padding: 28px;
  border-radius: 22px;
  background: linear-gradient(150deg, #1f1b4d 0%, #3b2c8f 52%, #6d3fd4 100%);
  color: #fff;
  box-shadow: 0 18px 40px rgba(49, 36, 120, 0.24);
}

.hero-panel__badge {
  display: inline-block;
  padding: 5px 10px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
  letter-spacing: 1px;
}

.hero-panel h2 {
  margin: 18px 0 12px;
  font-size: 20px;
}

.hero-panel ul {
  margin: 0;
  padding-left: 18px;
  color: rgba(255, 255, 255, 0.82);
  line-height: 2;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 18px;
  margin-top: 28px;
}

.feature-card {
  display: flex;
  flex-direction: column;
  padding: 22px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--surface);
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.feature-card:hover {
  transform: translateY(-3px);
  border-color: #cbc9f7;
  box-shadow: 0 14px 30px rgba(16, 24, 40, 0.08);
}

.feature-card h3 {
  margin: 0 0 10px;
  font-size: 17px;
}

.feature-card p {
  flex: 1;
  margin: 0 0 12px;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.75;
}

.feature-card :deep(.el-button) {
  align-self: flex-start;
  padding-left: 0;
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
  }

  .hero-copy {
    padding: 26px 22px;
  }
}
</style>
