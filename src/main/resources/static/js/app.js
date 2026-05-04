// ===== STATE =====
let token = localStorage.getItem('jwt_token');
let currentUser = JSON.parse(localStorage.getItem('current_user') || 'null');

// Caches populated at load time and reused by filter/sort handlers
// (avoid refetching on every keystroke)
const state = {
    equipmentCache: [],
    usersCache: [],
    requestsCache: [],
    myRequestsCache: [],
    assessmentQueueCache: [],
    assessmentHistoryCache: [],
    myQueuePositions: {},
    equipmentQuickFilters: loadQuickFilters('equipment_quick_filters'),
    myRequestsQuickFilters: loadQuickFilters('my_requests_quick_filters'),
    manageQuickFilters: loadManageQuickFilters(),
    usersQuickFilters: loadQuickFilters('users_quick_filters'),
    assessmentQuickFilters: loadQuickFilters('assessment_quick_filters'),
    usersSortKey: 'id',
    usersSortDir: 'asc',
    activeStrategy: 'weightedScoring',
    charts: { status: null, topUsers: null, utilization: null }
};

const quickFilterConflicts = {
    equipment: {
        'available': ['out-of-stock'],
        'scarce': ['out-of-stock'],
        'out-of-stock': ['available', 'scarce'],
        'high-utilization': []
    },
    myRequests: {
        'pending': ['active', 'overdue', 'awaiting-assessment'],
        'active': ['pending', 'awaiting-assessment'],
        'overdue': ['pending', 'awaiting-assessment'],
        'awaiting-assessment': ['pending', 'active', 'overdue'],
        'exam': [],
        'high-priority': []
    },
    manage: {
        'needs-action': ['ready-pickup', 'overdue', 'awaiting-assessment'],
        'ready-pickup': ['needs-action', 'overdue', 'awaiting-assessment'],
        'overdue': ['needs-action', 'ready-pickup', 'awaiting-assessment'],
        'awaiting-assessment': ['needs-action', 'ready-pickup', 'overdue'],
        'exam': [],
        'high-priority': []
    },
    users: {
        'students': ['non-students'],
        'non-students': ['students'],
        'admins': [],
        'reputation-risk': ['top-reputation'],
        'top-reputation': ['reputation-risk']
    },
    assessment: {
        'needs-assessment': ['positive-impact', 'negative-impact', 'damage-issues'],
        'late': ['on-time'],
        'on-time': ['late'],
        'positive-impact': ['needs-assessment', 'negative-impact', 'damage-issues'],
        'negative-impact': ['needs-assessment', 'positive-impact'],
        'damage-issues': ['needs-assessment', 'positive-impact']
    }
};

function loadManageQuickFilters() {
    try {
        const raw = localStorage.getItem('manage_quick_filters');
        if (raw) return JSON.parse(raw);
        const legacy = localStorage.getItem('manage_quick_filter');
        return legacy ? [legacy] : [];
    } catch (e) {
        return [];
    }
}

function loadQuickFilters(storageKey) {
    try {
        const raw = localStorage.getItem(storageKey);
        return raw ? JSON.parse(raw) : [];
    } catch (e) {
        return [];
    }
}

function nextQuickFilterState(active, name, conflictMap = {}) {
    if (active.includes(name)) return active.filter(f => f !== name);
    const conflicts = new Set(conflictMap[name] || []);
    return [...active.filter(f => f !== name && !conflicts.has(f)), name];
}

function toggleQuickFilter(stateKey, storageKey, name, applyFn, conflictMap = {}) {
    const active = state[stateKey] || [];
    state[stateKey] = nextQuickFilterState(active, name, conflictMap);
    localStorage.setItem(storageKey, JSON.stringify(state[stateKey]));
    applyFn();
}

function removeQuickFilter(stateKey, storageKey, name) {
    state[stateKey] = (state[stateKey] || []).filter(f => f !== name);
    localStorage.setItem(storageKey, JSON.stringify(state[stateKey]));
}

function removeQuickFilters(stateKey, storageKey, names) {
    const excluded = new Set(names);
    const active = state[stateKey] || [];
    const next = active.filter(f => !excluded.has(f));
    if (next.length === active.length) return;
    state[stateKey] = next;
    localStorage.setItem(storageKey, JSON.stringify(state[stateKey]));
}

function clearQuickFilters(stateKey, storageKey) {
    state[stateKey] = [];
    localStorage.removeItem(storageKey);
}

function renderQuickFilterButtons(selector, stateKey, dataKey, isActiveFn = null) {
    document.querySelectorAll(selector).forEach(btn => {
        const name = btn.dataset[dataKey];
        const active = isActiveFn ? isActiveFn(name) : (state[stateKey] || []).includes(name);
        btn.classList.toggle('active', active);
    });
}

// ===== API HELPER =====
// Counter so concurrent calls don't toggle the spinner off while others are in flight.
let pendingRequests = 0;
function setSpinner(active) {
    pendingRequests += active ? 1 : -1;
    if (pendingRequests < 0) pendingRequests = 0;
    const el = document.getElementById('global-spinner');
    if (el) el.classList.toggle('active', pendingRequests > 0);
}

async function api(path, options = {}) {
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = 'Bearer ' + token;

    setSpinner(true);
    try {
        const resp = await fetch('/api' + path, { ...options, headers });
        if (resp.status === 204) return null;

        const contentType = resp.headers.get('content-type') || '';
        const data = contentType.includes('application/json') ? await resp.json() : {};
        if (!resp.ok) {
            const msg = data.validationErrors
                ? Object.values(data.validationErrors).join(', ')
                : (data.message || resp.statusText || 'Request failed');
            throw new Error(msg);
        }
        return data;
    } finally {
        setSpinner(false);
    }
}

function emptyState(title, message) {
    return `<div class="empty-state">
        <svg class="empty-state-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="9"/>
            <path d="M9 10h.01M15 10h.01M8 15c1 1 2.5 1.5 4 1.5s3-.5 4-1.5"/>
        </svg>
        <div class="empty-state-title">${title}</div>
        <div class="empty-state-message">${message}</div>
    </div>`;
}

// ===== AUTH =====
function showAuthTab(tab) {
    document.getElementById('login-form').style.display = tab === 'login' ? 'block' : 'none';
    document.getElementById('register-form').style.display = tab === 'register' ? 'block' : 'none';
    document.querySelectorAll('.auth-tabs button').forEach((b, i) => {
        b.classList.toggle('active', (i === 0 && tab === 'login') || (i === 1 && tab === 'register'));
    });
}

async function login() {
    try {
        const data = await api('/auth/login', {
            method: 'POST',
            body: JSON.stringify({
                username: document.getElementById('login-username').value,
                password: document.getElementById('login-password').value
            })
        });
        saveAuth(data);
    } catch (e) {
        showAlert('auth-alert', e.message, 'error');
    }
}

async function register() {
    try {
        const data = await api('/auth/register', {
            method: 'POST',
            body: JSON.stringify({
                username: document.getElementById('reg-username').value,
                email: document.getElementById('reg-email').value,
                password: document.getElementById('reg-password').value,
                userType: document.getElementById('reg-usertype').value
            })
        });
        saveAuth(data);
    } catch (e) {
        showAlert('auth-alert', e.message, 'error');
    }
}

function saveAuth(data) {
    token = data.token;
    currentUser = data.user;
    localStorage.setItem('jwt_token', token);
    localStorage.setItem('current_user', JSON.stringify(currentUser));
    enterApp();
}

function logout() {
    token = null;
    currentUser = null;
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('current_user');
    document.getElementById('auth-screen').classList.remove('hidden');
    document.getElementById('app-screen').classList.add('hidden');
}

// ===== APP INIT =====
function enterApp() {
    document.getElementById('auth-screen').classList.add('hidden');
    document.getElementById('app-screen').classList.remove('hidden');

    document.getElementById('header-user').textContent = currentUser.username;
    const roleBadge = document.getElementById('header-role');
    roleBadge.textContent = currentUser.role;
    roleBadge.className = 'badge badge-' + (currentUser.role === 'ADMIN' ? 'approved' : 'rented');

    const isAdmin = currentUser.role === 'ADMIN';
    document.querySelectorAll('.admin-only').forEach(el => {
        el.classList.toggle('hidden', !isAdmin);
    });

    const examFields = document.getElementById('exam-fields');
    if (examFields) {
        examFields.classList.toggle('hidden', currentUser.userType !== 'STUDENT');
    }

    showSection('dashboard');
}

// ===== NAVIGATION =====
function showSection(name) {
    const isAdmin = currentUser && currentUser.role === 'ADMIN';
    // Admin sees the admin dashboard when clicking "Dashboard"
    const targetId = (name === 'dashboard' && isAdmin)
        ? 'section-admin-dashboard'
        : 'section-' + name;

    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    const target = document.getElementById(targetId);
    if (target) target.classList.add('active');

    document.querySelectorAll('nav button').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('nav button').forEach(b => {
        if (b.getAttribute('onclick')?.includes("'" + name + "'")) b.classList.add('active');
    });

    switch (name) {
        case 'dashboard':
            if (isAdmin) loadAdminDashboard();
            else loadClientDashboard();
            break;
        case 'equipment': loadEquipment(); break;
        case 'my-requests': loadMyRequests(); break;
        case 'new-request': loadEquipmentSelect(); break;
        case 'pending-requests': loadPendingRequests(); break;
        case 'all-users': loadAllUsers(); break;
        case 'assessments': loadAssessments(); break;
        case 'strategy': loadStrategy(); break;
    }
}

// ===== CLIENT DASHBOARD =====
async function loadClientDashboard() {
    try {
        const [me, equipment, myRequests, queuePositions] = await Promise.all([
            api('/users/me'),
            api('/equipment'),
            api('/rental-requests/my'),
            api('/rental-requests/my-queue-positions')
        ]);
        currentUser = me;
        localStorage.setItem('current_user', JSON.stringify(me));

        const available = equipment.filter(e => e.availableQuantity > 0).length;
        const activeRentals = myRequests.filter(r => r.status === 'RENTED');
        const overdueRentals = activeRentals.filter(r => r.overdue);
        const pendingMine = myRequests.filter(r => r.status === 'PENDING');

        document.getElementById('dashboard-stats').innerHTML = `
            <div class="stat-card">
                <div class="number">${me.reputationScore.toFixed(1)}</div>
                <div class="label">Reputation</div>
            </div>
            <div class="stat-card">
                <div class="number">${activeRentals.length}</div>
                <div class="label">Active Rentals</div>
            </div>
            <div class="stat-card">
                <div class="number">${pendingMine.length}</div>
                <div class="label">Pending Requests</div>
            </div>
            <div class="stat-card">
                <div class="number">${available}</div>
                <div class="label">Equipment Available</div>
            </div>
        `;

        let html = '';

        if (overdueRentals.length > 0) {
            html += `<div class="card overdue">
                <h3><span class="badge badge-overdue">OVERDUE</span> ${overdueRentals.length} rental(s) past due date</h3>
                <p style="margin-top:6px; color:#555">Return the equipment as soon as possible to avoid further reputation penalty (up to -10 points).</p>
            </div>`;
        }

        if (activeRentals.length > 0) {
            html += `<div class="card"><h3>Active Rentals</h3>`;
            for (const r of activeRentals) {
                const daysTxt = r.overdue
                    ? `<span class="badge badge-overdue">${r.daysOverdue} day(s) overdue</span>`
                    : (r.daysRemaining !== null && r.daysRemaining !== undefined
                        ? `<span class="badge badge-rented">${r.daysRemaining} day(s) remaining</span>`
                        : '');
                html += `<p style="margin:8px 0">
                    <strong>${r.equipmentName}</strong> · due ${r.endDate} ${daysTxt}
                </p>`;
            }
            html += `</div>`;
        }

        if (pendingMine.length > 0) {
            html += `<div class="card"><h3>Pending Requests — Queue Position</h3>`;
            for (const r of pendingMine) {
                const pos = queuePositions[r.id];
                const posTxt = pos
                    ? `<span class="queue-position">Position ${pos}</span>`
                    : '<span style="color:#888">not queued</span>';
                html += `<p style="margin:8px 0">
                    <strong>${r.equipmentName}</strong> · priority ${r.priorityScore?.toFixed(1) || 'N/A'} ${posTxt}
                </p>`;
            }
            html += `</div>`;
        }

        try {
            const assessments = await api('/return-assessments/user/' + me.id);
            if (assessments.length > 0) {
                const recent = assessments.slice(-5).reverse();
                html += `<div class="card"><h3>Reputation History (recent)</h3>
                    <ul class="reputation-list">
                        ${recent.map(a => {
                            const impact = a.reputationImpact;
                            const cls = impact > 0 ? 'rep-positive' : impact < 0 ? 'rep-negative' : 'rep-neutral';
                            const sign = impact > 0 ? '+' : '';
                            return `<li>
                                <span>${a.conditionRating} — Request #${a.rentalRequestId}</span>
                                <span class="${cls}">${sign}${impact.toFixed(1)}</span>
                            </li>`;
                        }).join('')}
                    </ul>
                </div>`;
            }
        } catch (e) { /* ignore if not accessible */ }

        if (!html) {
            html = `<div class="card">
                <h3>Welcome, ${me.username}!</h3>
                <p>No active rentals or pending requests yet. Browse equipment to get started.</p>
                <p class="meta">Account created: ${formatDate(me.createdAt)}</p>
            </div>`;
        }

        document.getElementById('dashboard-content').innerHTML = html;
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

// ===== ADMIN DASHBOARD =====
async function loadAdminDashboard() {
    try {
        const [stats, feed] = await Promise.all([
            api('/admin/dashboard-stats'),
            api('/admin/activity-feed?limit=20')
        ]);

        renderKpiCards(stats);
        renderStatusChart(stats.statusDistribution);
        renderTopUsersChart(stats.topUsers);
        renderUtilizationChart(stats.perEquipmentUtilization);
        renderActivityFeed(feed);
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

function renderKpiCards(stats) {
    const pct = stats.utilizationPct || 0;
    document.getElementById('admin-kpi-cards').innerHTML = `
        <button class="kpi-card kpi-action" onclick="openManageRequests({ status: 'PENDING' })">
            <div class="kpi-value">${stats.pendingCount}</div>
            <div class="kpi-label">Pending Requests</div>
        </button>
        <button class="kpi-card kpi-success kpi-action" onclick="openManageRequests({ status: 'RENTED' })">
            <div class="kpi-value">${stats.activeRentalsCount}</div>
            <div class="kpi-label">Active Rentals</div>
        </button>
        <button class="kpi-card ${stats.overdueCount > 0 ? 'kpi-danger' : ''} kpi-action" onclick="openManageRequests({ quick: 'overdue' })">
            <div class="kpi-value">${stats.overdueCount}</div>
            <div class="kpi-label">Overdue</div>
        </button>
        <button class="kpi-card kpi-warning kpi-action" onclick="openManageRequests({ status: 'RETURNED' })">
            <div class="kpi-value">${stats.returnedCount || 0}</div>
            <div class="kpi-label">Awaiting Assessment</div>
        </button>
        <button class="kpi-card ${pct >= 80 ? 'kpi-warning' : ''} kpi-action" onclick="showSection('equipment')">
            <div class="kpi-value">${pct.toFixed(1)}%</div>
            <div class="kpi-label">Utilization</div>
        </button>
    `;
}

function openManageRequests(filters = {}) {
    const saved = JSON.parse(localStorage.getItem('manage_filters') || '{"mgr-sort":"priority"}');
    if (filters.status !== undefined) saved['mgr-status-filter'] = filters.status;
    if (!saved['mgr-sort']) saved['mgr-sort'] = 'priority';
    if (filters.quick) {
        addManageQuickFilter(filters.quick);
        if (filters.quick === 'overdue') saved['mgr-action-filter'] = 'overdue';
        if (filters.quick === 'awaiting-assessment') saved['mgr-status-filter'] = 'RETURNED';
    }
    localStorage.setItem('manage_filters', JSON.stringify(saved));
    showSection('pending-requests');
}

function renderStatusChart(distribution) {
    const ctx = document.getElementById('chart-status');
    if (state.charts.status) state.charts.status.destroy();
    const labels = Object.keys(distribution);
    const data = Object.values(distribution);
    state.charts.status = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels,
            datasets: [{
                data,
                backgroundColor: ['#ffc107', '#28a745', '#dc3545', '#0d6efd', '#6c757d', '#198754']
            }]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}

function renderTopUsersChart(users) {
    const ctx = document.getElementById('chart-top-users');
    if (state.charts.topUsers) state.charts.topUsers.destroy();
    state.charts.topUsers = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: users.map(u => u.username),
            datasets: [{
                label: 'Reputation',
                data: users.map(u => u.reputationScore),
                backgroundColor: '#16213e'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: { y: { beginAtZero: true, max: 200 } }
        }
    });
}

function renderUtilizationChart(perEquipment) {
    const ctx = document.getElementById('chart-utilization');
    if (state.charts.utilization) state.charts.utilization.destroy();
    const labels = Object.keys(perEquipment);
    const data = Object.values(perEquipment);
    state.charts.utilization = new Chart(ctx, {
        type: 'bar',
        data: {
            labels,
            datasets: [{
                label: 'Utilization %',
                data,
                backgroundColor: data.map(v => v >= 80 ? '#dc3545' : v >= 50 ? '#ffc107' : '#28a745')
            }]
        },
        options: {
            indexAxis: 'y',
            responsive: true,
            maintainAspectRatio: false,
            scales: { x: { beginAtZero: true, max: 100 } }
        }
    });
}

function renderActivityFeed(events) {
    const container = document.getElementById('activity-feed-list');
    if (!events || events.length === 0) {
        container.innerHTML = emptyState(
            'No recent activity',
            'Requests and assessments will appear here as they happen.'
        );
        return;
    }
    container.innerHTML = events.map(ev => {
        const iconClass = activityIconClass(ev.type);
        const iconLetter = ev.type.includes('ASSESSMENT') ? 'A' : ev.type.replace('REQUEST_', '').charAt(0);
        return `<div class="activity-item activity-clickable" onclick="openActivityReference(${ev.referenceId})">
            <div class="activity-icon ${iconClass}">${iconLetter}</div>
            <div class="activity-body">
                <div class="activity-message">${ev.message}</div>
                <div class="activity-meta">by ${ev.actorUsername} · ref #${ev.referenceId}</div>
            </div>
            <div class="activity-time">${timeAgo(ev.timestamp)}</div>
        </div>`;
    }).join('');
}

function openActivityReference(referenceId) {
    if (!referenceId) return;
    state.manageQuickFilters = [];
    localStorage.setItem('manage_quick_filters', '[]');
    localStorage.removeItem('manage_quick_filter');
    localStorage.setItem('manage_filters', JSON.stringify({
        'mgr-search': String(referenceId),
        'mgr-sort': 'newest'
    }));
    showSection('pending-requests');
}

function activityIconClass(type) {
    if (type === 'ASSESSMENT_DONE') return 'activity-icon-assessment';
    if (type === 'REQUEST_APPROVED') return 'activity-icon-approved';
    if (type === 'REQUEST_REJECTED') return 'activity-icon-rejected';
    if (type === 'REQUEST_RENTED') return 'activity-icon-rented';
    if (type === 'REQUEST_RETURNED') return 'activity-icon-returned';
    if (type === 'REQUEST_COMPLETED') return 'activity-icon-completed';
    return 'activity-icon-created';
}

// ===== EQUIPMENT =====
async function loadEquipment() {
    try {
        const list = await api('/equipment');
        state.equipmentCache = list;

        const catSet = new Set(list.map(e => e.category).filter(Boolean));
        const catSelect = document.getElementById('eq-category-filter');
        const currentVal = catSelect.value;
        catSelect.innerHTML = '<option value="">All categories</option>' +
            [...catSet].sort().map(c => `<option value="${c}">${c}</option>`).join('');
        catSelect.value = currentVal;

        applyEquipmentFilters();
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

function applyEquipmentFilters() {
    const search = (document.getElementById('eq-search')?.value || '').toLowerCase();
    const cat = document.getElementById('eq-category-filter')?.value || '';
    const avail = document.getElementById('eq-availability-filter')?.value || '';
    const sort = document.getElementById('eq-sort')?.value || 'name';
    syncEquipmentQuickFiltersWithControls(avail);

    let filtered = [...state.equipmentCache];
    if (search) filtered = filtered.filter(e => e.name.toLowerCase().includes(search)
        || (e.description || '').toLowerCase().includes(search));
    if (cat) filtered = filtered.filter(e => e.category === cat);
    if (avail === 'available') filtered = filtered.filter(e => e.availableQuantity > 0);
    if (avail === 'unavailable') filtered = filtered.filter(e => e.availableQuantity === 0);
    if (avail === 'scarce') filtered = filtered.filter(e => e.availableQuantity > 0 && e.availableQuantity <= Math.max(1, Math.ceil(e.totalQuantity * 0.25)));
    if (state.equipmentQuickFilters.length > 0) {
        filtered = filtered.filter(e => state.equipmentQuickFilters.every(name => equipmentQuickFilterMatches(e, name)));
    }

    filtered.sort((a, b) => {
        const utilization = e => e.totalQuantity ? (e.totalQuantity - e.availableQuantity) / e.totalQuantity : 0;
        if (sort === 'availability-low') return a.availableQuantity - b.availableQuantity || a.name.localeCompare(b.name);
        if (sort === 'availability-high') return b.availableQuantity - a.availableQuantity || a.name.localeCompare(b.name);
        if (sort === 'utilization-high') return utilization(b) - utilization(a) || a.name.localeCompare(b.name);
        return a.name.localeCompare(b.name);
    });
    renderEquipmentFilterState(filtered.length);

    const container = document.getElementById('equipment-list');
    if (filtered.length === 0) {
        container.innerHTML = emptyState(
            'No equipment found',
            'Try adjusting your search or filters.'
        );
        return;
    }
    container.innerHTML = filtered.map(eq => {
        const scarce = eq.availableQuantity > 0 && eq.availableQuantity <= Math.max(1, Math.ceil(eq.totalQuantity * 0.25));
        const utilization = eq.totalQuantity ? Math.round(((eq.totalQuantity - eq.availableQuantity) / eq.totalQuantity) * 100) : 0;
        return `
        <div class="card equipment-card" onclick="showEquipmentDetailModal(${eq.id})">
            <h3>${eq.name}</h3>
            <p>${eq.description || 'No description'}</p>
            <p><strong>Category:</strong> ${eq.category}</p>
            <p><strong>Available:</strong> ${eq.availableQuantity} / ${eq.totalQuantity}
               <span class="badge badge-${eq.status.toLowerCase()}">${eq.status}</span>
               ${scarce ? '<span class="badge badge-overdue">LOW STOCK</span>' : ''}</p>
            <p><strong>Utilization:</strong> ${utilization}%</p>
            <p class="meta">Added: ${formatDate(eq.createdAt)}</p>
            ${currentUser.role === 'ADMIN' ? `
                <div class="actions" onclick="event.stopPropagation()">
                    <button class="btn btn-warning btn-sm" onclick="showEditEquipmentModal(${eq.id})">Edit</button>
                    <button class="btn btn-danger btn-sm" onclick="deleteEquipment(${eq.id})">Delete</button>
                </div>
            ` : ''}
        </div>
    `; }).join('');
}

function renderEquipmentFilterState(count) {
    renderQuickFilterButtons('[data-equipment-filter]', 'equipmentQuickFilters', 'equipmentFilter', isEquipmentQuickFilterActive);
    setFilterActiveState(['eq-search', 'eq-category-filter', 'eq-availability-filter', 'eq-sort'], {
        'eq-sort': 'name'
    });
    const filters = [];
    const search = document.getElementById('eq-search')?.value || '';
    const category = document.getElementById('eq-category-filter')?.value || '';
    const availability = document.getElementById('eq-availability-filter')?.value || '';
    const sort = document.getElementById('eq-sort')?.value || 'name';
    if (search) filters.push({ id: 'eq-search', label: `Search: ${search}` });
    if (category) filters.push({ id: 'eq-category-filter', label: `Category: ${category}` });
    if (availability) filters.push({ id: 'eq-availability-filter', label: `Availability: ${labelForValue(availability)}` });
    if (sort !== 'name') filters.push({ id: 'eq-sort', label: `Sort: ${labelForValue(sort)}` });
    state.equipmentQuickFilters.forEach(name => {
        filters.push({ id: `quick:${name}`, label: `Quick: ${equipmentQuickFilterLabel(name)}` });
    });
    renderFilterSummary('equipment-summary', `<strong>${count}</strong> equipment shown`, filters, 'removeEquipmentFilter');
}

function setEquipmentQuickFilter(name) {
    if (isEquipmentQuickFilterActive(name)) {
        removeQuickFilter('equipmentQuickFilters', 'equipment_quick_filters', name);
        clearEquipmentControlForQuick(name);
        applyEquipmentFilters();
        return;
    }
    clearEquipmentControlConflicts(name);
    toggleQuickFilter('equipmentQuickFilters', 'equipment_quick_filters', name, applyEquipmentFilters, quickFilterConflicts.equipment);
}

function isEquipmentQuickFilterActive(name) {
    const availability = document.getElementById('eq-availability-filter')?.value || '';
    if ((state.equipmentQuickFilters || []).includes(name)) return true;
    if (name === 'available') return availability === 'available';
    if (name === 'scarce') return availability === 'scarce';
    if (name === 'out-of-stock') return availability === 'unavailable';
    return false;
}

function clearEquipmentControlForQuick(name) {
    const availability = document.getElementById('eq-availability-filter');
    if (!availability) return;
    if (name === 'available' && availability.value === 'available') availability.value = '';
    if (name === 'scarce' && availability.value === 'scarce') availability.value = '';
    if (name === 'out-of-stock' && availability.value === 'unavailable') availability.value = '';
}

function clearEquipmentControlConflicts(name) {
    (quickFilterConflicts.equipment[name] || []).forEach(clearEquipmentControlForQuick);
}

function syncEquipmentQuickFiltersWithControls(availability) {
    if (availability === 'available') {
        removeQuickFilters('equipmentQuickFilters', 'equipment_quick_filters', ['out-of-stock']);
    }
    if (availability === 'scarce') {
        removeQuickFilters('equipmentQuickFilters', 'equipment_quick_filters', ['out-of-stock']);
    }
    if (availability === 'unavailable') {
        removeQuickFilters('equipmentQuickFilters', 'equipment_quick_filters', ['available', 'scarce']);
    }
}

function equipmentQuickFilterMatches(eq, name) {
    const scarce = eq.availableQuantity > 0 && eq.availableQuantity <= Math.max(1, Math.ceil(eq.totalQuantity * 0.25));
    const utilization = eq.totalQuantity ? (eq.totalQuantity - eq.availableQuantity) / eq.totalQuantity : 0;
    if (name === 'available') return eq.availableQuantity > 0;
    if (name === 'scarce') return scarce;
    if (name === 'out-of-stock') return eq.availableQuantity === 0;
    if (name === 'high-utilization') return utilization >= 0.75;
    return true;
}

function equipmentQuickFilterLabel(name) {
    const labels = {
        'available': 'Available',
        'scarce': 'Low stock',
        'out-of-stock': 'Out of stock',
        'high-utilization': 'High utilization'
    };
    return labels[name] || labelForValue(name);
}

function removeEquipmentFilter(id) {
    if (id.startsWith('quick:')) {
        removeQuickFilter('equipmentQuickFilters', 'equipment_quick_filters', id.substring(6));
        applyEquipmentFilters();
        return;
    }
    const el = document.getElementById(id);
    if (!el) return;
    el.value = id === 'eq-sort' ? 'name' : '';
    applyEquipmentFilters();
}

function resetEquipmentFilters() {
    clearQuickFilters('equipmentQuickFilters', 'equipment_quick_filters');
    ['eq-search', 'eq-category-filter', 'eq-availability-filter'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });
    const sort = document.getElementById('eq-sort');
    if (sort) sort.value = 'name';
    applyEquipmentFilters();
}

async function showEquipmentDetailModal(id) {
    try {
        const eq = state.equipmentCache.find(e => e.id === id) || await api('/equipment/' + id);
        let queueHtml = '<p style="color:#888">No pending requests in queue.</p>';
        let strategyHtml = '';
        try {
            const [queue, strategy] = await Promise.all([
                api('/rental-requests/prioritized/' + id),
                api('/admin/prioritization-strategy')
            ]);
            strategyHtml = `<p class="meta">Active strategy: <strong>${strategy.strategy}</strong></p>`;
            if (queue.length > 0) {
                queueHtml = `<div class="queue-list">${queue.map((r, i) => `
                    <div class="queue-row">
                        <span class="queue-rank">#${i + 1}</span>
                        <span><strong>${r.username}</strong> (${r.userType || 'USER'})</span>
                        <span class="priority-score ${priorityClassFor(r.priorityScore)}">${r.priorityScore?.toFixed(1) || 'N/A'}</span>
                        ${r.isForExam ? '<span class="badge badge-pending">EXAM</span>' : ''}
                        <span class="meta">${r.startDate} to ${r.endDate}</span>
                    </div>
                `).join('')}</div>`;
            }
        } catch (e) { /* queue may be admin-only; keep empty */ }

        const utilization = eq.totalQuantity ? Math.round(((eq.totalQuantity - eq.availableQuantity) / eq.totalQuantity) * 100) : 0;
        openModal(`
            <h3>${eq.name}</h3>
            <p>${eq.description || 'No description'}</p>
            <p><strong>Category:</strong> ${eq.category}</p>
            <p><strong>Status:</strong> <span class="badge badge-${eq.status.toLowerCase()}">${eq.status}</span></p>
            <p><strong>Available:</strong> ${eq.availableQuantity} / ${eq.totalQuantity}</p>
            <p><strong>Utilization:</strong> ${utilization}%</p>
            <p class="meta">Added: ${formatDate(eq.createdAt)}</p>
            <hr style="margin:16px 0">
            <h3 style="margin-bottom:8px">Priority Queue</h3>
            ${strategyHtml}
            ${queueHtml}
            <div class="actions" style="margin-top:16px">
                <button class="btn" onclick="closeModal()">Close</button>
            </div>
        `);
    } catch (e) {
        toast(e.message, 'error');
    }
}

function showAddEquipmentModal() {
    openModal(`
        <h3>Add Equipment</h3>
        <div class="form-group">
            <label>Name</label>
            <input type="text" id="eq-name" placeholder="e.g. Rigol DS1054Z Oscilloscope">
        </div>
        <div class="form-group">
            <label>Description</label>
            <textarea id="eq-desc" placeholder="Equipment description..."></textarea>
        </div>
        <div class="form-group">
            <label>Category</label>
            <input type="text" id="eq-cat" placeholder="e.g. Oscilloscope">
        </div>
        <div class="form-group">
            <label>Total Quantity</label>
            <input type="number" id="eq-qty" min="1" value="1">
        </div>
        <div class="actions">
            <button class="btn btn-primary" onclick="addEquipment()">Add</button>
            <button class="btn" onclick="closeModal()">Cancel</button>
        </div>
    `);
}

async function addEquipment() {
    try {
        await api('/equipment', {
            method: 'POST',
            body: JSON.stringify({
                name: document.getElementById('eq-name').value,
                description: document.getElementById('eq-desc').value,
                category: document.getElementById('eq-cat').value,
                totalQuantity: parseInt(document.getElementById('eq-qty').value)
            })
        });
        closeModal();
        toast('Equipment added', 'success');
        loadEquipment();
    } catch (e) {
        toast(e.message, 'error');
    }
}

async function showEditEquipmentModal(id) {
    try {
        const eq = await api('/equipment/' + id);
        openModal(`
            <h3>Edit Equipment</h3>
            <div class="form-group">
                <label>Name</label>
                <input type="text" id="eq-name" value="${eq.name}">
            </div>
            <div class="form-group">
                <label>Description</label>
                <textarea id="eq-desc">${eq.description || ''}</textarea>
            </div>
            <div class="form-group">
                <label>Category</label>
                <input type="text" id="eq-cat" value="${eq.category}">
            </div>
            <div class="form-group">
                <label>Total Quantity</label>
                <input type="number" id="eq-qty" min="1" value="${eq.totalQuantity}">
            </div>
            <div class="actions">
                <button class="btn btn-primary" onclick="updateEquipment(${id})">Save</button>
                <button class="btn" onclick="closeModal()">Cancel</button>
            </div>
        `);
    } catch (e) {
        toast(e.message, 'error');
    }
}

async function updateEquipment(id) {
    try {
        await api('/equipment/' + id, {
            method: 'PUT',
            body: JSON.stringify({
                name: document.getElementById('eq-name').value,
                description: document.getElementById('eq-desc').value,
                category: document.getElementById('eq-cat').value,
                totalQuantity: parseInt(document.getElementById('eq-qty').value)
            })
        });
        closeModal();
        toast('Equipment updated', 'success');
        loadEquipment();
    } catch (e) {
        toast(e.message, 'error');
    }
}

async function deleteEquipment(id) {
    if (!confirm('Delete this equipment? Existing related requests may prevent deletion.')) return;
    try {
        await api('/equipment/' + id, { method: 'DELETE' });
        toast('Equipment deleted', 'success');
        loadEquipment();
    } catch (e) {
        toast(e.message, 'error');
    }
}

// ===== MY REQUESTS =====
async function loadMyRequests() {
    try {
        const [list, positions] = await Promise.all([
            api('/rental-requests/my'),
            api('/rental-requests/my-queue-positions')
        ]);
        state.myRequestsCache = list;
        state.myQueuePositions = positions || {};
        applyMyRequestsFilters();
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

function applyMyRequestsFilters() {
    const search = (document.getElementById('my-search')?.value || '').toLowerCase();
    const status = document.getElementById('my-status-filter')?.value || '';
    const timing = document.getElementById('my-timing-filter')?.value || '';
    const sort = document.getElementById('my-sort')?.value || 'newest';
    syncMyRequestsQuickFiltersWithControls(status, timing);

    let list = [...state.myRequestsCache];
    if (search) list = list.filter(r =>
        r.equipmentName.toLowerCase().includes(search) ||
        (r.projectDescription || '').toLowerCase().includes(search) ||
        String(r.id).includes(search));
    if (status) list = list.filter(r => r.status === status);
    if (timing === 'overdue') list = list.filter(r => r.overdue);
    if (timing === 'active') list = list.filter(r => r.status === 'RENTED');
    if (timing === 'exam') list = list.filter(r => r.isForExam);
    if (state.myRequestsQuickFilters.length > 0) {
        list = list.filter(r => state.myRequestsQuickFilters.every(name => myRequestsQuickFilterMatches(r, name)));
    }

    if (sort === 'overdue') {
        list.sort((a, b) => {
            if (a.overdue !== b.overdue) return a.overdue ? -1 : 1;
            return new Date(b.createdAt) - new Date(a.createdAt);
        });
    } else if (sort === 'priority') {
        list.sort((a, b) => (b.priorityScore || 0) - (a.priorityScore || 0));
    } else if (sort === 'oldest') {
        list.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
    } else {
        list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    }
    renderMyRequestsFilterState(list.length);

    const container = document.getElementById('my-requests-list');
    if (list.length === 0) {
        const isFiltered = !!(search || status || timing || sort !== 'newest' || state.myRequestsQuickFilters.length);
        container.innerHTML = emptyState(
            isFiltered ? 'No requests match your filter' : 'No requests yet',
            isFiltered ? 'Try selecting a different status.' : 'Create one from the "New Request" tab to get started.'
        );
        return;
    }
    container.innerHTML = list.map(r => renderRequestCard(r, false, state.myQueuePositions[r.id])).join('');
}

function renderMyRequestsFilterState(count) {
    renderQuickFilterButtons('[data-my-filter]', 'myRequestsQuickFilters', 'myFilter', isMyRequestsQuickFilterActive);
    setFilterActiveState(['my-search', 'my-status-filter', 'my-timing-filter', 'my-sort'], {
        'my-sort': 'newest'
    });
    const filters = [];
    const search = document.getElementById('my-search')?.value || '';
    const status = document.getElementById('my-status-filter')?.value || '';
    const timing = document.getElementById('my-timing-filter')?.value || '';
    const sort = document.getElementById('my-sort')?.value || 'newest';
    if (search) filters.push({ id: 'my-search', label: `Search: ${search}` });
    if (status) filters.push({ id: 'my-status-filter', label: `Status: ${labelForValue(status)}` });
    if (timing) filters.push({ id: 'my-timing-filter', label: `Timing: ${labelForValue(timing)}` });
    if (sort !== 'newest') filters.push({ id: 'my-sort', label: `Sort: ${labelForValue(sort)}` });
    state.myRequestsQuickFilters.forEach(name => {
        filters.push({ id: `quick:${name}`, label: `Quick: ${myRequestsQuickFilterLabel(name)}` });
    });
    renderFilterSummary('my-summary', `<strong>${count}</strong> requests shown`, filters, 'removeMyRequestsFilter');
}

function setMyRequestsQuickFilter(name) {
    if (isMyRequestsQuickFilterActive(name)) {
        removeQuickFilter('myRequestsQuickFilters', 'my_requests_quick_filters', name);
        clearMyRequestsControlForQuick(name);
        applyMyRequestsFilters();
        return;
    }
    clearMyRequestsControlConflicts(name);
    toggleQuickFilter('myRequestsQuickFilters', 'my_requests_quick_filters', name, applyMyRequestsFilters, quickFilterConflicts.myRequests);
}

function isMyRequestsQuickFilterActive(name) {
    const status = document.getElementById('my-status-filter')?.value || '';
    const timing = document.getElementById('my-timing-filter')?.value || '';
    if ((state.myRequestsQuickFilters || []).includes(name)) return true;
    if (name === 'pending') return status === 'PENDING';
    if (name === 'active') return status === 'RENTED' || timing === 'active';
    if (name === 'overdue') return timing === 'overdue';
    if (name === 'awaiting-assessment') return status === 'RETURNED';
    if (name === 'exam') return timing === 'exam';
    return false;
}

function clearMyRequestsControlForQuick(name) {
    const status = document.getElementById('my-status-filter');
    const timing = document.getElementById('my-timing-filter');
    if (name === 'pending' && status?.value === 'PENDING') status.value = '';
    if (name === 'active') {
        if (status?.value === 'RENTED') status.value = '';
        if (timing?.value === 'active') timing.value = '';
    }
    if (name === 'overdue' && timing?.value === 'overdue') timing.value = '';
    if (name === 'awaiting-assessment' && status?.value === 'RETURNED') status.value = '';
    if (name === 'exam' && timing?.value === 'exam') timing.value = '';
}

function clearMyRequestsControlConflicts(name) {
    (quickFilterConflicts.myRequests[name] || []).forEach(clearMyRequestsControlForQuick);
}

function syncMyRequestsQuickFiltersWithControls(status, timing) {
    if (status === 'PENDING') {
        removeQuickFilters('myRequestsQuickFilters', 'my_requests_quick_filters', ['active', 'overdue', 'awaiting-assessment']);
    } else if (status === 'RENTED') {
        removeQuickFilters('myRequestsQuickFilters', 'my_requests_quick_filters', ['pending', 'awaiting-assessment']);
    } else if (status === 'RETURNED') {
        removeQuickFilters('myRequestsQuickFilters', 'my_requests_quick_filters', ['pending', 'active', 'overdue']);
    } else if (status) {
        removeQuickFilters('myRequestsQuickFilters', 'my_requests_quick_filters', ['pending', 'active', 'overdue', 'awaiting-assessment']);
    }
    if (timing === 'active' || timing === 'overdue') {
        removeQuickFilters('myRequestsQuickFilters', 'my_requests_quick_filters', ['pending', 'awaiting-assessment']);
    }
}

function myRequestsQuickFilterMatches(req, name) {
    if (name === 'pending') return req.status === 'PENDING';
    if (name === 'active') return req.status === 'RENTED';
    if (name === 'overdue') return req.overdue;
    if (name === 'awaiting-assessment') return req.status === 'RETURNED';
    if (name === 'exam') return req.isForExam;
    if (name === 'high-priority') return (req.priorityScore || 0) >= 75;
    return true;
}

function myRequestsQuickFilterLabel(name) {
    const labels = {
        'pending': 'Pending',
        'active': 'Active rentals',
        'overdue': 'Overdue',
        'awaiting-assessment': 'Awaiting assessment',
        'exam': 'Exam requests',
        'high-priority': 'High priority'
    };
    return labels[name] || labelForValue(name);
}

function removeMyRequestsFilter(id) {
    if (id.startsWith('quick:')) {
        removeQuickFilter('myRequestsQuickFilters', 'my_requests_quick_filters', id.substring(6));
        applyMyRequestsFilters();
        return;
    }
    const el = document.getElementById(id);
    if (!el) return;
    el.value = id === 'my-sort' ? 'newest' : '';
    applyMyRequestsFilters();
}

function resetMyRequestsFilters() {
    clearQuickFilters('myRequestsQuickFilters', 'my_requests_quick_filters');
    ['my-search', 'my-status-filter', 'my-timing-filter'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });
    const sort = document.getElementById('my-sort');
    if (sort) sort.value = 'newest';
    applyMyRequestsFilters();
}

// ===== NEW REQUEST =====
async function loadEquipmentSelect() {
    try {
        const list = await api('/equipment');
        const select = document.getElementById('req-equipment');
        select.innerHTML = list
            .filter(e => e.availableQuantity > 0)
            .map(e => `<option value="${e.id}">${e.name} (${e.availableQuantity} available)</option>`)
            .join('');
        if (select.innerHTML === '') {
            select.innerHTML = '<option disabled>No equipment available</option>';
        }
        const today = new Date();
        const nextWeek = new Date(today);
        nextWeek.setDate(today.getDate() + 7);
        document.getElementById('req-start').value = formatDateInput(today);
        document.getElementById('req-end').value = formatDateInput(nextWeek);
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

function toggleExamFields() {
    const checked = document.getElementById('req-exam').checked;
    document.getElementById('exam-extra').classList.toggle('hidden', !checked);
}

async function createRequest() {
    try {
        const body = {
            equipmentId: parseInt(document.getElementById('req-equipment').value),
            startDate: document.getElementById('req-start').value,
            endDate: document.getElementById('req-end').value,
            projectDescription: document.getElementById('req-description').value || null,
            isForExam: document.getElementById('req-exam')?.checked || false
        };
        if (body.isForExam) {
            body.examDate = document.getElementById('req-exam-date').value || null;
            body.justification = document.getElementById('req-justification').value || null;
        }
        const result = await api('/rental-requests', { method: 'POST', body: JSON.stringify(body) });
        toast(`Request created (priority ${result.priorityScore?.toFixed(1) || 'N/A'})`, 'success');
        document.getElementById('req-description').value = '';
        showSection('my-requests');
    } catch (e) {
        toast(e.message, 'error');
    }
}

// ===== MANAGE REQUESTS (Admin) =====
async function loadPendingRequests() {
    try {
        const [list, strategyData] = await Promise.all([
            api('/rental-requests/all'),
            api('/admin/prioritization-strategy')
        ]);
        state.requestsCache = list;
        state.activeStrategy = strategyData.strategy;
        populateManageCategoryFilter(list);
        restoreManageFilters();
        applyManageFilters();
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

function populateManageCategoryFilter(list) {
    const select = document.getElementById('mgr-category-filter');
    if (!select) return;
    const currentVal = select.value;
    const categories = [...new Set(list.map(r => r.equipmentCategory).filter(Boolean))].sort();
    select.innerHTML = '<option value="">All categories</option>' +
        categories.map(c => `<option value="${c}">${c}</option>`).join('');
    select.value = categories.includes(currentVal) ? currentVal : '';
}

function restoreManageFilters() {
    const saved = JSON.parse(localStorage.getItem('manage_filters') || '{}');
    const ids = ['mgr-search', 'mgr-status-filter', 'mgr-user-type-filter', 'mgr-category-filter',
        'mgr-priority-filter', 'mgr-action-filter', 'mgr-sort', 'mgr-start-from', 'mgr-start-to'];
    ids.forEach(id => {
        const el = document.getElementById(id);
        if (!el) return;
        if (saved[id] !== undefined) el.value = saved[id];
        else el.value = id === 'mgr-sort' ? 'priority' : '';
    });
}

function persistManageFilters() {
    const ids = ['mgr-search', 'mgr-status-filter', 'mgr-user-type-filter', 'mgr-category-filter',
        'mgr-priority-filter', 'mgr-action-filter', 'mgr-sort', 'mgr-start-from', 'mgr-start-to'];
    const saved = {};
    ids.forEach(id => {
        const el = document.getElementById(id);
        if (el) saved[id] = el.value;
    });
    localStorage.setItem('manage_filters', JSON.stringify(saved));
    localStorage.setItem('manage_quick_filters', JSON.stringify(state.manageQuickFilters || []));
    localStorage.removeItem('manage_quick_filter');
}

function setQuickFilter(name) {
    if (isManageQuickFilterActive(name)) {
        removeManageQuickFilter(name);
        clearShortcutControl(name);
    } else {
        (quickFilterConflicts.manage[name] || []).forEach(clearShortcutControl);
        addManageQuickFilter(name);
        applyShortcutControl(name);
    }
    applyManageFilters();
}

function addManageQuickFilter(name) {
    state.manageQuickFilters = nextQuickFilterState(state.manageQuickFilters || [], name, quickFilterConflicts.manage);
}

function removeManageQuickFilter(name) {
    state.manageQuickFilters = state.manageQuickFilters.filter(f => f !== name);
}

function isManageQuickFilterActive(name) {
    if (state.manageQuickFilters.includes(name)) return true;
    const status = document.getElementById('mgr-status-filter')?.value || '';
    const action = document.getElementById('mgr-action-filter')?.value || '';
    const priority = document.getElementById('mgr-priority-filter')?.value || '';
    if (name === 'needs-action') return status === 'PENDING';
    if (name === 'ready-pickup') return status === 'APPROVED';
    if (name === 'awaiting-assessment') return status === 'RETURNED';
    if (name === 'overdue') return action === 'overdue';
    if (name === 'exam') return action === 'exam';
    if (name === 'high-priority') return priority === 'high';
    return false;
}

function applyShortcutControl(name) {
    if (name === 'needs-action') document.getElementById('mgr-status-filter').value = 'PENDING';
    if (name === 'ready-pickup') document.getElementById('mgr-status-filter').value = 'APPROVED';
    if (name === 'awaiting-assessment') document.getElementById('mgr-status-filter').value = 'RETURNED';
    if (name === 'overdue') document.getElementById('mgr-action-filter').value = 'overdue';
    if (name === 'exam') document.getElementById('mgr-action-filter').value = 'exam';
    if (name === 'high-priority') document.getElementById('mgr-priority-filter').value = 'high';
}

function clearShortcutControl(name) {
    if (name === 'needs-action' && document.getElementById('mgr-status-filter').value === 'PENDING') document.getElementById('mgr-status-filter').value = '';
    if (name === 'ready-pickup' && document.getElementById('mgr-status-filter').value === 'APPROVED') document.getElementById('mgr-status-filter').value = '';
    if (name === 'awaiting-assessment' && document.getElementById('mgr-status-filter').value === 'RETURNED') document.getElementById('mgr-status-filter').value = '';
    if (name === 'overdue' && document.getElementById('mgr-action-filter').value === 'overdue') document.getElementById('mgr-action-filter').value = '';
    if (name === 'exam' && document.getElementById('mgr-action-filter').value === 'exam') document.getElementById('mgr-action-filter').value = '';
    if (name === 'high-priority' && document.getElementById('mgr-priority-filter').value === 'high') document.getElementById('mgr-priority-filter').value = '';
}

function resetManageFilters() {
    state.manageQuickFilters = [];
    localStorage.removeItem('manage_filters');
    localStorage.removeItem('manage_quick_filters');
    localStorage.removeItem('manage_quick_filter');
    ['mgr-search', 'mgr-status-filter', 'mgr-user-type-filter', 'mgr-category-filter',
        'mgr-priority-filter', 'mgr-action-filter', 'mgr-start-from', 'mgr-start-to'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });
    const sort = document.getElementById('mgr-sort');
    if (sort) sort.value = 'priority';
    applyManageFilters();
}

function applyManageFilters() {
    const search = (document.getElementById('mgr-search')?.value || '').toLowerCase();
    const status = document.getElementById('mgr-status-filter')?.value || '';
    const userType = document.getElementById('mgr-user-type-filter')?.value || '';
    const category = document.getElementById('mgr-category-filter')?.value || '';
    const priority = document.getElementById('mgr-priority-filter')?.value || '';
    const action = document.getElementById('mgr-action-filter')?.value || '';
    const sort = document.getElementById('mgr-sort')?.value || 'priority';
    const startFrom = document.getElementById('mgr-start-from')?.value || '';
    const startTo = document.getElementById('mgr-start-to')?.value || '';
    syncManageQuickFiltersWithControls(status, action);

    let list = [...state.requestsCache];
    if (search) list = list.filter(r =>
        r.username.toLowerCase().includes(search) ||
        r.equipmentName.toLowerCase().includes(search) ||
        (r.equipmentCategory || '').toLowerCase().includes(search) ||
        String(r.id).includes(search));
    if (status) list = list.filter(r => r.status === status);
    if (userType) list = list.filter(r => r.userType === userType);
    if (category) list = list.filter(r => r.equipmentCategory === category);
    if (priority === 'high') list = list.filter(r => (r.priorityScore || 0) >= 75);
    if (priority === 'medium') list = list.filter(r => (r.priorityScore || 0) >= 50 && (r.priorityScore || 0) < 75);
    if (priority === 'low') list = list.filter(r => (r.priorityScore || 0) < 50);
    if (action === 'needs-action') list = list.filter(isNeedsAction);
    if (action === 'overdue') list = list.filter(r => r.overdue);
    if (action === 'exam') list = list.filter(r => r.isForExam);
    if (startFrom) list = list.filter(r => r.startDate >= startFrom);
    if (startTo) list = list.filter(r => r.startDate <= startTo);

    if (state.manageQuickFilters.length > 0) {
        list = list.filter(r => state.manageQuickFilters.every(name => quickFilterMatches(r, name)));
    }

    sortManageRequests(list, sort);
    persistManageFilters();
    renderManageFilterState();
    renderManageSummary(list);

    const container = document.getElementById('pending-requests-list');
    if (list.length === 0) {
        container.innerHTML = emptyState(
            'No requests to manage',
            'Try clearing your filters, or check back once users submit new requests.'
        );
        return;
    }

    if (status) {
        container.innerHTML = list.map(r => renderRequestCard(r, true)).join('');
        return;
    }

    const pending  = list.filter(r => r.status === 'PENDING');
    const approved = list.filter(r => r.status === 'APPROVED');
    const rented   = list.filter(r => r.status === 'RENTED');
    const returned = list.filter(r => r.status === 'RETURNED');
    const other    = list.filter(r => !['PENDING','APPROVED','RENTED','RETURNED'].includes(r.status));

    const section = (title, items) => items.length
        ? `<h3 class="request-group-title">${title} (${items.length})</h3>${items.map(r => renderRequestCard(r, true)).join('')}`
        : '';

    container.innerHTML =
        section('Pending — awaiting decision', pending) +
        section('Approved — ready to be handed out', approved) +
        section('Rented — currently with user', rented) +
        section('Returned — awaiting assessment', returned) +
        section('Other', other);
}

function quickFilterMatches(req, name) {
    if (name === 'needs-action') return req.status === 'PENDING';
    if (name === 'ready-pickup') return req.status === 'APPROVED';
    if (name === 'overdue') return req.overdue;
    if (name === 'awaiting-assessment') return req.status === 'RETURNED';
    if (name === 'exam') return req.isForExam;
    if (name === 'high-priority') return (req.priorityScore || 0) >= 75;
    return true;
}

function isNeedsAction(req) {
    return ['PENDING', 'APPROVED', 'RETURNED'].includes(req.status) || req.overdue;
}

function syncManageQuickFiltersWithControls(status, action) {
    if (status === 'PENDING') {
        removeQuickFilters('manageQuickFilters', 'manage_quick_filters', ['ready-pickup', 'overdue', 'awaiting-assessment']);
    } else if (status === 'APPROVED') {
        removeQuickFilters('manageQuickFilters', 'manage_quick_filters', ['needs-action', 'overdue', 'awaiting-assessment']);
    } else if (status === 'RETURNED') {
        removeQuickFilters('manageQuickFilters', 'manage_quick_filters', ['needs-action', 'ready-pickup', 'overdue']);
    } else if (status === 'RENTED') {
        removeQuickFilters('manageQuickFilters', 'manage_quick_filters', ['needs-action', 'ready-pickup', 'awaiting-assessment']);
    } else if (status) {
        removeQuickFilters('manageQuickFilters', 'manage_quick_filters', ['needs-action', 'ready-pickup', 'overdue', 'awaiting-assessment']);
    }
    if (action === 'overdue') {
        removeQuickFilters('manageQuickFilters', 'manage_quick_filters', ['needs-action', 'ready-pickup', 'awaiting-assessment']);
    }
}

function sortManageRequests(list, sort) {
    const isFifo = state.activeStrategy === 'fifo';
    const strategyCompare = (a, b) => {
        if (isFifo) {
            return new Date(a.createdAt) - new Date(b.createdAt);
        }
        const pa = a.priorityScore ?? 0, pb = b.priorityScore ?? 0;
        if (pa !== pb) return pb - pa;
        return new Date(a.createdAt) - new Date(b.createdAt);
    };

    list.sort((a, b) => {
        if (sort === 'oldest') return new Date(a.createdAt) - new Date(b.createdAt);
        if (sort === 'newest') return new Date(b.createdAt) - new Date(a.createdAt);
        if (sort === 'due-date') return new Date(a.endDate) - new Date(b.endDate);
        if (sort === 'overdue') {
            if ((a.overdue ? 1 : 0) !== (b.overdue ? 1 : 0)) return a.overdue ? -1 : 1;
            if ((a.daysOverdue || 0) !== (b.daysOverdue || 0)) return (b.daysOverdue || 0) - (a.daysOverdue || 0);
            return strategyCompare(a, b);
        }
        if ((a.overdue ? 1 : 0) !== (b.overdue ? 1 : 0)) return a.overdue ? -1 : 1;
        return strategyCompare(a, b);
    });
}

function renderManageFilterState() {
    document.querySelectorAll('[data-manage-filter]').forEach(btn => {
        btn.classList.toggle('active', isManageQuickFilterActive(btn.dataset.manageFilter));
    });
    ['mgr-search', 'mgr-status-filter', 'mgr-user-type-filter', 'mgr-category-filter',
        'mgr-priority-filter', 'mgr-action-filter', 'mgr-start-from', 'mgr-start-to'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.classList.toggle('filter-active', !!el.value);
    });
    const sort = document.getElementById('mgr-sort');
    if (sort) sort.classList.toggle('filter-active', sort.value !== 'priority');
}

function renderManageSummary(list) {
    const counts = {
        total: list.length,
        pending: list.filter(r => r.status === 'PENDING').length,
        approved: list.filter(r => r.status === 'APPROVED').length,
        rented: list.filter(r => r.status === 'RENTED').length,
        overdue: list.filter(r => r.overdue).length,
        returned: list.filter(r => r.status === 'RETURNED').length
    };
    const el = document.getElementById('manage-summary');
    if (!el) return;
    const activeFilters = getActiveManageFilters();
    el.innerHTML = `
        <div class="summary-counts">
            <span><strong>${counts.total}</strong> shown</span>
            <span>${counts.pending} pending</span>
            <span>${counts.approved} ready</span>
            <span>${counts.rented} rented</span>
            <span class="${counts.overdue ? 'summary-danger' : ''}">${counts.overdue} overdue</span>
            <span>${counts.returned} awaiting assessment</span>
        </div>
        <div class="active-filter-list">
            ${activeFilters.length
                ? activeFilters.map(f => `<button class="active-filter-pill" onclick="removeManageFilter('${f.id}')">${f.label} x</button>`).join('')
                : '<span class="no-active-filters">No active filters</span>'}
        </div>
    `;
}

function getActiveManageFilters() {
    const filters = [];
    const value = id => document.getElementById(id)?.value || '';
    const push = (id, label) => filters.push({ id, label });
    const search = value('mgr-search');
    if (search) push('mgr-search', `Search: ${search}`);
    if (value('mgr-status-filter')) push('mgr-status-filter', `Status: ${labelForValue(value('mgr-status-filter'))}`);
    if (value('mgr-user-type-filter')) push('mgr-user-type-filter', `User type: ${labelForValue(value('mgr-user-type-filter'))}`);
    if (value('mgr-category-filter')) push('mgr-category-filter', `Category: ${value('mgr-category-filter')}`);
    if (value('mgr-priority-filter')) push('mgr-priority-filter', `Priority: ${labelForValue(value('mgr-priority-filter'))}`);
    if (value('mgr-action-filter')) push('mgr-action-filter', `Work: ${labelForValue(value('mgr-action-filter'))}`);
    if (value('mgr-start-from')) push('mgr-start-from', `Start from: ${value('mgr-start-from')}`);
    if (value('mgr-start-to')) push('mgr-start-to', `Start to: ${value('mgr-start-to')}`);
    if (value('mgr-sort') && value('mgr-sort') !== 'priority') push('mgr-sort', `Sort: ${labelForValue(value('mgr-sort'))}`);
    state.manageQuickFilters.forEach(name => {
        if (!isShortcutRepresentedByControl(name)) {
            push(`quick:${name}`, `Quick: ${quickFilterLabel(name)}`);
        }
    });
    return filters;
}

function isShortcutRepresentedByControl(name) {
    const status = document.getElementById('mgr-status-filter')?.value || '';
    const action = document.getElementById('mgr-action-filter')?.value || '';
    const priority = document.getElementById('mgr-priority-filter')?.value || '';
    if (name === 'needs-action') return status === 'PENDING';
    if (name === 'ready-pickup') return status === 'APPROVED';
    if (name === 'awaiting-assessment') return status === 'RETURNED';
    if (name === 'overdue') return action === 'overdue';
    if (name === 'exam') return action === 'exam';
    if (name === 'high-priority') return priority === 'high';
    return false;
}

function removeManageFilter(id) {
    if (id.startsWith('quick:')) {
        removeManageQuickFilter(id.substring(6));
    } else {
        const el = document.getElementById(id);
        if (el) el.value = id === 'mgr-sort' ? 'priority' : '';
        if (id === 'mgr-status-filter') {
            ['needs-action', 'ready-pickup', 'awaiting-assessment'].forEach(removeManageQuickFilter);
        }
        if (id === 'mgr-action-filter') {
            ['overdue', 'exam'].forEach(removeManageQuickFilter);
        }
        if (id === 'mgr-priority-filter') {
            removeManageQuickFilter('high-priority');
        }
    }
    applyManageFilters();
}

function labelForValue(value) {
    return String(value).toLowerCase().replace(/-/g, ' ').replace(/_/g, ' ')
        .replace(/\b\w/g, ch => ch.toUpperCase());
}

function quickFilterLabel(name) {
    const labels = {
        'needs-action': 'Needs decision',
        'ready-pickup': 'Ready for pickup',
        'overdue': 'Overdue',
        'awaiting-assessment': 'Awaiting assessment',
        'exam': 'Exam requests',
        'high-priority': 'High priority'
    };
    return labels[name] || labelForValue(name);
}

async function approveRequest(id) {
    if (!confirm('Approve this request and reserve one equipment unit?')) return;
    try {
        await api('/rental-requests/' + id + '/approve', { method: 'PUT' });
        toast('Request approved', 'success');
        loadPendingRequests();
    } catch (e) {
        toast(e.message, 'error');
    }
}

async function rejectRequest(id) {
    if (!confirm('Reject this request? The user will see it as rejected.')) return;
    try {
        await api('/rental-requests/' + id + '/reject', { method: 'PUT' });
        toast('Request rejected', 'info');
        loadPendingRequests();
    } catch (e) {
        toast(e.message, 'error');
    }
}

async function markRented(id) {
    if (!confirm('Mark this approved request as rented out?')) return;
    try {
        await api('/rental-requests/' + id + '/rent', { method: 'PUT' });
        toast('Marked as rented', 'success');
        reloadRequestsView();
    } catch (e) {
        toast(e.message, 'error');
    }
}

async function markReturned(id) {
    if (!confirm('Mark this rental as returned and ready for assessment?')) return;
    try {
        await api('/rental-requests/' + id + '/return', { method: 'PUT' });
        toast('Marked as returned', 'success');
        reloadRequestsView();
    } catch (e) {
        toast(e.message, 'error');
    }
}

function reloadRequestsView() {
    const pendingSection = document.getElementById('section-pending-requests');
    if (pendingSection && pendingSection.classList.contains('active')) {
        loadPendingRequests();
    } else {
        loadMyRequests();
    }
}

// ===== ALL USERS (Admin) =====
async function loadAllUsers() {
    try {
        const list = await api('/users');
        state.usersCache = list;
        applyUsersFilters();
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

function applyUsersFilters() {
    const search = (document.getElementById('usr-search')?.value || '').toLowerCase();
    const typeFilter = document.getElementById('usr-type-filter')?.value || '';
    const roleFilter = document.getElementById('usr-role-filter')?.value || '';
    const reputationFilter = document.getElementById('usr-reputation-filter')?.value || '';
    syncUsersQuickFiltersWithControls(typeFilter, roleFilter, reputationFilter);

    let list = [...state.usersCache];
    if (search) list = list.filter(u =>
        u.username.toLowerCase().includes(search) ||
        u.email.toLowerCase().includes(search));
    if (typeFilter) list = list.filter(u => u.userType === typeFilter);
    if (roleFilter) list = list.filter(u => u.role === roleFilter);
    if (reputationFilter === 'top') list = list.filter(u => u.reputationScore >= 130);
    if (reputationFilter === 'standard') list = list.filter(u => u.reputationScore >= 70 && u.reputationScore < 130);
    if (reputationFilter === 'risk') list = list.filter(u => u.reputationScore < 70);
    if (state.usersQuickFilters.length > 0) {
        list = list.filter(u => state.usersQuickFilters.every(name => usersQuickFilterMatches(u, name)));
    }

    const key = state.usersSortKey;
    const dir = state.usersSortDir === 'asc' ? 1 : -1;
    list.sort((a, b) => {
        const av = a[key], bv = b[key];
        if (typeof av === 'number' && typeof bv === 'number') return (av - bv) * dir;
        return String(av).localeCompare(String(bv)) * dir;
    });
    renderUsersFilterState(list.length);

    const sortCls = c => {
        if (c !== key) return 'sortable';
        return 'sortable ' + (state.usersSortDir === 'asc' ? 'sort-asc' : 'sort-desc');
    };

    document.getElementById('users-table-container').innerHTML = `
        <table>
            <thead>
                <tr>
                    <th class="${sortCls('id')}" onclick="sortUsers('id')">ID</th>
                    <th class="${sortCls('username')}" onclick="sortUsers('username')">Username</th>
                    <th class="${sortCls('email')}" onclick="sortUsers('email')">Email</th>
                    <th class="${sortCls('role')}" onclick="sortUsers('role')">Role</th>
                    <th class="${sortCls('userType')}" onclick="sortUsers('userType')">Type</th>
                    <th class="${sortCls('reputationScore')}" onclick="sortUsers('reputationScore')">Reputation</th>
                    <th class="${sortCls('createdAt')}" onclick="sortUsers('createdAt')">Created</th>
                </tr>
            </thead>
            <tbody>
                ${list.map(u => `
                    <tr>
                        <td>${u.id}</td>
                        <td><strong>${u.username}</strong></td>
                        <td>${u.email}</td>
                        <td><span class="badge badge-${u.role === 'ADMIN' ? 'approved' : 'rented'}">${u.role}</span></td>
                        <td>${u.userType}</td>
                        <td>${u.reputationScore.toFixed(1)}</td>
                        <td>${formatDate(u.createdAt)}</td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

function sortUsers(key) {
    if (state.usersSortKey === key) {
        state.usersSortDir = state.usersSortDir === 'asc' ? 'desc' : 'asc';
    } else {
        state.usersSortKey = key;
        state.usersSortDir = 'asc';
    }
    applyUsersFilters();
}

// ===== ASSESSMENTS (Admin) =====
async function loadAssessments() {
    try {
        const allRequests = await api('/rental-requests/all');
        const returned = allRequests
            .filter(r => r.status === 'RETURNED')
            .sort((a, b) => {
                const lateA = returnedLateDays(a), lateB = returnedLateDays(b);
                if (lateA !== lateB) return lateB - lateA;
                return new Date(a.returnedAt || a.endDate) - new Date(b.returnedAt || b.endDate);
            });
        state.assessmentQueueCache = returned;
        const users = await api('/users');

        const options = returned.length
            ? returned.map(r => `<option value="${r.id}">#${r.id} - ${r.username} - ${r.equipmentName} (${returnedLateDays(r)} day(s) late)</option>`).join('')
            : '<option value="" disabled>No returned requests awaiting assessment</option>';

        let html = '<h3 style="margin-bottom:12px">Submit New Assessment</h3>';
        html += `
            <div class="card" style="max-width:600px; margin-bottom:24px">
                <div class="form-group">
                    <label>Returned Rental Request</label>
                    <select id="assess-req-id" onchange="updateAssessmentPreview()">${options}</select>
                </div>
                <div id="assessment-request-preview" class="assessment-preview"></div>
                <div class="form-group">
                    <label>Condition Rating</label>
                    <select id="assess-rating" onchange="updateAssessmentPreview()">
                        <option value="EXCELLENT">Excellent (+5.0 reputation)</option>
                        <option value="GOOD">Good (+2.0 reputation)</option>
                        <option value="FAIR">Fair (0 reputation)</option>
                        <option value="POOR">Poor (-5.0 reputation)</option>
                        <option value="DAMAGED">Damaged (-15.0 reputation)</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Notes</label>
                    <textarea id="assess-notes" placeholder="Observations about the equipment condition..."></textarea>
                </div>
                <div id="assessment-impact-preview" class="impact-preview"></div>
                <button class="btn btn-success" ${returned.length === 0 ? 'disabled' : ''} onclick="submitAssessment()">Submit Assessment</button>
            </div>
        `;

        html += `<h3 style="margin-bottom:12px">Awaiting Assessment (<span id="assessment-queue-count">${returned.length}</span>)</h3>`;
        html += '<div id="assessment-queue-list"></div>';

        const requestById = new Map(allRequests.map(r => [r.id, r]));
        const history = [];
        for (const u of users) {
            try {
                const assessments = await api('/return-assessments/user/' + u.id);
                assessments.forEach(a => history.push({
                    ...a,
                    username: u.username,
                    userType: u.userType,
                    userReputationScore: u.reputationScore,
                    request: requestById.get(a.rentalRequestId) || null
                }));
            } catch (e) { /* skip */ }
        }
        state.assessmentHistoryCache = history;

        html += '<h3 style="margin-bottom:12px">Assessment History</h3>';
        html += '<div id="assessment-history-list"></div>';

        document.getElementById('assessments-list').innerHTML = html;
        window.assessmentRequestsCache = returned;
        updateAssessmentPreview();
        applyAssessmentFilters();
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

function renderAssessmentQueueCard(req) {
    const late = returnedLateDays(req);
    return `<div class="card compact-card assessment-card ${late > 0 ? 'overdue' : ''}">
        <div class="card-split-header">
            <div>
                <h3>${req.equipmentName}</h3>
                <p><strong>${req.username}</strong> (${req.userType || 'N/A'}, reputation ${req.userReputationScore?.toFixed(1) || 'N/A'})</p>
            </div>
            <span class="badge ${late > 0 ? 'badge-overdue' : 'badge-returned'}">${late > 0 ? `${late} day(s) late` : 'ON TIME'}</span>
        </div>
        <p>Due ${req.endDate}; returned ${req.returnedAt || 'N/A'}</p>
    </div>`;
}

function applyAssessmentFilters() {
    const search = (document.getElementById('assess-search')?.value || '').toLowerCase();
    const rating = document.getElementById('assess-rating-filter')?.value || '';
    const impact = document.getElementById('assess-impact-filter')?.value || '';
    const late = document.getElementById('assess-late-filter')?.value || '';
    const sort = document.getElementById('assess-sort')?.value || 'newest';
    syncAssessmentQuickFiltersWithControls(rating, impact, late);

    let queue = [...state.assessmentQueueCache];
    let list = [...state.assessmentHistoryCache];
    if (search) queue = queue.filter(r =>
        r.username.toLowerCase().includes(search) ||
        (r.equipmentName || '').toLowerCase().includes(search) ||
        (r.equipmentCategory || '').toLowerCase().includes(search) ||
        String(r.id).includes(search));
    if (search) list = list.filter(a =>
        a.username.toLowerCase().includes(search) ||
        (a.request?.equipmentName || '').toLowerCase().includes(search) ||
        (a.request?.equipmentCategory || '').toLowerCase().includes(search) ||
        (a.notes || '').toLowerCase().includes(search) ||
        String(a.rentalRequestId).includes(search));
    if (rating || impact) queue = [];
    if (rating) list = list.filter(a => a.conditionRating === rating);
    if (impact === 'positive') list = list.filter(a => (a.reputationImpact || 0) > 0);
    if (impact === 'neutral') list = list.filter(a => (a.reputationImpact || 0) === 0);
    if (impact === 'negative') list = list.filter(a => (a.reputationImpact || 0) < 0);
    if (late === 'late') queue = queue.filter(r => returnedLateDays(r) > 0);
    if (late === 'on-time') queue = queue.filter(r => returnedLateDays(r) === 0);
    if (late === 'late') list = list.filter(a => a.request && returnedLateDays(a.request) > 0);
    if (late === 'on-time') list = list.filter(a => !a.request || returnedLateDays(a.request) === 0);
    if (state.assessmentQuickFilters.length > 0) {
        queue = queue.filter(r => state.assessmentQuickFilters.every(name => assessmentQueueQuickFilterMatches(r, name)));
        list = list.filter(a => state.assessmentQuickFilters.every(name => assessmentHistoryQuickFilterMatches(a, name)));
    }

    list.sort((a, b) => {
        if (sort === 'impact-low') return (a.reputationImpact || 0) - (b.reputationImpact || 0);
        if (sort === 'impact-high') return (b.reputationImpact || 0) - (a.reputationImpact || 0);
        return new Date(b.assessedAt || 0) - new Date(a.assessedAt || 0);
    });

    renderAssessmentQueue(queue);
    renderAssessmentFilterState(queue.length, list.length);
    const container = document.getElementById('assessment-history-list');
    if (!container) return;
    if (list.length === 0) {
        container.innerHTML = emptyState('No assessments match your filters', 'Clear filters or choose a different condition/impact.');
        return;
    }

    container.innerHTML = list.map(renderAssessmentHistoryCard).join('');
}

function renderAssessmentQueue(queue) {
    const count = document.getElementById('assessment-queue-count');
    if (count) count.textContent = queue.length;
    const container = document.getElementById('assessment-queue-list');
    if (!container) return;
    if (queue.length === 0) {
        const isFiltered = isAssessmentFiltered();
        container.innerHTML = emptyState(
            isFiltered ? 'No returned requests match your filters' : 'No requests awaiting assessment',
            isFiltered ? 'Clear filters to see the full assessment queue.' : 'Returned rentals will appear here before final assessment.'
        );
        return;
    }
    container.innerHTML = queue.map(r => renderAssessmentQueueCard(r)).join('');
}

function renderAssessmentHistoryCard(a) {
    const ratingBadge = a.conditionRating === 'EXCELLENT' || a.conditionRating === 'GOOD' ? 'approved'
        : a.conditionRating === 'FAIR' ? 'pending' : 'rejected';
    const impactCls = a.reputationImpact > 0 ? 'rep-positive' : a.reputationImpact < 0 ? 'rep-negative' : 'rep-neutral';
    const sign = a.reputationImpact > 0 ? '+' : '';
    const late = a.request ? returnedLateDays(a.request) : 0;
    return `<div class="card compact-card assessment-card">
        <div class="card-split-header">
            <div>
                <h3>${a.username} - Request #${a.rentalRequestId}</h3>
                <p>${a.request?.equipmentName || 'Equipment unavailable'} ${a.request?.equipmentCategory ? `(${a.request.equipmentCategory})` : ''}</p>
            </div>
            <span class="badge badge-${ratingBadge}">${a.conditionRating}</span>
        </div>
        <p>Impact: <span class="${impactCls}">${sign}${a.reputationImpact.toFixed(1)}</span>
            ${late > 0 ? `<span class="badge badge-overdue">${late} day(s) late</span>` : '<span class="badge badge-returned">ON TIME</span>'}</p>
        <p>${a.notes || 'No notes'}</p>
        <p class="meta">Assessed: ${formatDate(a.assessedAt)} by ${a.operatorUsername}</p>
    </div>`;
}

function renderAssessmentFilterState(queueCount, historyCount) {
    renderQuickFilterButtons('[data-assessment-filter]', 'assessmentQuickFilters', 'assessmentFilter', isAssessmentQuickFilterActive);
    setFilterActiveState(['assess-search', 'assess-rating-filter', 'assess-impact-filter', 'assess-late-filter', 'assess-sort'], {
        'assess-sort': 'newest'
    });
    const filters = [];
    const search = document.getElementById('assess-search')?.value || '';
    const rating = document.getElementById('assess-rating-filter')?.value || '';
    const impact = document.getElementById('assess-impact-filter')?.value || '';
    const late = document.getElementById('assess-late-filter')?.value || '';
    const sort = document.getElementById('assess-sort')?.value || 'newest';
    if (search) filters.push({ id: 'assess-search', label: `Search: ${search}` });
    if (rating) filters.push({ id: 'assess-rating-filter', label: `Rating: ${labelForValue(rating)}` });
    if (impact) filters.push({ id: 'assess-impact-filter', label: `Impact: ${labelForValue(impact)}` });
    if (late) filters.push({ id: 'assess-late-filter', label: `Return: ${labelForValue(late)}` });
    if (sort !== 'newest') filters.push({ id: 'assess-sort', label: `Sort: ${labelForValue(sort)}` });
    state.assessmentQuickFilters.forEach(name => {
        filters.push({ id: `quick:${name}`, label: `Quick: ${assessmentQuickFilterLabel(name)}` });
    });
    renderFilterSummary(
        'assess-summary',
        `<strong>${queueCount}</strong> queue / <strong>${historyCount}</strong> history shown`,
        filters,
        'removeAssessmentFilter'
    );
}

function setAssessmentQuickFilter(name) {
    if (isAssessmentQuickFilterActive(name)) {
        removeQuickFilter('assessmentQuickFilters', 'assessment_quick_filters', name);
        clearAssessmentControlForQuick(name);
        applyAssessmentFilters();
        return;
    }
    clearAssessmentControlConflicts(name);
    toggleQuickFilter('assessmentQuickFilters', 'assessment_quick_filters', name, applyAssessmentFilters, quickFilterConflicts.assessment);
}

function isAssessmentQuickFilterActive(name) {
    const rating = document.getElementById('assess-rating-filter')?.value || '';
    const impact = document.getElementById('assess-impact-filter')?.value || '';
    const late = document.getElementById('assess-late-filter')?.value || '';
    if ((state.assessmentQuickFilters || []).includes(name)) return true;
    if (name === 'late') return late === 'late';
    if (name === 'on-time') return late === 'on-time';
    if (name === 'positive-impact') return impact === 'positive';
    if (name === 'negative-impact') return impact === 'negative';
    if (name === 'damage-issues') return ['POOR', 'DAMAGED'].includes(rating);
    return false;
}

function clearAssessmentControlForQuick(name) {
    const rating = document.getElementById('assess-rating-filter');
    const impact = document.getElementById('assess-impact-filter');
    const late = document.getElementById('assess-late-filter');
    if (name === 'late' && late?.value === 'late') late.value = '';
    if (name === 'on-time' && late?.value === 'on-time') late.value = '';
    if (name === 'positive-impact' && impact?.value === 'positive') impact.value = '';
    if (name === 'negative-impact' && impact?.value === 'negative') impact.value = '';
    if (name === 'damage-issues' && ['POOR', 'DAMAGED'].includes(rating?.value)) rating.value = '';
}

function clearAssessmentControlConflicts(name) {
    (quickFilterConflicts.assessment[name] || []).forEach(clearAssessmentControlForQuick);
    const rating = document.getElementById('assess-rating-filter');
    const impact = document.getElementById('assess-impact-filter');
    if (name === 'needs-assessment') {
        if (rating) rating.value = '';
        if (impact) impact.value = '';
    }
    if (name === 'positive-impact') {
        if (['FAIR', 'POOR', 'DAMAGED'].includes(rating?.value)) rating.value = '';
        if (['negative', 'neutral'].includes(impact?.value)) impact.value = '';
    }
    if (name === 'negative-impact') {
        if (['EXCELLENT', 'GOOD', 'FAIR'].includes(rating?.value)) rating.value = '';
        if (['positive', 'neutral'].includes(impact?.value)) impact.value = '';
    }
    if (name === 'damage-issues') {
        if (['EXCELLENT', 'GOOD', 'FAIR'].includes(rating?.value)) rating.value = '';
        if (['positive', 'neutral'].includes(impact?.value)) impact.value = '';
    }
}

function syncAssessmentQuickFiltersWithControls(rating, impact, late) {
    if (rating) {
        removeQuickFilters('assessmentQuickFilters', 'assessment_quick_filters', ['needs-assessment']);
    }
    if (['EXCELLENT', 'GOOD'].includes(rating)) {
        removeQuickFilters('assessmentQuickFilters', 'assessment_quick_filters', ['negative-impact', 'damage-issues']);
    } else if (rating === 'FAIR') {
        removeQuickFilters('assessmentQuickFilters', 'assessment_quick_filters', ['positive-impact', 'negative-impact', 'damage-issues']);
    } else if (['POOR', 'DAMAGED'].includes(rating)) {
        removeQuickFilters('assessmentQuickFilters', 'assessment_quick_filters', ['positive-impact']);
    }
    if (impact === 'positive') {
        removeQuickFilters('assessmentQuickFilters', 'assessment_quick_filters', ['needs-assessment', 'negative-impact', 'damage-issues']);
    } else if (impact === 'negative') {
        removeQuickFilters('assessmentQuickFilters', 'assessment_quick_filters', ['needs-assessment', 'positive-impact']);
    } else if (impact === 'neutral') {
        removeQuickFilters('assessmentQuickFilters', 'assessment_quick_filters', ['needs-assessment', 'positive-impact', 'negative-impact', 'damage-issues']);
    }
    if (late === 'late') {
        removeQuickFilters('assessmentQuickFilters', 'assessment_quick_filters', ['on-time']);
    } else if (late === 'on-time') {
        removeQuickFilters('assessmentQuickFilters', 'assessment_quick_filters', ['late']);
    }
}

function assessmentQueueQuickFilterMatches(req, name) {
    if (name === 'needs-assessment') return true;
    if (name === 'late') return returnedLateDays(req) > 0;
    if (name === 'on-time') return returnedLateDays(req) === 0;
    if (name === 'positive-impact') return false;
    if (name === 'negative-impact') return false;
    if (name === 'damage-issues') return false;
    return true;
}

function assessmentHistoryQuickFilterMatches(assessment, name) {
    const late = assessment.request ? returnedLateDays(assessment.request) : 0;
    if (name === 'needs-assessment') return false;
    if (name === 'late') return late > 0;
    if (name === 'on-time') return late === 0;
    if (name === 'positive-impact') return (assessment.reputationImpact || 0) > 0;
    if (name === 'negative-impact') return (assessment.reputationImpact || 0) < 0;
    if (name === 'damage-issues') return ['POOR', 'DAMAGED'].includes(assessment.conditionRating);
    return true;
}

function assessmentQuickFilterLabel(name) {
    const labels = {
        'needs-assessment': 'Needs assessment',
        'late': 'Late returns',
        'on-time': 'On-time returns',
        'positive-impact': 'Positive impact',
        'negative-impact': 'Negative impact',
        'damage-issues': 'Damage issues'
    };
    return labels[name] || labelForValue(name);
}

function isAssessmentFiltered() {
    return !!(
        document.getElementById('assess-search')?.value ||
        document.getElementById('assess-rating-filter')?.value ||
        document.getElementById('assess-impact-filter')?.value ||
        document.getElementById('assess-late-filter')?.value ||
        (document.getElementById('assess-sort')?.value || 'newest') !== 'newest' ||
        state.assessmentQuickFilters.length
    );
}

function removeAssessmentFilter(id) {
    if (id.startsWith('quick:')) {
        removeQuickFilter('assessmentQuickFilters', 'assessment_quick_filters', id.substring(6));
        applyAssessmentFilters();
        return;
    }
    const el = document.getElementById(id);
    if (!el) return;
    el.value = id === 'assess-sort' ? 'newest' : '';
    applyAssessmentFilters();
}

function resetAssessmentFilters() {
    clearQuickFilters('assessmentQuickFilters', 'assessment_quick_filters');
    ['assess-search', 'assess-rating-filter', 'assess-impact-filter', 'assess-late-filter'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });
    const sort = document.getElementById('assess-sort');
    if (sort) sort.value = 'newest';
    applyAssessmentFilters();
}

function setFilterActiveState(ids, defaults = {}) {
    ids.forEach(id => {
        const el = document.getElementById(id);
        if (!el) return;
        const defaultValue = defaults[id] ?? '';
        el.classList.toggle('filter-active', el.value !== defaultValue);
    });
}

function renderFilterSummary(containerId, countLabel, filters, removeFnName) {
    const el = document.getElementById(containerId);
    if (!el) return;
    el.innerHTML = `
        <div class="summary-counts">
            <span>${countLabel}</span>
        </div>
        <div class="active-filter-list">
            ${filters.length
                ? filters.map(f => `<button class="active-filter-pill" onclick="${removeFnName}('${f.id}')">${escapeHtml(f.label)} x</button>`).join('')
                : '<span class="no-active-filters">No active filters</span>'}
        </div>
    `;
}

function renderUsersFilterState(count) {
    renderQuickFilterButtons('[data-users-filter]', 'usersQuickFilters', 'usersFilter', isUsersQuickFilterActive);
    setFilterActiveState(['usr-search', 'usr-type-filter', 'usr-role-filter', 'usr-reputation-filter']);
    const filters = [];
    const search = document.getElementById('usr-search')?.value || '';
    const type = document.getElementById('usr-type-filter')?.value || '';
    const role = document.getElementById('usr-role-filter')?.value || '';
    const reputation = document.getElementById('usr-reputation-filter')?.value || '';
    if (search) filters.push({ id: 'usr-search', label: `Search: ${search}` });
    if (type) filters.push({ id: 'usr-type-filter', label: `Type: ${labelForValue(type)}` });
    if (role) filters.push({ id: 'usr-role-filter', label: `Role: ${labelForValue(role)}` });
    if (reputation) filters.push({ id: 'usr-reputation-filter', label: `Reputation: ${labelForValue(reputation)}` });
    if (state.usersSortKey !== 'id' || state.usersSortDir !== 'asc') {
        filters.push({ id: 'usr-sort', label: `Sort: ${labelForValue(state.usersSortKey)} ${state.usersSortDir}` });
    }
    state.usersQuickFilters.forEach(name => {
        filters.push({ id: `quick:${name}`, label: `Quick: ${usersQuickFilterLabel(name)}` });
    });
    renderFilterSummary('users-summary', `<strong>${count}</strong> users shown`, filters, 'removeUsersFilter');
}

function setUsersQuickFilter(name) {
    if (isUsersQuickFilterActive(name)) {
        removeQuickFilter('usersQuickFilters', 'users_quick_filters', name);
        clearUsersControlForQuick(name);
        applyUsersFilters();
        return;
    }
    clearUsersControlConflicts(name);
    toggleQuickFilter('usersQuickFilters', 'users_quick_filters', name, applyUsersFilters, quickFilterConflicts.users);
}

function isUsersQuickFilterActive(name) {
    const type = document.getElementById('usr-type-filter')?.value || '';
    const role = document.getElementById('usr-role-filter')?.value || '';
    const reputation = document.getElementById('usr-reputation-filter')?.value || '';
    if ((state.usersQuickFilters || []).includes(name)) return true;
    if (name === 'students') return type === 'STUDENT';
    if (name === 'non-students') return type === 'NON_STUDENT';
    if (name === 'admins') return role === 'ADMIN';
    if (name === 'reputation-risk') return reputation === 'risk';
    if (name === 'top-reputation') return reputation === 'top';
    return false;
}

function clearUsersControlForQuick(name) {
    const type = document.getElementById('usr-type-filter');
    const role = document.getElementById('usr-role-filter');
    const reputation = document.getElementById('usr-reputation-filter');
    if (name === 'students' && type?.value === 'STUDENT') type.value = '';
    if (name === 'non-students' && type?.value === 'NON_STUDENT') type.value = '';
    if (name === 'admins' && role?.value === 'ADMIN') role.value = '';
    if (name === 'reputation-risk' && reputation?.value === 'risk') reputation.value = '';
    if (name === 'top-reputation' && reputation?.value === 'top') reputation.value = '';
}

function clearUsersControlConflicts(name) {
    (quickFilterConflicts.users[name] || []).forEach(clearUsersControlForQuick);
}

function syncUsersQuickFiltersWithControls(type, role, reputation) {
    if (type === 'STUDENT') {
        removeQuickFilters('usersQuickFilters', 'users_quick_filters', ['non-students']);
    } else if (type === 'NON_STUDENT') {
        removeQuickFilters('usersQuickFilters', 'users_quick_filters', ['students']);
    }
    if (role === 'USER') {
        removeQuickFilters('usersQuickFilters', 'users_quick_filters', ['admins']);
    }
    if (reputation === 'top') {
        removeQuickFilters('usersQuickFilters', 'users_quick_filters', ['reputation-risk']);
    } else if (reputation === 'risk') {
        removeQuickFilters('usersQuickFilters', 'users_quick_filters', ['top-reputation']);
    } else if (reputation === 'standard') {
        removeQuickFilters('usersQuickFilters', 'users_quick_filters', ['top-reputation', 'reputation-risk']);
    }
}

function usersQuickFilterMatches(user, name) {
    if (name === 'students') return user.userType === 'STUDENT';
    if (name === 'non-students') return user.userType === 'NON_STUDENT';
    if (name === 'admins') return user.role === 'ADMIN';
    if (name === 'reputation-risk') return (user.reputationScore || 0) < 70;
    if (name === 'top-reputation') return (user.reputationScore || 0) >= 130;
    return true;
}

function usersQuickFilterLabel(name) {
    const labels = {
        'students': 'Students',
        'non-students': 'Non-students',
        'admins': 'Admins',
        'reputation-risk': 'Reputation risk',
        'top-reputation': 'Top reputation'
    };
    return labels[name] || labelForValue(name);
}

function removeUsersFilter(id) {
    if (id === 'usr-sort') {
        state.usersSortKey = 'id';
        state.usersSortDir = 'asc';
    } else if (id.startsWith('quick:')) {
        removeQuickFilter('usersQuickFilters', 'users_quick_filters', id.substring(6));
    } else {
        const el = document.getElementById(id);
        if (el) el.value = '';
    }
    applyUsersFilters();
}

function resetUsersFilters() {
    clearQuickFilters('usersQuickFilters', 'users_quick_filters');
    ['usr-search', 'usr-type-filter', 'usr-role-filter', 'usr-reputation-filter'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });
    state.usersSortKey = 'id';
    state.usersSortDir = 'asc';
    applyUsersFilters();
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function updateAssessmentPreview() {
    const select = document.getElementById('assess-req-id');
    const ratingEl = document.getElementById('assess-rating');
    const impactEl = document.getElementById('assessment-impact-preview');
    const requestEl = document.getElementById('assessment-request-preview');
    if (!select || !ratingEl || !impactEl || !requestEl || !select.value) return;

    const req = (window.assessmentRequestsCache || []).find(r => String(r.id) === select.value);
    if (!req) return;
    const ratingImpact = conditionImpact(ratingEl.value);
    const latePenalty = -Math.min(10, returnedLateDays(req));
    const total = ratingImpact + latePenalty;
    const cls = total > 0 ? 'rep-positive' : total < 0 ? 'rep-negative' : 'rep-neutral';
    const sign = n => n > 0 ? '+' + n.toFixed(1) : n.toFixed(1);

    requestEl.innerHTML = `
        <strong>${req.equipmentName}</strong><br>
        ${req.username} - due ${req.endDate}, returned ${req.returnedAt || 'N/A'}
    `;
    impactEl.innerHTML = `
        <span>Rating: ${sign(ratingImpact)}</span>
        <span>Late penalty: ${sign(latePenalty)}</span>
        <strong class="${cls}">Total impact: ${sign(total)}</strong>
    `;
}

function conditionImpact(rating) {
    const impacts = { EXCELLENT: 5, GOOD: 2, FAIR: 0, POOR: -5, DAMAGED: -15 };
    return impacts[rating] ?? 0;
}

function returnedLateDays(req) {
    if (!req.returnedAt || !req.endDate) return 0;
    const due = new Date(req.endDate + 'T00:00:00');
    const returned = new Date(req.returnedAt + 'T00:00:00');
    return Math.max(0, Math.round((returned - due) / 86400000));
}

async function submitAssessment() {
    try {
        const selectEl = document.getElementById('assess-req-id');
        if (!selectEl.value) {
            toast('No returned request selected', 'warning');
            return;
        }
        const result = await api('/return-assessments', {
            method: 'POST',
            body: JSON.stringify({
                rentalRequestId: parseInt(selectEl.value),
                conditionRating: document.getElementById('assess-rating').value,
                notes: document.getElementById('assess-notes').value || null
            })
        });
        toast(`Assessment submitted (impact ${result.reputationImpact > 0 ? '+' : ''}${result.reputationImpact.toFixed(1)})`, 'success');
        await loadAssessments();
        state.requestsCache = await api('/rental-requests/all');
    } catch (e) {
        toast(e.message, 'error');
    }
}

// ===== STRATEGY (Admin) =====
async function loadStrategy() {
    try {
        const data = await api('/admin/prioritization-strategy');
        state.activeStrategy = data.strategy;
        document.getElementById('current-strategy').textContent = data.strategy;
        document.getElementById('strategy-select').value = data.strategy;
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

async function changeStrategy() {
    try {
        const data = await api('/admin/prioritization-strategy', {
            method: 'PUT',
            body: JSON.stringify({ strategy: document.getElementById('strategy-select').value })
        });
        state.activeStrategy = data.strategy;
        document.getElementById('current-strategy').textContent = data.strategy;
        toast('Strategy changed to ' + data.strategy, 'success');
    } catch (e) {
        toast(e.message, 'error');
    }
}

// ===== RENDER HELPERS =====
function renderRequestCard(req, showAdminActions = false, queuePosition = null) {
    const priorityClass = priorityClassFor(req.priorityScore);

    let actions = '';
    const isAdmin = currentUser.role === 'ADMIN';

    if (isAdmin && showAdminActions && req.status === 'PENDING') {
        actions = `
            <button class="btn btn-success btn-sm" onclick="approveRequest(${req.id})">Approve</button>
            <button class="btn btn-danger btn-sm" onclick="rejectRequest(${req.id})">Reject</button>
        `;
    }
    if (isAdmin && showAdminActions && req.status === 'APPROVED') {
        actions += `<button class="btn btn-info btn-sm" onclick="markRented(${req.id})">Mark Rented</button>`;
    }
    if (isAdmin && showAdminActions && req.status === 'RENTED') {
        actions += `<button class="btn btn-warning btn-sm" onclick="markReturned(${req.id})">Mark Returned</button>`;
    }

    let timingBadge = '';
    if (req.overdue) {
        timingBadge = `<span class="badge badge-overdue">${req.daysOverdue} day(s) overdue</span>`;
    } else if (req.status === 'RENTED' && (req.daysRemaining !== null && req.daysRemaining !== undefined)) {
        timingBadge = `<span class="badge badge-rented">${req.daysRemaining} day(s) left</span>`;
    }

    const queueBadge = queuePosition
        ? `<span class="queue-position">Queue position ${queuePosition}</span>`
        : '';
    const adminMeta = showAdminActions ? `
        <div class="request-insights">
            <span>User type: <strong>${req.userType || 'N/A'}</strong></span>
            <span>Reputation: <strong>${req.userReputationScore?.toFixed(1) || 'N/A'}</strong></span>
            <span>Category: <strong>${req.equipmentCategory || 'N/A'}</strong></span>
            ${req.isForExam ? `<span>Exam urgency: <strong>${req.examDate || 'N/A'}</strong></span>` : ''}
        </div>
    ` : '';
    const priorityNote = showAdminActions ? priorityExplanation(req) : '';

    return `
        <div class="card request-card ${req.overdue ? 'overdue' : ''}">
            <div class="request-card-header">
                <div>
                    <h3>${req.equipmentName}</h3>
                    <p><strong>User:</strong> ${req.username} | <strong>Request #${req.id}</strong></p>
                </div>
                <div class="badge-row">
                    <span class="badge badge-${req.status.toLowerCase()}">${req.status}</span>
                    ${req.isForExam ? '<span class="badge badge-pending">EXAM</span>' : ''}
                    ${timingBadge}
                </div>
            </div>
            ${adminMeta}
            <p><strong>Period:</strong> ${req.startDate} to ${req.endDate}${req.returnedAt ? ` · returned ${req.returnedAt}` : ''}</p>
            ${req.projectDescription ? `<p><strong>Project:</strong> ${req.projectDescription}</p>` : ''}
            ${req.isForExam ? `<p><strong>Exam:</strong> ${req.examDate || 'N/A'} - ${req.justification || 'No justification'}</p>` : ''}
            <p>Priority: <span class="priority-score ${priorityClass}">${req.priorityScore?.toFixed(1) || 'N/A'}</span> ${queueBadge}</p>
            ${priorityNote}
            <p class="meta">Created: ${formatDate(req.createdAt)}</p>
            ${actions ? `<div class="actions">${actions}</div>` : ''}
        </div>
    `;
}

function priorityClassFor(score) {
    return (score || 0) >= 75 ? 'priority-high'
        : (score || 0) >= 50 ? 'priority-medium' : 'priority-low';
}

function priorityExplanation(req) {
    const parts = [];
    if ((req.userReputationScore || 0) >= 130) parts.push('strong reputation');
    if ((req.userReputationScore || 0) < 70) parts.push('low reputation risk');
    if (req.isForExam) parts.push('academic urgency');
    if (req.overdue) parts.push(`${req.daysOverdue} day(s) overdue`);
    if ((req.priorityScore || 0) >= 75) parts.push('high priority queue');
    if (parts.length === 0) return '<p class="meta">Priority context: standard request.</p>';
    return `<p class="meta">Priority context: ${parts.join(', ')}.</p>`;
}

// ===== UTILITIES =====
function formatDate(dateStr) {
    if (!dateStr) return 'N/A';
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
}

function formatDateInput(date) {
    return date.toISOString().split('T')[0];
}

function timeAgo(dateStr) {
    if (!dateStr) return '';
    const diffMs = Date.now() - new Date(dateStr).getTime();
    const mins = Math.floor(diffMs / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return mins + ' min ago';
    const hours = Math.floor(mins / 60);
    if (hours < 24) return hours + 'h ago';
    const days = Math.floor(hours / 24);
    if (days < 30) return days + 'd ago';
    const months = Math.floor(days / 30);
    return months + 'mo ago';
}

function showAlert(containerId, message, type) {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = `<div class="alert alert-${type}">${message}</div>`;
    setTimeout(() => { container.innerHTML = ''; }, 5000);
}

function toast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const el = document.createElement('div');
    el.className = 'toast toast-' + type;
    el.textContent = message;
    container.appendChild(el);
    setTimeout(() => el.remove(), 4200);
}

function openModal(html) {
    document.getElementById('modal-content').innerHTML = html;
    document.getElementById('modal-overlay').classList.add('active');
}

function closeModal(event) {
    if (event && event.target !== document.getElementById('modal-overlay')) return;
    document.getElementById('modal-overlay').classList.remove('active');
}

// ===== INIT =====
if (token && currentUser) {
    enterApp();
}
