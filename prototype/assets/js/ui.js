/* ============================================================
   云枢智学 · 通用交互层（存储 / 导航 / 弹窗 / Toast）
   ============================================================ */
(function (global) {
  'use strict';

  /* ---------------- 存储 ---------------- */
  const Store = {
    get(key, fallback) {
      try {
        const raw = localStorage.getItem('ys_' + key);
        return raw === null ? fallback : JSON.parse(raw);
      } catch (e) { return fallback; }
    },
    set(key, val) {
      try { localStorage.setItem('ys_' + key, JSON.stringify(val)); } catch (e) {}
    },
    remove(key) { try { localStorage.removeItem('ys_' + key); } catch (e) {} },
    clearAll() {
      Object.keys(localStorage).filter(k => k.indexOf('ys_') === 0).forEach(k => localStorage.removeItem(k));
    }
  };

  // 首次访问自动填充演示账号，方便直接体验
  if (!Store.get('user', null) && !Store.get('logged_out', false)) {
    Store.set('user', {
      name: '林晓', phone: '13912345678', email: 'linxiao@example.com', gender: 'female',
      role: 'student', isAdmin: false, avatar: '',
      joined: '2026-09-12', days: 42, questions: 386, interviews: 12, accuracy: 86,
      streak: 7
    });
  }
  if (!Store.get('admin', null) && !Store.get('admin_logged_out', false)) {
    Store.set('admin', { name: '超级管理员', role: 'super_admin', roleName: '超级管理员', phone: '13800000000' });
  }

  const UI = {};
  UI.Store = Store;
  UI.user = () => Store.get('user', null);
  UI.admin = () => Store.get('admin', null);

  /* ---------------- 基础工具 ---------------- */
  UI.esc = s => String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  UI.debounce = (fn, wait) => {
    let t; return function () { clearTimeout(t); const a = arguments, c = this; t = setTimeout(() => fn.apply(c, a), wait || 300); };
  };
  UI.fmtDuration = min => {
    if (min < 60) return min + ' 分钟';
    const h = Math.floor(min / 60), m = min % 60;
    return h + ' 小时' + (m ? ' ' + m + ' 分' : '');
  };
  UI.fmtSec = sec => {
    const h = Math.floor(sec / 3600), m = Math.floor((sec % 3600) / 60), s = sec % 60;
    return [h, m, s].map(v => String(v).padStart(2, '0')).join(':');
  };
  UI.initial = name => {
    const n = String(name || '?');
    return n.length > 4 ? n.slice(0, 2) : n.slice(-2);
  };
  UI.scoreColor = n => n >= 8 ? '#12b76a' : n >= 6 ? '#f79009' : '#f04438';
  UI.scoreSoft = n => n >= 8 ? '#ecfdf3' : n >= 6 ? '#fffaeb' : '#fef3f2';
  UI.typeName = t => ({ single: '单选', multiple: '多选', judge: '判断', short: '简答' }[t] || t);
  UI.diffName = d => ({ easy: '简单', medium: '中等', hard: '困难' }[d] || d);
  UI.letterList = arr => (Array.isArray(arr) ? arr.join('') : (arr || ''));

  UI.debounceInput = (el, fn, wait) => el.addEventListener('input', UI.debounce(fn, wait || 300));

  /* ---------------- Toast ---------------- */
  UI.toast = function (msg, type) {
    type = type || 'success';
    let stack = document.querySelector('.toast-stack');
    if (!stack) { stack = document.createElement('div'); stack.className = 'toast-stack'; document.body.appendChild(stack); }
    const icons = { success: '✓', error: '✕', warn: '!', info: 'i' };
    const el = document.createElement('div');
    el.className = 'toast ' + type;
    el.innerHTML = `<span class="ti">${icons[type] || '✓'}</span><span>${UI.esc(msg)}</span>`;
    stack.appendChild(el);
    setTimeout(() => { el.classList.add('out'); setTimeout(() => el.remove(), 220); }, 2600);
  };

  /* ---------------- Modal ---------------- */
  UI.open = function (id) {
    const m = typeof id === 'string' ? document.getElementById(id) : id;
    if (!m) return;
    m.classList.add('open');
    document.body.style.overflow = 'hidden';
  };
  UI.close = function (id) {
    const m = typeof id === 'string' ? document.getElementById(id) : id;
    if (!m) return;
    m.classList.remove('open');
    if (!document.querySelector('.modal-mask.open')) document.body.style.overflow = '';
  };
  UI.bindModals = function () {
    document.addEventListener('click', e => {
      const mask = e.target.closest('.modal-mask');
      if (mask && e.target === mask && !mask.dataset.static) UI.close(mask);
      const closer = e.target.closest('[data-close]');
      if (closer) UI.close(closer.closest('.modal-mask'));
    });
    document.addEventListener('keydown', e => {
      if (e.key === 'Escape') {
        const open = document.querySelectorAll('.modal-mask.open');
        if (open.length) UI.close(open[open.length - 1]);
      }
    });
  };

  UI.confirm = function (opts) {
    opts = Object.assign({
      title: '确认操作', message: '', confirmText: '确认', cancelText: '取消',
      danger: false, icon: '', onConfirm: null
    }, opts || {});
    return new Promise(resolve => {
      const mask = document.createElement('div');
      mask.className = 'modal-mask';
      mask.innerHTML = `
        <div class="modal modal-sm" role="dialog">
          <div class="modal-body" style="padding:26px 24px 20px">
            <div class="row gap-16" style="align-items:flex-start">
              <div class="modal-icon ${opts.danger ? 'danger' : 'warn'}">${opts.icon || (opts.danger ? '🗑' : '?')}</div>
              <div style="flex:1;min-width:0">
                <h3 style="font-size:16.5px;font-weight:750">${UI.esc(opts.title)}</h3>
                <div style="color:var(--muted);font-size:13px;margin-top:6px;line-height:1.7">${opts.message || ''}</div>
              </div>
            </div>
          </div>
          <div class="modal-foot">
            <button class="btn btn-outline" data-act="cancel">${UI.esc(opts.cancelText)}</button>
            <button class="btn ${opts.danger ? 'btn-danger' : 'btn-primary'}" data-act="ok">${UI.esc(opts.confirmText)}</button>
          </div>
        </div>`;
      document.body.appendChild(mask);
      requestAnimationFrame(() => mask.classList.add('open'));
      const done = v => {
        mask.classList.remove('open');
        setTimeout(() => mask.remove(), 200);
        if (!document.querySelector('.modal-mask.open')) document.body.style.overflow = '';
        resolve(v);
      };
      mask.querySelector('[data-act="cancel"]').onclick = () => done(false);
      mask.querySelector('[data-act="ok"]').onclick = () => { if (opts.onConfirm) opts.onConfirm(); done(true); };
      mask.onclick = e => { if (e.target === mask) done(false); };
      mask.querySelector('[data-act="ok"]').focus();
    });
  };

  /* ---------------- 头部 / 页脚 ---------------- */
  const NAV = [
    { key: 'home', label: '首页', href: 'index.html' },
    { key: 'roadmap', label: '学习路线', href: 'roadmap.html' },
    { key: 'questions', label: '题库', href: 'questions.html' },
    { key: 'interview', label: 'AI面试', href: 'interview-config.html' }
  ];

  UI.renderHeader = function (active) {
    const mount = document.getElementById('siteHeader');
    if (!mount) return;
    const u = UI.user();
    const navHtml = NAV.map(n => `<a href="${n.href}" class="${active === n.key ? 'active' : ''}">${n.label}</a>`).join('');
    const actions = u ? `
      <div class="dropdown" id="userDropdown">
        <button class="user-trigger" id="userTrigger">
          <span class="avatar avatar-sm">${UI.esc(UI.initial(u.name))}</span>
          <span class="uname">${UI.esc(u.name)}</span>
          <span class="caret">▾</span>
        </button>
        <div class="dropdown-menu">
          <div class="dd-head">
            <div class="n">${UI.esc(u.name)}</div>
            <div class="e">${UI.esc(u.phone ? u.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '')}</div>
          </div>
          <a href="profile.html"><span>👤</span>个人中心</a>
          <a href="mistakes.html"><span>📕</span>我的错题</a>
          <a href="interview-records.html"><span>🗂</span>面试记录</a>
          ${u.isAdmin ? '<a href="../admin/dashboard.html"><span>⚙️</span>管理后台</a>' : ''}
          <div class="dd-sep"></div>
          <button class="danger" id="navLogout"><span>↪</span>退出登录</button>
        </div>
      </div>` : `
      <a class="btn btn-ghost btn-sm" href="login.html">登录</a>
      <a class="btn btn-primary btn-sm" href="register.html">免费注册</a>`;

    mount.outerHTML = `
      <header class="site-header">
        <div class="container-wide inner">
          <a class="logo" href="index.html">
            <span class="logo-mark">云</span><span>云枢智学</span>
          </a>
          <nav class="main-nav">${navHtml}</nav>
          <div class="header-actions">${actions}</div>
        </div>
      </header>`;

    const trigger = document.getElementById('userTrigger');
    const dd = document.getElementById('userDropdown');
    if (trigger) {
      trigger.addEventListener('click', e => { e.stopPropagation(); dd.classList.toggle('open'); });
      document.addEventListener('click', () => dd.classList.remove('open'));
      const lo = document.getElementById('navLogout');
      lo && lo.addEventListener('click', async e => {
        e.stopPropagation();
        const ok = await UI.confirm({ title: '退出登录', message: '退出后需要重新登录才能继续练习与面试。', confirmText: '退出登录', danger: true, icon: '↪' });
        if (ok) {
          Store.set('logged_out', true);
          Store.remove('user');
          UI.toast('已退出登录', 'success');
          setTimeout(() => location.href = 'index.html', 400);
        }
      });
    }
  };

  UI.renderFooter = function () {
    const mount = document.getElementById('siteFooter');
    if (!mount) return;
    mount.outerHTML = `
      <footer class="site-footer">
        <div class="container-wide">
          <div class="foot-main">
            <div>
              <a class="logo" href="index.html"><span class="logo-mark">云</span><span>云枢智学</span></a>
              <p class="foot-desc">AI 赋能的智能面试训练平台。按路线学、按阶段练、用 AI 面试检验成果，把学习变成可验证的能力。</p>
            </div>
            <div>
              <h5>产品</h5>
              <div class="foot-links">
                <a href="roadmap.html">学习路线</a>
                <a href="questions.html">题库</a>
                <a href="interview-config.html">AI 面试</a>
              </div>
            </div>
            <div>
              <h5>支持</h5>
              <div class="foot-links">
                <a href="javascript:void(0)" data-placeholder="帮助中心">帮助中心</a>
                <a href="javascript:void(0)" data-placeholder="意见反馈">意见反馈</a>
                <a href="javascript:void(0)" data-placeholder="联系我们">联系我们</a>
              </div>
            </div>
            <div>
              <h5>关于</h5>
              <div class="foot-links">
                <a href="javascript:void(0)" data-placeholder="关于我们">关于我们</a>
                <a href="javascript:void(0)" data-placeholder="用户协议">用户协议</a>
                <a href="javascript:void(0)" data-placeholder="隐私政策">隐私政策</a>
              </div>
            </div>
          </div>
          <div class="foot-bottom">
            <span>© 2026 云枢智学 · 仅用于产品原型演示，页面数据均为模拟数据</span>
            <a class="admin-entry" href="../admin/login.html">管理后台</a>
          </div>
        </div>
      </footer>`;
    document.querySelectorAll('[data-placeholder]').forEach(a => {
      a.addEventListener('click', e => { e.preventDefault(); UI.toast(`「${a.dataset.placeholder}」本期为占位入口`, 'info'); });
    });
  };

  /* ---------------- 数字动画 ---------------- */
  UI.animateCount = function (el, target, duration) {
    duration = duration || 1200;
    const start = performance.now();
    const from = 0;
    function tick(now) {
      const p = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - p, 3);
      const val = Math.round(from + (target - from) * eased);
      el.textContent = val.toLocaleString('en-US');
      if (p < 1) requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
  };

  UI.initReveal = function () {
    const els = document.querySelectorAll('.reveal');
    if (!('IntersectionObserver' in window)) { els.forEach(e => e.classList.add('in')); return; }
    const io = new IntersectionObserver((entries) => {
      entries.forEach(en => { if (en.isIntersecting) { en.target.classList.add('in'); io.unobserve(en.target); } });
    }, { threshold: .08 });
    els.forEach(e => io.observe(e));
  };

  /* ---------------- 权限 ---------------- */
  UI.requireUser = function () {
    const u = UI.user();
    if (!u) {
      const redir = encodeURIComponent(location.pathname.split('/').pop() + location.search);
      location.replace('login.html?redirect=' + redir);
      return null;
    }
    return u;
  };

  UI.requireAdmin = function () {
    const a = UI.admin();
    if (!a) {
      const redir = encodeURIComponent(location.pathname.split('/').pop() + location.search);
      location.replace('login.html?redirect=' + redir);
      return null;
    }
    return a;
  };

  /* ---------------- 通用小部件 ---------------- */
  UI.ringSvg = function (score, size, stroke, opts) {
    opts = opts || {};
    size = size || 160; stroke = stroke || 12;
    const r = (size - stroke) / 2;
    const c = 2 * Math.PI * r;
    const pct = Math.max(0, Math.min(100, score)) / 100;
    const color = opts.color || UI.scoreColor(score / 10);
    return `<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" style="transform:rotate(-90deg)">
      <circle cx="${size / 2}" cy="${size / 2}" r="${r}" fill="none" stroke="#eef0f6" stroke-width="${stroke}"/>
      <circle cx="${size / 2}" cy="${size / 2}" r="${r}" fill="none" stroke="${color}" stroke-width="${stroke}"
        stroke-linecap="round" stroke-dasharray="${c}" stroke-dashoffset="${c * (1 - pct)}"
        style="transition:stroke-dashoffset 1.1s cubic-bezier(.2,.8,.3,1)"/>
    </svg>`;
  };

  UI.badge = function (text, type) { return `<span class="badge badge-${type || 'gray'}">${text}</span>`; };

  /* ---------------- 报告生成 ---------------- */
  UI.buildReport = function (opt) {
    const scores = opt.scores || [];
    const avg = scores.length ? scores.reduce((a, b) => a + b, 0) / scores.length : 6.5;
    const score = Math.round(Math.max(38, Math.min(96, avg * 10 + (Math.random() * 6 - 3))));
    const dims = [
      { name: '专业知识', score: Math.round(Math.min(98, Math.max(35, score + (Math.random() * 14 - 7)))) },
      { name: '逻辑表达', score: Math.round(Math.min(98, Math.max(35, score + (Math.random() * 16 - 8)))) },
      { name: '项目经验', score: Math.round(Math.min(98, Math.max(35, score - 4 + (Math.random() * 14 - 7)))) },
      { name: '问题分析', score: Math.round(Math.min(98, Math.max(35, score + (Math.random() * 12 - 6)))) },
      { name: '技术深度', score: Math.round(Math.min(98, Math.max(35, score - 6 + (Math.random() * 14 - 7)))) },
      { name: '沟通表达', score: Math.round(Math.min(98, Math.max(35, score + (Math.random() * 12 - 6)))) }
    ].map(d => Object.assign(d, { score: Math.round(d.score) }));
    return Object.assign({
      id: 'rpt' + Date.now(),
      title: (opt.subjectName || 'AI 模拟面试') + ' · 面试报告',
      subjectId: opt.subjectId, subjectName: opt.subjectName,
      difficulty: opt.difficulty, count: opt.count || scores.length,
      durationSec: opt.durationSec || 0, score: score,
      accuracy: Math.round(score * 0.92),
      date: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
      dims: dims,
      pros: [
        '对核心概念的理解较为准确，能够结合具体场景说明技术选型理由。',
        '回答结构清晰，先给结论再展开细节，便于面试官快速抓住重点。',
        score >= 75 ? '能够主动补充项目中的量化结果，体现出较强的工程落地意识。' : '表达意愿积极，面对追问能够持续补充信息。'
      ],
      improvements: [
        { text: '部分回答停留在概念复述，缺少「为什么这样做」的权衡分析。', tip: '建议使用 STAR 结构回答，并补充至少一个取舍点与验证结果。' },
        { text: '对边界情况和失败路径的考虑不足，方案的完整性有待提升。', tip: '回答设计类问题时，习惯性补充异常处理、监控与回滚方案。' },
        { text: '技术细节的深度不足，关键参数与原理描述较模糊。', tip: '针对高频考点整理 3 个可量化的实践案例，做到能讲清楚参数选择依据。' }
      ],
      suggestions: [
        '针对本次方向的高频考点做一次系统复盘，重点补齐薄弱环节。',
        '每道题练习时先列关键词提纲，再组织语言，控制在 90 秒内完成表达。',
        '补充 1-2 个端到端项目案例，明确背景、方案、指标与复盘。',
        '一周内再进行一次同方向面试，验证提升效果。'
      ]
    }, opt.extra || {});
  };

  global.UI = UI;
})(window);
