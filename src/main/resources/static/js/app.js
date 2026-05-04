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
    myQueuePositions: {},
    usersSortKey: 'id',
    usersSortDir: 'asc',
    activeStrategy: 'weightedScoring',
    charts: { status: null, topUsers: null, utilization: null }
};

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

        const data = await resp.json();
        if (!resp.ok) {
            const msg = data.validationErrors
                ? Object.values(data.validationErrors).join(', ')
                : (data.message || 'Request failed');
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
        <div class="kpi-card">
            <div class="kpi-value">${stats.pendingCount}</div>
            <div class="kpi-label">Pending Requests</div>
        </div>
        <div class="kpi-card kpi-success">
            <div class="kpi-value">${stats.activeRentalsCount}</div>
            <div class="kpi-label">Active Rentals</div>
        </div>
        <div class="kpi-card ${stats.overdueCount > 0 ? 'kpi-danger' : ''}">
            <div class="kpi-value">${stats.overdueCount}</div>
            <div class="kpi-label">Overdue</div>
        </div>
        <div class="kpi-card ${pct >= 80 ? 'kpi-warning' : ''}">
            <div class="kpi-value">${pct.toFixed(1)}%</div>
            <div class="kpi-label">Utilization</div>
        </div>
    `;
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
        return `<div class="activity-item">
            <div class="activity-icon ${iconClass}">${iconLetter}</div>
            <div class="activity-body">
                <div class="activity-message">${ev.message}</div>
                <div class="activity-meta">by ${ev.actorUsername} · ref #${ev.referenceId}</div>
            </div>
            <div class="activity-time">${timeAgo(ev.timestamp)}</div>
        </div>`;
    }).join('');
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

    let filtered = state.equipmentCache;
    if (search) filtered = filtered.filter(e => e.name.toLowerCase().includes(search)
        || (e.description || '').toLowerCase().includes(search));
    if (cat) filtered = filtered.filter(e => e.category === cat);
    if (avail === 'available') filtered = filtered.filter(e => e.availableQuantity > 0);
    if (avail === 'unavailable') filtered = filtered.filter(e => e.availableQuantity === 0);

    const container = document.getElementById('equipment-list');
    if (filtered.length === 0) {
        container.innerHTML = emptyState(
            'No equipment found',
            'Try adjusting your search or filters.'
        );
        return;
    }
    container.innerHTML = filtered.map(eq => `
        <div class="card" style="cursor:pointer" onclick="showEquipmentDetailModal(${eq.id})">
            <h3>${eq.name}</h3>
            <p>${eq.description || 'No description'}</p>
            <p><strong>Category:</strong> ${eq.category}</p>
            <p><strong>Available:</strong> ${eq.availableQuantity} / ${eq.totalQuantity}
               <span class="badge badge-${eq.status.toLowerCase()}">${eq.status}</span></p>
            <p class="meta">Added: ${formatDate(eq.createdAt)}</p>
            ${currentUser.role === 'ADMIN' ? `
                <div class="actions" onclick="event.stopPropagation()">
                    <button class="btn btn-warning btn-sm" onclick="showEditEquipmentModal(${eq.id})">Edit</button>
                    <button class="btn btn-danger btn-sm" onclick="deleteEquipment(${eq.id})">Delete</button>
                </div>
            ` : ''}
        </div>
    `).join('');
}

async function showEquipmentDetailModal(id) {
    try {
        const eq = state.equipmentCache.find(e => e.id === id) || await api('/equipment/' + id);
        let queueHtml = '<p style="color:#888">No pending requests in queue.</p>';
        try {
            const queue = await api('/rental-requests/prioritized/' + id);
            if (queue.length > 0) {
                queueHtml = `<ol style="padding-left:20px">${queue.map((r, i) => `
                    <li style="margin:6px 0">
                        <strong>${r.username}</strong> (priority ${r.priorityScore?.toFixed(1) || 'N/A'})
                        ${r.isForExam ? '<span class="badge badge-pending">EXAM</span>' : ''}
                        — ${r.startDate} to ${r.endDate}
                    </li>
                `).join('')}</ol>`;
            }
        } catch (e) { /* queue may be admin-only; keep empty */ }

        openModal(`
            <h3>${eq.name}</h3>
            <p>${eq.description || 'No description'}</p>
            <p><strong>Category:</strong> ${eq.category}</p>
            <p><strong>Status:</strong> <span class="badge badge-${eq.status.toLowerCase()}">${eq.status}</span></p>
            <p><strong>Available:</strong> ${eq.availableQuantity} / ${eq.totalQuantity}</p>
            <p class="meta">Added: ${formatDate(eq.createdAt)}</p>
            <hr style="margin:16px 0">
            <h3 style="margin-bottom:8px">Priority Queue</h3>
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
    if (!confirm('Are you sure you want to delete this equipment?')) return;
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
    const status = document.getElementById('my-status-filter')?.value || '';
    const sort = document.getElementById('my-sort')?.value || 'newest';

    let list = [...state.myRequestsCache];
    if (status) list = list.filter(r => r.status === status);

    if (sort === 'overdue') {
        list.sort((a, b) => {
            if (a.overdue !== b.overdue) return a.overdue ? -1 : 1;
            return new Date(b.createdAt) - new Date(a.createdAt);
        });
    } else if (sort === 'oldest') {
        list.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
    } else {
        list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    }

    const container = document.getElementById('my-requests-list');
    if (list.length === 0) {
        const isFiltered = (document.getElementById('my-status-filter')?.value || '') !== '';
        container.innerHTML = emptyState(
            isFiltered ? 'No requests match your filter' : 'No requests yet',
            isFiltered ? 'Try selecting a different status.' : 'Create one from the "New Request" tab to get started.'
        );
        return;
    }
    container.innerHTML = list.map(r => renderRequestCard(r, false, state.myQueuePositions[r.id])).join('');
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
        applyManageFilters();
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

function applyManageFilters() {
    const search = (document.getElementById('mgr-search')?.value || '').toLowerCase();
    const status = document.getElementById('mgr-status-filter')?.value || '';
    const isFifo = state.activeStrategy === 'fifo';

    let list = [...state.requestsCache];
    if (search) list = list.filter(r =>
        r.username.toLowerCase().includes(search) ||
        r.equipmentName.toLowerCase().includes(search));
    if (status) list = list.filter(r => r.status === status);

    // Overdue always first (operator attention). Then the active strategy
    // decides: FIFO → oldest createdAt first; Weighted → priority desc with
    // oldest-first tiebreak (matches the backend's ORDER BY).
    list.sort((a, b) => {
        if ((a.overdue ? 1 : 0) !== (b.overdue ? 1 : 0)) return a.overdue ? -1 : 1;
        if (isFifo) {
            return new Date(a.createdAt) - new Date(b.createdAt);
        }
        const pa = a.priorityScore ?? 0, pb = b.priorityScore ?? 0;
        if (pa !== pb) return pb - pa;
        return new Date(a.createdAt) - new Date(b.createdAt);
    });

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
        ? `<h3 style="margin:16px 0 8px">${title} (${items.length})</h3>${items.map(r => renderRequestCard(r, true)).join('')}`
        : '';

    container.innerHTML =
        section('Pending — awaiting decision', pending) +
        section('Approved — ready to be handed out', approved) +
        section('Rented — currently with user', rented) +
        section('Returned — awaiting assessment', returned) +
        section('Other', other);
}

async function approveRequest(id) {
    try {
        await api('/rental-requests/' + id + '/approve', { method: 'PUT' });
        toast('Request approved', 'success');
        loadPendingRequests();
    } catch (e) {
        toast(e.message, 'error');
    }
}

async function rejectRequest(id) {
    if (!confirm('Reject this request?')) return;
    try {
        await api('/rental-requests/' + id + '/reject', { method: 'PUT' });
        toast('Request rejected', 'info');
        loadPendingRequests();
    } catch (e) {
        toast(e.message, 'error');
    }
}

async function markRented(id) {
    try {
        await api('/rental-requests/' + id + '/rent', { method: 'PUT' });
        toast('Marked as rented', 'success');
        reloadRequestsView();
    } catch (e) {
        toast(e.message, 'error');
    }
}

async function markReturned(id) {
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

    let list = [...state.usersCache];
    if (search) list = list.filter(u =>
        u.username.toLowerCase().includes(search) ||
        u.email.toLowerCase().includes(search));
    if (typeFilter) list = list.filter(u => u.userType === typeFilter);

    const key = state.usersSortKey;
    const dir = state.usersSortDir === 'asc' ? 1 : -1;
    list.sort((a, b) => {
        const av = a[key], bv = b[key];
        if (typeof av === 'number' && typeof bv === 'number') return (av - bv) * dir;
        return String(av).localeCompare(String(bv)) * dir;
    });

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
        const returned = allRequests.filter(r => r.status === 'RETURNED');
        const users = await api('/users');

        const options = returned.length
            ? returned.map(r => `<option value="${r.id}">#${r.id} — ${r.username} — ${r.equipmentName} (returned ${r.returnedAt || 'recently'})</option>`).join('')
            : '<option value="" disabled>No returned requests awaiting assessment</option>';

        let html = '<h3 style="margin-bottom:12px">Submit New Assessment</h3>';
        html += `
            <div class="card" style="max-width:600px; margin-bottom:24px">
                <div class="form-group">
                    <label>Returned Rental Request</label>
                    <select id="assess-req-id">${options}</select>
                </div>
                <div class="form-group">
                    <label>Condition Rating</label>
                    <select id="assess-rating">
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
                <button class="btn btn-success" ${returned.length === 0 ? 'disabled' : ''} onclick="submitAssessment()">Submit Assessment</button>
            </div>
        `;

        html += '<h3 style="margin-bottom:12px">Assessment History by User</h3>';
        for (const u of users) {
            try {
                const assessments = await api('/return-assessments/user/' + u.id);
                if (assessments.length > 0) {
                    html += `<div class="card"><h3>${u.username} (reputation: ${u.reputationScore.toFixed(1)})</h3>`;
                    for (const a of assessments) {
                        const ratingBadge = a.conditionRating === 'EXCELLENT' || a.conditionRating === 'GOOD' ? 'approved'
                            : a.conditionRating === 'FAIR' ? 'pending' : 'rejected';
                        html += `<p>Request #${a.rentalRequestId}:
                            <span class="badge badge-${ratingBadge}">${a.conditionRating}</span>
                            Impact: ${a.reputationImpact > 0 ? '+' : ''}${a.reputationImpact.toFixed(1)} | ${a.notes || 'No notes'}</p>`;
                    }
                    html += '</div>';
                }
            } catch (e) { /* skip */ }
        }

        document.getElementById('assessments-list').innerHTML = html;
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
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
        loadAssessments();
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
    const priorityClass = (req.priorityScore || 0) >= 75 ? 'priority-high'
        : (req.priorityScore || 0) >= 50 ? 'priority-medium' : 'priority-low';

    let actions = '';
    const isAdmin = currentUser.role === 'ADMIN';

    if (isAdmin && showAdminActions && req.status === 'PENDING') {
        actions = `
            <button class="btn btn-success btn-sm" onclick="approveRequest(${req.id})">Approve</button>
            <button class="btn btn-danger btn-sm" onclick="rejectRequest(${req.id})">Reject</button>
        `;
    }
    if (isAdmin && req.status === 'APPROVED') {
        actions += `<button class="btn btn-info btn-sm" onclick="markRented(${req.id})">Mark Rented</button>`;
    }
    if (req.status === 'RENTED') {
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

    return `
        <div class="card ${req.overdue ? 'overdue' : ''}">
            <div style="display:flex; justify-content:space-between; align-items:start; gap:8px">
                <div>
                    <h3>${req.equipmentName}</h3>
                    <p><strong>User:</strong> ${req.username} | <strong>Request #${req.id}</strong></p>
                </div>
                <div style="display:flex; gap:6px; flex-wrap:wrap; justify-content:flex-end">
                    <span class="badge badge-${req.status.toLowerCase()}">${req.status}</span>
                    ${timingBadge}
                </div>
            </div>
            <p><strong>Period:</strong> ${req.startDate} to ${req.endDate}${req.returnedAt ? ` · returned ${req.returnedAt}` : ''}</p>
            ${req.projectDescription ? `<p><strong>Project:</strong> ${req.projectDescription}</p>` : ''}
            ${req.isForExam ? `<p><strong>Exam:</strong> ${req.examDate || 'N/A'} - ${req.justification || 'No justification'}</p>` : ''}
            <p>Priority: <span class="priority-score ${priorityClass}">${req.priorityScore?.toFixed(1) || 'N/A'}</span> ${queueBadge}</p>
            <p class="meta">Created: ${formatDate(req.createdAt)}</p>
            ${actions ? `<div class="actions">${actions}</div>` : ''}
        </div>
    `;
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
