/* ============================================================
   云枢智学 · 管理端通用壳层
   ============================================================ */
(function (global) {
  'use strict';

  const MENU = [
    {
      group: '概览', items: [
        { key: 'dashboard', label: '数据看板', href: 'dashboard.html', icon: '▦' }
      ]
    },
    {
      group: '内容管理', items: [
        { key: 'subjects', label: '学科管理', href: 'subjects.html', icon: '📚' },
        { key: 'courses', label: 'B站课程库', href: 'courses.html', icon: '🎬' },
        { key: 'question-bank', label: '题库管理', href: 'question-bank.html', icon: '📝', badge: 6 },
        { key: 'interview-questions', label: '面试题库管理', href: 'interview-questions.html', icon: '🧠' },
        { key: 'roadmaps', label: '学习路线', href: 'roadmaps.html', icon: '🗺' }
      ]
    },
    {
      group: '学员管理', items: [
        { key: 'students', label: '学员管理', href: 'students.html', icon: '👥' }
      ]
    },
    {
      group: '系统管理', superOnly: true, items: [
        { key: 'users', label: '用户管理', href: 'users.html', icon: '👤' },
        { key: 'roles', label: '角色管理', href: 'roles.html', icon: '🛡' },
        { key: 'logs', label: '操作日志', href: 'logs.html', icon: '📜' },
        { key: 'settings', label: '系统设置', href: 'settings.html', icon: '⚙' }
      ]
    }
  ];

  const ROLE_NAME = { super_admin: '超级管理员', content_reviewer: '内容审核', operator: '运营人员' };

  const Admin = {};
  Admin.ROLE_NAME = ROLE_NAME;
  Admin.menu = MENU;

  Admin.current = () => UI.admin();

  Admin.require = function () {
    const a = UI.admin();
    if (!a) {
      const redir = encodeURIComponent(location.pathname.split('/').pop() + location.search);
      location.replace('login.html?redirect=' + redir);
      return null;
    }
    return a;
  };

  Admin.can = function (action) {
    const a = UI.admin();
    if (!a) return false;
    if (a.role === 'super_admin') return true;
    if (a.role === 'content_reviewer') {
      return ['view', 'audit', 'viewStudents'].indexOf(action) > -1;
    }
    if (a.role === 'operator') {
      return ['view', 'create', 'edit', 'delete', 'import', 'export', 'viewStudents', 'editStudents', 'deleteStudents'].indexOf(action) > -1;
    }
    return false;
  };

  Admin.isSuper = () => (Admin.current() || {}).role === 'super_admin';

  Admin.applyPermissions = function (root) {
    (root || document).querySelectorAll('[data-perm]').forEach(el => {
      if (!Admin.can(el.dataset.perm)) el.remove();
    });
    (root || document).querySelectorAll('[data-super-only]').forEach(el => {
      if (!Admin.isSuper()) el.remove();
    });
  };

  Admin.log = function (action, target, detail) {
    const a = Admin.current();
    const now = new Date();
    const pad = n => String(n).padStart(2, '0');
    const time = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
    const id = 'LOG-' + time.replace(/[-: ]/g, '').slice(0, 8) + '-' + Math.floor(1000 + Math.random() * 9000);
    const list = UI.Store.get('logs_extra', []);
    list.unshift({ id, user: a ? a.name : '未知', action, target, ip: '192.168.12.34', time, detail });
    UI.Store.set('logs_extra', list.slice(0, 60));
  };

  Admin.renderShell = function (activeKey) {
    const admin = Admin.require();
    if (!admin) return null;

    let activeItem = null, activeGroup = '';
    MENU.forEach(g => g.items.forEach(it => { if (it.key === activeKey) { activeItem = it; activeGroup = g.group; } }));
    if (!activeItem) { activeItem = MENU[0].items[0]; activeGroup = MENU[0].group; }

    const collapsed = UI.Store.get('admin_collapsed', false);

    const navHtml = MENU.filter(g => !g.superOnly || admin.role === 'super_admin').map(g => `
      <div class="nav-group-title">${g.group}</div>
      ${g.items.map(it => `
        <a class="nav-item ${it.key === activeKey ? 'active' : ''}" href="${it.href}">
          <span class="ni-ico">${it.icon}</span><span class="txt">${it.label}</span>
          ${it.badge ? `<span class="badge-n">${it.badge}</span>` : ''}
        </a>`).join('')}
    `).join('');

    const asideMount = document.getElementById('adminAsideMount');
    if (asideMount) {
      asideMount.outerHTML = `
        <aside class="admin-aside ${collapsed ? 'collapsed' : ''}" id="adminAside">
          <a class="brand" href="dashboard.html">
            <span class="logo-mark">云</span><span class="brand-text">云枢智学 · 管理端</span>
          </a>
          <nav class="admin-nav">${navHtml}</nav>
          <div class="aside-foot">
            <div class="aside-user">
              <span class="avatar">${UI.esc(UI.initial(admin.name))}</span>
              <div class="au-info">
                <div class="au-n">${UI.esc(admin.name)}</div>
                <div class="au-r">${UI.esc(ROLE_NAME[admin.role] || '管理员')}</div>
              </div>
            </div>
          </div>
        </aside>`;
    }

    const topMount = document.getElementById('adminTopbarMount');
    if (topMount) {
      topMount.outerHTML = `
        <header class="admin-topbar">
          <button class="collapse-toggle" id="collapseToggle" title="折叠菜单">☰</button>
          <div class="crumb"><span>${activeGroup} / </span>${activeItem.label}</div>
          <div class="top-actions">
            <div class="dropdown" id="bellDropdown">
              <button class="btn-icon bell" id="bellBtn" title="通知">
                🔔<span class="dot-badge" id="bellBadge">3</span>
              </button>
              <div class="dropdown-menu">
                <div class="dd-head"><div class="n">通知中心</div><div class="e">3 条未读消息</div></div>
                <div class="notif-item"><span class="ni-dot" style="background:#f79009"></span><div><b>6 道题目待审核</b><div class="e">来自 AI应用与智能体开发 等学科</div></div></div>
                <div class="notif-item"><span class="ni-dot" style="background:#2e90fa"></span><div><b>B站课程库新增 2 门课程</b><div class="e">运营人员 林运营 于 17:55 添加</div></div></div>
                <div class="notif-item"><span class="ni-dot" style="background:#12b76a"></span><div><b>本周学员增长 12.4%</b><div class="e">可在数据看板查看详情</div></div></div>
                <div class="dd-sep"></div>
                <button id="readAll" style="justify-content:center;color:var(--brand)">全部标记为已读</button>
              </div>
            </div>
            <div class="dropdown" id="adminDropdown">
              <button class="user-trigger" id="adminTrigger" style="padding:4px 8px 4px 4px">
                <span class="avatar avatar-sm">${UI.esc(UI.initial(admin.name))}</span>
                <span class="uname">${UI.esc(admin.name)}</span>
                <span class="caret">▾</span>
              </button>
              <div class="dropdown-menu">
                <div class="dd-head">
                  <div class="n">${UI.esc(admin.name)}</div>
                  <div class="e">${UI.esc(ROLE_NAME[admin.role] || '管理员')} · ${UI.esc(admin.phone || '')}</div>
                </div>
                <a href="../portal/index.html">↩ 返回学员前台</a>
                <div class="dd-sep"></div>
                <button class="danger" id="adminLogout">↪ 退出登录</button>
              </div>
            </div>
          </div>
        </header>`;
    }

    // 折叠
    const aside = document.getElementById('adminAside');
    const main = document.querySelector('.admin-main');
    const toggle = document.getElementById('collapseToggle');
    if (collapsed && main) main.classList.add('collapsed');
    toggle && toggle.addEventListener('click', () => {
      const isCollapsed = aside.classList.toggle('collapsed');
      main.classList.toggle('collapsed', isCollapsed);
      UI.Store.set('admin_collapsed', isCollapsed);
    });

    // 下拉
    const bell = document.getElementById('bellDropdown');
    const bellBtn = document.getElementById('bellBtn');
    bellBtn && bellBtn.addEventListener('click', e => { e.stopPropagation(); bell.classList.toggle('open'); });
    document.getElementById('readAll')?.addEventListener('click', () => {
      const badge = document.getElementById('bellBadge');
      if (badge) badge.style.display = 'none';
      UI.toast('已全部标记为已读', 'success');
      bell.classList.remove('open');
    });

    const admDrop = document.getElementById('adminDropdown');
    const admTrigger = document.getElementById('adminTrigger');
    admTrigger && admTrigger.addEventListener('click', e => { e.stopPropagation(); admDrop.classList.toggle('open'); });
    document.addEventListener('click', () => {
      bell && bell.classList.remove('open');
      admDrop && admDrop.classList.remove('open');
    });

    document.getElementById('adminLogout')?.addEventListener('click', async () => {
      const ok = await UI.confirm({
        title: '退出登录', message: '退出后需要重新登录管理后台才能继续操作。',
        confirmText: '退出登录', danger: true, icon: '↪'
      });
      if (ok) {
        UI.Store.set('admin_logged_out', true);
        UI.Store.remove('admin');
        UI.toast('已退出管理后台', 'success');
        setTimeout(() => location.href = 'login.html', 400);
      }
    });

    // 通知项点击
    document.querySelectorAll('.notif-item').forEach(el => el.addEventListener('click', () => {
      el.querySelector('.ni-dot').style.opacity = .25;
      const badge = document.getElementById('bellBadge');
      if (badge) { const n = +badge.textContent; if (n > 1) badge.textContent = n - 1; else badge.style.display = 'none'; }
      UI.toast('已标记该通知为已读', 'info');
    }));

    // 页面标题
    document.title = activeItem.label + ' · 云枢智学管理端';
    return admin;
  };

  /* ---------------- 管理端本地数据（演示用，写入 localStorage） ---------------- */
  Admin.store = UI.Store;
  Admin.getSubjects = () => UI.Store.get('admin_subjects', DB.SUBJECTS);
  Admin.saveSubjects = list => UI.Store.set('admin_subjects', list);
  Admin.getCourses = () => UI.Store.get('admin_courses', DB.COURSES);
  Admin.saveCourses = list => UI.Store.set('admin_courses', list);
  Admin.getRoutes = () => UI.Store.get('admin_routes', DB.ROUTES);
  Admin.saveRoutes = list => UI.Store.set('admin_routes', list);
  Admin.getQuestions = () => UI.Store.get('admin_questions', DB.ADMIN_QUESTIONS);
  Admin.saveQuestions = list => UI.Store.set('admin_questions', list);
  Admin.getStudents = () => UI.Store.get('admin_students', DB.STUDENTS);
  Admin.saveStudents = list => UI.Store.set('admin_students', list);
  Admin.getUsers = () => UI.Store.get('admin_users', DB.ADMIN_USERS);
  Admin.saveUsers = list => UI.Store.set('admin_users', list);
  Admin.getRoles = () => UI.Store.get('admin_roles', DB.ROLES);
  Admin.saveRoles = list => UI.Store.set('admin_roles', list);
  Admin.getLogs = () => {
    const extra = UI.Store.get('logs_extra', []);
    return UI.Store.get('logs_cleared', false) ? extra : extra.concat(DB.LOGS);
  };

  Admin.nextId = function (prefix) {
    return prefix + Date.now().toString(36).slice(-5) + Math.floor(Math.random() * 90 + 10);
  };

  Admin.copy = obj => JSON.parse(JSON.stringify(obj));

  /* ---------------- 小工具 ---------------- */
  Admin.formatNum = n => Number(n || 0).toLocaleString('en-US');

  Admin.statusBadge = function (status) {
    const map = {
      published: ['已上架', 'success'], offline: ['已下架', 'gray'], pending: ['待审核', 'warn'],
      draft: ['草稿', 'outline'], rejected: ['已驳回', 'danger'],
      enabled: ['已启用', 'success'], disabled: ['已禁用', 'danger']
    };
    const v = map[status] || [status, 'gray'];
    return `<span class="badge badge-${v[1]}">${v[0]}</span>`;
  };

  Admin.typeBadge = t => `<span class="badge badge-${t === 'single' ? 'brand' : t === 'multiple' ? 'info' : t === 'judge' ? 'warn' : 'success'}">${UI.typeName(t)}</span>`;

  Admin.diffHtml = d => `<span class="diff diff-${d}">● ${UI.diffName(d)}</span>`;

  Admin.daysAgo = function (dateStr) {
    const d = new Date(String(dateStr).replace(/-/g, '/'));
    return Math.max(0, Math.floor((new Date('2026-11-05T12:00:00') - d) / 86400000));
  };

  Admin.coverStyle = c => `background:${c.cover || 'linear-gradient(135deg,#6366f1,#a855f7)'}`;

  global.Admin = Admin;
})(window);
