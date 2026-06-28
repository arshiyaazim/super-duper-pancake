/* Al-Aqsa Admin Panel — shared utilities */

const FAZLE_API = () => localStorage.getItem('fazle_api_url') || 'http://127.0.0.1:8200';
const FAZLE_KEY = () => localStorage.getItem('fazle_api_key') || 'fk_MpRgBQCHFk43X1os4cgXrSjFnCqHVEyvlfciuUM7LPI';
const LW_API    = () => localStorage.getItem('lw_api_url')    || 'http://127.0.0.1:8310';
const LW_TOKEN  = () => localStorage.getItem('lw_token')      || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiJhZG1pbiIsInJvbGUiOiJhZG1pbiIsInVzZXJuYW1lIjoiYWRtaW4iLCJpYXQiOjE3ODIyNTA5MDksImV4cCI6MTgxMzc4NjkwOX0.404ISyhgRKyZpkQLKMBDEUoyVYxQe-BfmZqckMIXtt0';
const LW_SECRET = () => localStorage.getItem('lw_gateway_secret') || '4bab47819a45359c24c7a486aaf36797f51d24ba40c3bb2d21964e96ae34bf2a';

/* ── API helpers ─────────────────────────────────────────────── */

async function fazleGet(path) {
  const r = await fetch(FAZLE_API() + path, {
    headers: { 'X-Internal-Key': FAZLE_KEY(), 'Accept': 'application/json' }
  });
  if (!r.ok) throw new Error(`${r.status} ${r.statusText}`);
  return r.json();
}

async function fazlePost(path, body) {
  const r = await fetch(FAZLE_API() + path, {
    method: 'POST',
    headers: {
      'X-Internal-Key': FAZLE_KEY(),
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  });
  if (!r.ok) {
    const err = await r.json().catch(() => ({}));
    throw new Error(err.detail || `${r.status} ${r.statusText}`);
  }
  return r.json();
}

async function fazlePatch(path, body) {
  const r = await fetch(FAZLE_API() + path, {
    method: 'PATCH',
    headers: {
      'X-Internal-Key': FAZLE_KEY(),
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  });
  if (!r.ok) throw new Error(`${r.status} ${r.statusText}`);
  return r.json();
}

async function lwGet(path) {
  const r = await fetch(LW_API() + path, {
    headers: {
      'Authorization': `Bearer ${LW_TOKEN()}`,
      'Accept': 'application/json'
    }
  });
  if (!r.ok) throw new Error(`${r.status} ${r.statusText}`);
  return r.json();
}

async function lwGatewayPost(path, body) {
  const r = await fetch(LW_API() + path, {
    method: 'POST',
    headers: {
      'X-Gateway-Secret': LW_SECRET(),
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  });
  return r.json();
}

/* ── Toast notifications ─────────────────────────────────────── */

function ensureToastContainer() {
  let c = document.getElementById('toast-container');
  if (!c) {
    c = document.createElement('div');
    c.id = 'toast-container';
    c.className = 'toast-container';
    document.body.appendChild(c);
  }
  return c;
}

function toast(msg, type = '') {
  const c = ensureToastContainer();
  const t = document.createElement('div');
  t.className = `toast${type ? ' ' + type : ''}`;
  t.textContent = msg;
  c.appendChild(t);
  setTimeout(() => t.remove(), 3500);
}

/* ── Relative time ───────────────────────────────────────────── */

function relTime(ts) {
  if (!ts) return '—';
  const d = new Date(typeof ts === 'number' ? ts * 1000 : ts);
  const secs = Math.round((Date.now() - d) / 1000);
  if (secs < 5)   return 'just now';
  if (secs < 60)  return `${secs}s ago`;
  if (secs < 3600) return `${Math.round(secs/60)}m ago`;
  if (secs < 86400) return `${Math.round(secs/3600)}h ago`;
  return `${Math.round(secs/86400)}d ago`;
}

function fmtDate(ts) {
  if (!ts) return '—';
  const d = new Date(typeof ts === 'number' ? ts * 1000 : ts);
  return d.toLocaleString('en-GB', { day:'2-digit', month:'short', hour:'2-digit', minute:'2-digit' });
}

/* ── Status badge helpers ────────────────────────────────────── */

function statusBadge(status) {
  const map = {
    ok: 'badge-green', healthy: 'badge-green', active: 'badge-green',
    sent: 'badge-green', approved: 'badge-green', created: 'badge-green',
    pending: 'badge-gold', sending: 'badge-gold', draft: 'badge-gold',
    failed: 'badge-red', error: 'badge-red', dlq: 'badge-red', stale: 'badge-red',
    ignored: 'badge-gray', inactive: 'badge-gray', closed: 'badge-gray',
    warn: 'badge-orange', duplicate: 'badge-orange',
  };
  const cls = map[String(status).toLowerCase()] || 'badge-gray';
  return `<span class="badge ${cls}">${status}</span>`;
}

/* ── Modal helpers ───────────────────────────────────────────── */

function openModal(id) {
  const el = document.getElementById(id);
  if (el) el.style.display = 'flex';
}

function closeModal(id) {
  const el = document.getElementById(id);
  if (el) el.style.display = 'none';
}

/* ── Auth check on load ──────────────────────────────────────── */

function requireAuth() {
  if (!FAZLE_KEY()) {
    const key = prompt('Enter Fazle Core API key (stored locally):');
    if (key) localStorage.setItem('fazle_api_key', key.trim());
    else return false;
  }
  return true;
}

/* ── Mobile sidebar toggle ───────────────────────────────────── */

function toggleSidebar() {
  const sidebar = document.querySelector('.sidebar');
  const overlay = document.getElementById('sidebar-overlay');
  if (!sidebar) return;
  const isOpen = sidebar.classList.toggle('open');
  if (overlay) overlay.classList.toggle('open', isOpen);
  document.body.style.overflow = isOpen ? 'hidden' : '';
}

function closeSidebar() {
  const sidebar = document.querySelector('.sidebar');
  const overlay = document.getElementById('sidebar-overlay');
  if (sidebar) sidebar.classList.remove('open');
  if (overlay) overlay.classList.remove('open');
  document.body.style.overflow = '';
}

function injectMobileShell() {
  // Overlay backdrop
  if (!document.getElementById('sidebar-overlay')) {
    const overlay = document.createElement('div');
    overlay.id = 'sidebar-overlay';
    overlay.className = 'sidebar-overlay';
    overlay.onclick = closeSidebar;
    document.body.prepend(overlay);
  }

  // Close button inside sidebar
  const sidebar = document.querySelector('.sidebar');
  if (sidebar && !sidebar.querySelector('.sidebar-close')) {
    const btn = document.createElement('button');
    btn.className = 'sidebar-close';
    btn.innerHTML = '×';
    btn.onclick = closeSidebar;
    sidebar.style.position = 'fixed';
    sidebar.appendChild(btn);
  }

  // Hamburger in topbar (prepend before page-title)
  const topbar = document.querySelector('.topbar');
  if (topbar && !topbar.querySelector('.hamburger')) {
    const btn = document.createElement('button');
    btn.className = 'hamburger';
    btn.setAttribute('aria-label', 'Menu');
    btn.innerHTML = '☰';
    btn.onclick = toggleSidebar;
    topbar.insertBefore(btn, topbar.firstChild);
  }

  // Close sidebar when a nav link is clicked on mobile
  document.querySelectorAll('.nav-link').forEach(link => {
    link.addEventListener('click', () => {
      if (window.innerWidth <= 768) closeSidebar();
    });
  });
}

/* ── Sidebar navigation markup ───────────────────────────────── */

const NAV_ITEMS = [
  { group: 'Overview', items: [
    { href: 'admin.html',       icon: '⊞', label: 'Dashboard' },
  ]},
  { group: 'Operations', items: [
    { href: 'wa-chat.html',     icon: '💬', label: 'WhatsApp Chat' },
    { href: 'escorts.html',     icon: '🛡️', label: 'Escort Programs' },
    { href: 'employees.html',   icon: '👥', label: 'Employees' },
  ]},
  { group: 'Finance', items: [
    { href: 'payroll.html',     icon: '💰', label: 'Payroll / FPE' },
  ]},
  { group: 'Integrations', items: [
    { href: 'location.html',    icon: '📍', label: 'Live Location' },
    { href: 'sms-gateway.html', icon: '📱', label: 'SMS Gateway' },
  ]},
  { group: 'System', items: [
    { href: 'settings.html',    icon: '⚙️', label: 'Settings' },
  ]},
];

function renderNav(currentPage) {
  const sidebar = document.querySelector('.sidebar-nav');
  if (!sidebar) return;
  const currentFile = currentPage || window.location.pathname.split('/').pop();
  sidebar.innerHTML = NAV_ITEMS.map(group => `
    <div class="nav-section">
      <div class="nav-label">${group.group}</div>
      ${group.items.map(item => `
        <a href="${item.href}" class="nav-link${item.href === currentFile ? ' active' : ''}">
          <span class="icon">${item.icon}</span>
          ${item.label}
        </a>
      `).join('')}
    </div>
  `).join('');
  // Mobile shell injected after nav is in the DOM
  injectMobileShell();
}

/* ── Bridge status helper ────────────────────────────────────── */

function bridgeStatusBadge(probe) {
  if (!probe) return statusBadge('unknown');
  if (probe.status === 'ok') {
    if (probe.quiet_but_poller_healthy) return statusBadge('quiet');
    return statusBadge('ok');
  }
  return statusBadge(probe.status || 'error');
}
