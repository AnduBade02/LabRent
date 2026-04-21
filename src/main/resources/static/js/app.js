// ===== STATE =====
let token = localStorage.getItem('jwt_token');
let currentUser = JSON.parse(localStorage.getItem('current_user') || 'null');

// ===== API HELPER =====
async function api(path, options = {}) {
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = 'Bearer ' + token;

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

    // Hide exam fields for non-students
    const examFields = document.getElementById('exam-fields');
    if (examFields) {
        examFields.classList.toggle('hidden', currentUser.userType !== 'STUDENT');
    }

    showSection('dashboard');
}

// ===== NAVIGATION =====
function showSection(name) {
    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    document.getElementById('section-' + name).classList.add('active');
    document.querySelectorAll('nav button').forEach(b => b.classList.remove('active'));
    const navBtns = document.querySelectorAll('nav button');
    navBtns.forEach(b => {
        if (b.getAttribute('onclick')?.includes(name)) b.classList.add('active');
    });

    // Load data for each section
    switch (name) {
        case 'dashboard': loadDashboard(); break;
        case 'equipment': loadEquipment(); break;
        case 'my-requests': loadMyRequests(); break;
        case 'new-request': loadEquipmentSelect(); break;
        case 'pending-requests': loadPendingRequests(); break;
        case 'all-users': loadAllUsers(); break;
        case 'assessments': loadAssessments(); break;
        case 'strategy': loadStrategy(); break;
    }
}

// ===== DASHBOARD =====
async function loadDashboard() {
    try {
        const [me, equipment] = await Promise.all([
            api('/users/me'),
            api('/equipment')
        ]);
        currentUser = me;
        localStorage.setItem('current_user', JSON.stringify(me));

        const available = equipment.filter(e => e.availableQuantity > 0).length;

        document.getElementById('dashboard-stats').innerHTML = `
            <div class="stat-card">
                <div class="number">${me.reputationScore.toFixed(1)}</div>
                <div class="label">Reputation Score</div>
            </div>
            <div class="stat-card">
                <div class="number">${me.userType}</div>
                <div class="label">User Type</div>
            </div>
            <div class="stat-card">
                <div class="number">${equipment.length}</div>
                <div class="label">Total Equipment</div>
            </div>
            <div class="stat-card">
                <div class="number">${available}</div>
                <div class="label">Available</div>
            </div>
        `;

        document.getElementById('dashboard-content').innerHTML = `
            <div class="card">
                <h3>Welcome, ${me.username}!</h3>
                <p>Role: <span class="badge badge-${me.role === 'ADMIN' ? 'approved' : 'rented'}">${me.role}</span></p>
                <p>Type: ${me.userType} | Reputation: ${me.reputationScore.toFixed(1)}/200</p>
                <p class="meta">Account created: ${formatDate(me.createdAt)}</p>
            </div>
        `;
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

// ===== EQUIPMENT =====
async function loadEquipment() {
    try {
        const list = await api('/equipment');
        const container = document.getElementById('equipment-list');
        if (list.length === 0) {
            container.innerHTML = '<p style="color:#888">No equipment found.</p>';
            return;
        }
        container.innerHTML = list.map(eq => `
            <div class="card">
                <h3>${eq.name}</h3>
                <p>${eq.description || 'No description'}</p>
                <p><strong>Category:</strong> ${eq.category}</p>
                <p><strong>Available:</strong> ${eq.availableQuantity} / ${eq.totalQuantity}
                   <span class="badge badge-${eq.status.toLowerCase()}">${eq.status}</span></p>
                <p class="meta">Added: ${formatDate(eq.createdAt)}</p>
                ${currentUser.role === 'ADMIN' ? `
                    <div class="actions">
                        <button class="btn btn-warning btn-sm" onclick="showEditEquipmentModal(${eq.id})">Edit</button>
                        <button class="btn btn-danger btn-sm" onclick="deleteEquipment(${eq.id})">Delete</button>
                    </div>
                ` : ''}
            </div>
        `).join('');
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

function showAddEquipmentModal() {
    openModal(`
        <h3>Add Equipment</h3>
        <div class="form-group">
            <label>Name</label>
            <input type="text" id="eq-name" placeholder="e.g. Osciloscop Rigol DS1054Z">
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
        showAlert('app-alert', 'Equipment added successfully!', 'success');
        loadEquipment();
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
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
        showAlert('app-alert', e.message, 'error');
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
        showAlert('app-alert', 'Equipment updated!', 'success');
        loadEquipment();
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

async function deleteEquipment(id) {
    if (!confirm('Are you sure you want to delete this equipment?')) return;
    try {
        await api('/equipment/' + id, { method: 'DELETE' });
        showAlert('app-alert', 'Equipment deleted.', 'success');
        loadEquipment();
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

// ===== MY REQUESTS =====
async function loadMyRequests() {
    try {
        const list = await api('/rental-requests/my');
        const container = document.getElementById('my-requests-list');
        if (list.length === 0) {
            container.innerHTML = '<p style="color:#888">No requests yet. Create one from the "New Request" tab.</p>';
            return;
        }
        container.innerHTML = list.map(renderRequestCard).join('');
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
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
        // Set default dates
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
        showAlert('app-alert', `Request created! Priority score: ${result.priorityScore?.toFixed(1) || 'N/A'}`, 'success');
        document.getElementById('req-description').value = '';
        showSection('my-requests');
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

// ===== PENDING REQUESTS (Admin) =====
async function loadPendingRequests() {
    try {
        const list = await api('/rental-requests/all');
        const container = document.getElementById('pending-requests-list');
        if (list.length === 0) {
            container.innerHTML = '<p style="color:#888">No requests.</p>';
            return;
        }

        const pending = list.filter(r => r.status === 'PENDING')
            .sort((a, b) => (b.priorityScore || 0) - (a.priorityScore || 0));
        const approved = list.filter(r => r.status === 'APPROVED');
        const rented = list.filter(r => r.status === 'RENTED');
        const returned = list.filter(r => r.status === 'RETURNED');

        const section = (title, items) => items.length
            ? `<h3 style="margin:16px 0 8px">${title} (${items.length})</h3>${items.map(r => renderRequestCard(r, true)).join('')}`
            : '';

        container.innerHTML =
            section('Pending — awaiting decision', pending) +
            section('Approved — ready to be handed out', approved) +
            section('Rented — currently with user', rented) +
            section('Returned — awaiting assessment', returned);
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

async function approveRequest(id) {
    try {
        await api('/rental-requests/' + id + '/approve', { method: 'PUT' });
        showAlert('app-alert', 'Request approved!', 'success');
        loadPendingRequests();
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

async function rejectRequest(id) {
    if (!confirm('Reject this request?')) return;
    try {
        await api('/rental-requests/' + id + '/reject', { method: 'PUT' });
        showAlert('app-alert', 'Request rejected.', 'success');
        loadPendingRequests();
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

async function markRented(id) {
    try {
        await api('/rental-requests/' + id + '/rent', { method: 'PUT' });
        showAlert('app-alert', 'Marked as rented!', 'success');
        reloadRequestsView();
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

async function markReturned(id) {
    try {
        await api('/rental-requests/' + id + '/return', { method: 'PUT' });
        showAlert('app-alert', 'Marked as returned!', 'success');
        reloadRequestsView();
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
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
        document.getElementById('users-table-container').innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Username</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Type</th>
                        <th>Reputation</th>
                        <th>Created</th>
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
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

// ===== ASSESSMENTS (Admin) =====
async function loadAssessments() {
    try {
        // Get all requests, filter RETURNED ones
        const pending = await api('/rental-requests/pending');
        // We need all requests — let's get from all users. Since we're admin, get pending requests
        // Actually we don't have an endpoint for all RETURNED requests. Let's use a workaround:
        // get all users, then get their requests
        const users = await api('/users');
        let allRequests = [];
        for (const u of users) {
            try {
                // We don't have a "get requests by user id" endpoint for admin
                // but we can check the rental-requests/pending and other statuses
                // Actually let's just show all requests and filter RETURNED
                const userReqs = await api('/rental-requests/my');
                // This won't work for other users. Let me get from the pending endpoint approach
                break;
            } catch(e) { break; }
        }

        // Better approach: Use multiple status endpoints or show guidance
        const container = document.getElementById('assessments-list');

        // Fetch all users and for each show assessment history
        let html = '<h3 style="margin-bottom:12px">Submit New Assessment</h3>';
        html += `
            <div class="card" style="max-width:500px; margin-bottom:24px">
                <div class="form-group">
                    <label>Rental Request ID (must be in RETURNED status)</label>
                    <input type="number" id="assess-req-id" placeholder="e.g. 5">
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
                <button class="btn btn-success" onclick="submitAssessment()">Submit Assessment</button>
            </div>
        `;

        // Show assessment history per user
        html += '<h3 style="margin-bottom:12px">Assessment History by User</h3>';
        for (const u of users) {
            try {
                const assessments = await api('/return-assessments/user/' + u.id);
                if (assessments.length > 0) {
                    html += `<div class="card"><h3>${u.username} (reputation: ${u.reputationScore.toFixed(1)})</h3>`;
                    for (const a of assessments) {
                        html += `<p>Request #${a.rentalRequestId}:
                            <span class="badge badge-${a.conditionRating === 'EXCELLENT' || a.conditionRating === 'GOOD' ? 'approved' : a.conditionRating === 'FAIR' ? 'pending' : 'rejected'}">${a.conditionRating}</span>
                            Impact: ${a.reputationImpact > 0 ? '+' : ''}${a.reputationImpact.toFixed(1)} | ${a.notes || 'No notes'}</p>`;
                    }
                    html += '</div>';
                }
            } catch (e) { /* skip */ }
        }

        container.innerHTML = html;
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

async function submitAssessment() {
    try {
        const result = await api('/return-assessments', {
            method: 'POST',
            body: JSON.stringify({
                rentalRequestId: parseInt(document.getElementById('assess-req-id').value),
                conditionRating: document.getElementById('assess-rating').value,
                notes: document.getElementById('assess-notes').value || null
            })
        });
        showAlert('app-alert',
            `Assessment submitted! Rating: ${result.conditionRating}, Reputation impact: ${result.reputationImpact > 0 ? '+' : ''}${result.reputationImpact.toFixed(1)}`,
            'success');
        loadAssessments();
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

// ===== STRATEGY (Admin) =====
async function loadStrategy() {
    try {
        const data = await api('/admin/prioritization-strategy');
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
        document.getElementById('current-strategy').textContent = data.strategy;
        showAlert('app-alert', 'Strategy changed to: ' + data.strategy, 'success');
    } catch (e) {
        showAlert('app-alert', e.message, 'error');
    }
}

// ===== RENDER HELPERS =====
function renderRequestCard(req, showAdminActions = false) {
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

    return `
        <div class="card">
            <div style="display:flex; justify-content:space-between; align-items:start">
                <div>
                    <h3>${req.equipmentName}</h3>
                    <p><strong>User:</strong> ${req.username} | <strong>Request #${req.id}</strong></p>
                </div>
                <span class="badge badge-${req.status.toLowerCase()}">${req.status}</span>
            </div>
            <p><strong>Period:</strong> ${req.startDate} to ${req.endDate}</p>
            ${req.projectDescription ? `<p><strong>Project:</strong> ${req.projectDescription}</p>` : ''}
            ${req.isForExam ? `<p><strong>Exam:</strong> ${req.examDate || 'N/A'} - ${req.justification || 'No justification'}</p>` : ''}
            <p>Priority: <span class="priority-score ${priorityClass}">${req.priorityScore?.toFixed(1) || 'N/A'}</span></p>
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

function showAlert(containerId, message, type) {
    const container = document.getElementById(containerId);
    container.innerHTML = `<div class="alert alert-${type}">${message}</div>`;
    setTimeout(() => { container.innerHTML = ''; }, 5000);
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
