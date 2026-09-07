// Global Application State & Chart Registries
const AppState = {
    user: null,
    activePanel: 'panel-dashboard',
    accounts: [],
    budgets: [],
    alerts: [],
    charts: {
        dailyTrend: null,
        byProvider: null,
        byService: null,
        byResource: null,
        analyticsTrend: null
    }
};

// UI Elements
const els = {
    toast: document.getElementById('toast'),
    authContainer: document.getElementById('auth-container'),
    appContainer: document.getElementById('app-container'),
    loginForm: document.getElementById('login-form'),
    registerForm: document.getElementById('register-form'),
    btnLogout: document.getElementById('btn-logout'),
    navLinks: document.querySelectorAll('.nav-link'),
    panels: document.querySelectorAll('.content-panel'),
    currentUsername: document.getElementById('current-username'),
    pageTitle: document.getElementById('page-title'),
    pageSubtitle: document.getElementById('page-subtitle'),
    btnSyncAll: document.getElementById('btn-sync-all'),
    
    // Auth Toggles
    toSignup: document.getElementById('to-signup'),
    toLogin: document.getElementById('to-login'),

    // Modals
    accountModal: document.getElementById('account-modal'),
    budgetModal: document.getElementById('budget-modal'),
    openAccModalBtn: document.getElementById('btn-open-account-modal'),
    openBudModalBtn: document.getElementById('btn-open-budget-modal'),
    closeModalBtns: document.querySelectorAll('.btn-close-modal'),
    addAccountForm: document.getElementById('add-account-form'),
    setBudgetForm: document.getElementById('set-budget-form'),
    
    // KPI Cards
    kpiMonthlySpent: document.getElementById('kpi-monthly-spent'),
    kpiChangeRate: document.getElementById('kpi-change-rate'),
    kpiProjectedSpent: document.getElementById('kpi-projected-spent'),
    kpiActiveAccounts: document.getElementById('kpi-active-accounts'),
    kpiUnreadAlerts: document.getElementById('kpi-unread-alerts'),
    badgeUnreadAlerts: document.getElementById('badge-unread-alerts'),
    
    // Dynamic Tables & Lists
    accountsTableBody: document.querySelector('#accounts-table tbody'),
    budgetsTableBody: document.querySelector('#budgets-table tbody'),
    budgetProgressCards: document.getElementById('budget-progress-cards'),
    dashboardAlertsList: document.getElementById('dashboard-alerts-list'),
    btnReadAllAlerts: document.getElementById('btn-read-all-alerts'),
    
    // Analytics & Filters
    filterAccount: document.getElementById('filter-account'),
    filterStartDate: document.getElementById('filter-start-date'),
    filterEndDate: document.getElementById('filter-end-date'),
    btnApplyFilters: document.getElementById('btn-apply-filters'),
    analyticsServiceTableBody: document.querySelector('#analytics-service-table tbody'),
    
    // Reports & Exports
    reportsTableBody: document.querySelector('#reports-table tbody'),
    btnExportCsv: document.getElementById('btn-export-csv'),
    btnExportPdf: document.getElementById('btn-export-pdf'),

    // Resource Inventory
    resFilterAccount: document.getElementById('res-filter-account'),
    resFilterType: document.getElementById('res-filter-type'),
    btnApplyResFilters: document.getElementById('btn-apply-res-filters'),
    resourcesTableBody: document.querySelector('#resources-table tbody'),
    dashboardAnomaliesList: document.getElementById('dashboard-anomalies-list'),

    // Profile Settings
    userProfileTrigger: document.getElementById('user-profile-trigger'),
    profileModal: document.getElementById('profile-modal'),
    profileForm: document.getElementById('profile-form'),
    profUsername: document.getElementById('prof-username'),
    profEmail: document.getElementById('prof-email'),
    profPassword: document.getElementById('prof-password'),

    // Email Sandbox
    emailInboxList: document.getElementById('email-inbox-list'),
    emailSubjectView: document.getElementById('email-subject-view'),
    emailSenderView: document.getElementById('email-sender-view'),
    emailDateView: document.getElementById('email-date-view'),
    emailBodyView: document.getElementById('email-body-view'),
    btnClearInbox: document.getElementById('btn-clear-inbox')
};

// Colors matching brand themes
const chartColors = {
    aws: '#ff9900',
    azure: '#0089d6',
    oci: '#f30000',
    indigo: '#6366f1',
    purple: '#a855f7',
    emerald: '#10b981',
    rose: '#ef4444',
    amber: '#f59e0b',
    border: 'rgba(255, 255, 255, 0.08)',
    text: '#cbd5e1',
    grid: 'rgba(255, 255, 255, 0.04)',
    palette: ['#6366f1', '#3b82f6', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#06b6d4']
};

// ----------------------------------------------------
// UTILITY FUNCTIONS
// ----------------------------------------------------

function showToast(message, type = 'success') {
    els.toast.textContent = message;
    els.toast.className = `toast show ${type}`;
    setTimeout(() => {
        els.toast.className = 'toast hidden';
    }, 4000);
}

function formatCurrency(value) {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

function formatDate(dateString) {
    if (!dateString) return 'Never';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

// ----------------------------------------------------
// AUTHENTICATION MANAGEMENT
// ----------------------------------------------------

async function checkAuthStatus() {
    try {
        const res = await fetch('/api/auth/status');
        if (res.ok) {
            const data = await res.json();
            if (data.authenticated) {
                handleLoginSuccess(data);
                return;
            }
        }
    } catch (e) {
        console.warn("Session check complete. Unauthenticated.");
    }
    showAuthPortal();
}

function showAuthPortal() {
    els.appContainer.classList.add('hidden');
    els.authContainer.classList.remove('hidden');
}

function handleLoginSuccess(user) {
    AppState.user = user;
    els.currentUsername.textContent = user.username;
    els.authContainer.classList.add('hidden');
    els.appContainer.classList.remove('hidden');
    showToast(`Welcome back, ${user.username}!`, 'success');
    
    // Reset defaults & Load initial app data
    switchPanel('panel-dashboard');
    loadDashboardData();
}

// Login
els.loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = els.loginForm.querySelector('#login-username').value;
    const password = els.loginForm.querySelector('#login-password').value;

    try {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json();
        if (res.ok) {
            handleLoginSuccess(data);
            els.loginForm.reset();
        } else {
            showToast(data.message || 'Login failed. Please check credentials.', 'error');
        }
    } catch (err) {
        showToast('Connection to authentication server failed.', 'error');
    }
});

// Register
els.registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = els.registerForm.querySelector('#reg-username').value;
    const email = els.registerForm.querySelector('#reg-email').value;
    const password = els.registerForm.querySelector('#reg-password').value;

    try {
        const res = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, password })
        });
        const data = await res.json();
        if (res.ok) {
            showToast('Registration successful! Please Sign In.', 'success');
            toggleAuthForms();
            els.registerForm.reset();
        } else {
            showToast(data.message || 'Registration failed.', 'error');
        }
    } catch (err) {
        showToast('Network error during registration.', 'error');
    }
});

// Logout
els.btnLogout.addEventListener('click', async () => {
    try {
        await fetch('/api/auth/logout', { method: 'POST' });
        AppState.user = null;
        showToast('You have signed out successfully.', 'success');
        showAuthPortal();
    } catch (e) {
        showAuthPortal();
    }
});

// Auth form toggles
function toggleAuthForms() {
    els.loginForm.classList.toggle('hidden');
    els.registerForm.classList.toggle('hidden');
}
els.toSignup.addEventListener('click', (e) => { e.preventDefault(); toggleAuthForms(); });
els.toLogin.addEventListener('click', (e) => { e.preventDefault(); toggleAuthForms(); });

// ----------------------------------------------------
// NAVIGATION PANELS ROUTER
// ----------------------------------------------------

function switchPanel(panelId) {
    AppState.activePanel = panelId;
    
    // Toggle active classes on nav
    els.navLinks.forEach(link => {
        if (link.getAttribute('data-target') === panelId) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });

    // Toggle active panels
    els.panels.forEach(panel => {
        if (panel.id === panelId) {
            panel.classList.add('active');
        } else {
            panel.classList.remove('active');
        }
    });

    // Header adjustments
    switch (panelId) {
        case 'panel-dashboard':
            els.pageTitle.textContent = "Dashboard Overview";
            els.pageSubtitle.textContent = "Real-time spending across your integrated cloud infrastructure";
            loadDashboardData();
            break;
        case 'panel-accounts':
            els.pageTitle.textContent = "Cloud Integrations";
            els.pageSubtitle.textContent = "Manage and synchronize your AWS, Azure, and Oracle credentials";
            loadAccountsData();
            break;
        case 'panel-budgets':
            els.pageTitle.textContent = "Budgets & Limit Alerts";
            els.pageSubtitle.textContent = "Configure cost boundaries to prevent billing surprises";
            loadBudgetsData();
            break;
        case 'panel-analytics':
            els.pageTitle.textContent = "Granular Cost Analytics";
            els.pageSubtitle.textContent = "Filter and examine resource costs by service, dates, and integrations";
            loadAnalyticsData();
            break;
        case 'panel-reports':
            els.pageTitle.textContent = "Monthly Statements & Insights";
            els.pageSubtitle.textContent = "Export monthly totals and evaluate automatic optimization tips";
            loadReportsData();
            break;
        case 'panel-resources':
            els.pageTitle.textContent = "Cloud Resource Inventory";
            els.pageSubtitle.textContent = "Granular visibility into compute, database, and storage assets";
            loadResourcesData();
            break;
        case 'panel-email-sandbox':
            els.pageTitle.textContent = "Simulated Webmail Inbox";
            els.pageSubtitle.textContent = "Inspect sandboxed emails and budget alerts sent by the dashboard";
            loadSandboxEmails();
            break;
    }
}

els.navLinks.forEach(link => {
    link.addEventListener('click', (e) => {
        e.preventDefault();
        const target = link.getAttribute('data-target');
        switchPanel(target);
    });
});

// Sync all button
els.btnSyncAll.addEventListener('click', async () => {
    const icon = els.btnSyncAll.querySelector('i');
    icon.classList.add('fa-spin');
    els.btnSyncAll.disabled = true;
    showToast('Initiating cloud cost synchronization...', 'warning');

    try {
        let syncedCount = 0;
        for (const account of AppState.accounts) {
            const res = await fetch(`/api/accounts/${account.id}/sync`, { method: 'POST' });
            if (res.ok) syncedCount++;
        }
        showToast(`Synchronization completed. Synced ${syncedCount} accounts.`, 'success');
        
        // Reload current view
        switchPanel(AppState.activePanel);
    } catch (e) {
        showToast('Sync pipeline encountered errors.', 'error');
    } finally {
        icon.classList.remove('fa-spin');
        els.btnSyncAll.disabled = false;
    }
});

// ----------------------------------------------------
// MODALS LOGIC
// ----------------------------------------------------

// Account Modal Form Dynamic Labels
els.accountModal.querySelector('#acc-provider').addEventListener('change', (e) => {
    const provider = e.target.value;
    const lblId = document.getElementById('lbl-acc-id');
    const lblKey = document.getElementById('lbl-acc-key');
    const lblSecret = document.getElementById('lbl-acc-secret');
    const lblOptional = document.getElementById('lbl-acc-optional');
    
    const inpId = document.getElementById('acc-identifier');
    const inpKey = document.getElementById('acc-key');
    const inpSecret = document.getElementById('acc-secret');
    const inpOptional = document.getElementById('acc-optional');

    if (provider === 'AWS') {
        lblId.textContent = "AWS Account ID";
        inpId.placeholder = "12-digit AWS account number";
        lblKey.textContent = "Access Key ID";
        inpKey.placeholder = "AKIAIOSFODNN7EXAMPLE";
        lblSecret.textContent = "Secret Access Key";
        inpSecret.placeholder = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
        lblOptional.textContent = "Default Region";
        inpOptional.placeholder = "e.g. us-east-1";
    } else if (provider === 'AZURE') {
        lblId.textContent = "Subscription ID";
        inpId.placeholder = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx";
        lblKey.textContent = "Application (Client) ID";
        inpKey.placeholder = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx";
        lblSecret.textContent = "Client Secret Key";
        inpSecret.placeholder = "Azure AD application credentials secret";
        lblOptional.textContent = "Directory (Tenant) ID";
        inpOptional.placeholder = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx";
    } else if (provider === 'OCI') {
        lblId.textContent = "Tenancy OCID";
        inpId.placeholder = "ocid1.tenancy.oc1..aaaaaaaaxxxxxx";
        lblKey.textContent = "User OCID";
        inpKey.placeholder = "ocid1.user.oc1..aaaaaaaaxxxxxx";
        lblSecret.textContent = "API Private Key (PEM)";
        inpSecret.placeholder = "-----BEGIN RSA PRIVATE KEY----- ...";
        lblOptional.textContent = "Key Fingerprint";
        inpOptional.placeholder = "aa:bb:cc:dd:ee:ff:gg:hh:ii:jj:kk:ll:mm:nn:oo:pp";
    }
});

// Modal Open/Close triggers
els.openAccModalBtn.addEventListener('click', () => { els.accountModal.classList.remove('hidden'); });
els.openBudModalBtn.addEventListener('click', () => { 
    // Populate scopes list in budget select options
    const scopeSelect = els.budgetModal.querySelector('#bud-scope');
    scopeSelect.innerHTML = '<option value="">Global (All Clouds)</option>';
    AppState.accounts.forEach(acc => {
        scopeSelect.innerHTML += `<option value="${acc.id}">${acc.name} (${acc.provider})</option>`;
    });
    els.budgetModal.classList.remove('hidden'); 
});

els.closeModalBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
        e.preventDefault();
        els.accountModal.classList.add('hidden');
        els.budgetModal.classList.add('hidden');
        if (els.profileModal) els.profileModal.classList.add('hidden');
    });
});

// Profile triggers and submit
if (els.userProfileTrigger) {
    els.userProfileTrigger.addEventListener('click', () => {
        if (AppState.user) {
            els.profUsername.value = AppState.user.username || '';
            els.profEmail.value = AppState.user.email || '';
            els.profPassword.value = '';
            els.profileModal.classList.remove('hidden');
        }
    });
}

if (els.profileForm) {
    els.profileForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = els.profUsername.value;
        const email = els.profEmail.value;
        const password = els.profPassword.value;

        try {
            const res = await fetch('/api/auth/profile', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, email, password })
            });
            const data = await res.json();
            if (res.ok) {
                showToast('Profile updated successfully!', 'success');
                AppState.user = data;
                els.currentUsername.textContent = data.username;
                els.profileModal.classList.add('hidden');
            } else {
                showToast(data.message || 'Failed to update profile.', 'error');
            }
        } catch (err) {
            showToast('Network error during profile update.', 'error');
        }
    });
}

// Add Account Submit
els.addAccountForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const provider = document.getElementById('acc-provider').value;
    const name = document.getElementById('acc-name').value;
    const accountIdentifier = document.getElementById('acc-identifier').value;
    const credentialKey = document.getElementById('acc-key').value;
    const credentialSecret = document.getElementById('acc-secret').value;
    const optionalConfig = document.getElementById('acc-optional').value;

    const btnSubmit = els.addAccountForm.querySelector('button[type="submit"]');
    btnSubmit.disabled = true;
    btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Core Syncing...';

    try {
        const res = await fetch('/api/accounts', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ provider, name, accountIdentifier, credentialKey, credentialSecret, optionalConfig })
        });
        const data = await res.json();
        
        if (res.ok) {
            showToast(`Connected to ${provider} account successfully! Seeded historical records.`, 'success');
            els.accountModal.classList.add('hidden');
            els.addAccountForm.reset();
            loadAccountsData();
        } else {
            showToast(data.message || 'Failed to connect cloud account.', 'error');
        }
    } catch (err) {
        showToast('Network error during account registration.', 'error');
    } finally {
        btnSubmit.disabled = false;
        btnSubmit.textContent = 'Connect Account';
    }
});

// Set Budget Submit
els.setBudgetForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = document.getElementById('bud-name').value;
    const cloudAccountId = document.getElementById('bud-scope').value;
    const limitAmount = document.getElementById('bud-limit').value;
    const thresholdPercentage = document.getElementById('bud-threshold').value;
    const emailNotification = document.getElementById('bud-email').checked;

    try {
        const res = await fetch('/api/budgets', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, cloudAccountId, limitAmount, thresholdPercentage, emailNotification })
        });
        const data = await res.json();
        
        if (res.ok) {
            showToast(`Created budget '${name}' successfully!`, 'success');
            els.budgetModal.classList.add('hidden');
            els.setBudgetForm.reset();
            loadBudgetsData();
        } else {
            showToast(data.message || 'Failed to save budget.', 'error');
        }
    } catch (err) {
        showToast('Network error during budget creation.', 'error');
    }
});

// ----------------------------------------------------
// CHART GENERATORS
// ----------------------------------------------------

function destroyChart(chartKey) {
    if (AppState.charts[chartKey]) {
        AppState.charts[chartKey].destroy();
        AppState.charts[chartKey] = null;
    }
}

// 1. Daily trend line chart
function renderDailyTrendChart(canvasId, dataset, forecastDataset, chartKey) {
    destroyChart(chartKey);
    const ctx = document.getElementById(canvasId).getContext('2d');
    
    let labels = dataset.map(d => d.date);
    let actualData = dataset.map(d => d.cost);
    let forecastData = [];

    if (forecastDataset && forecastDataset.length > 0) {
        const forecastLabels = forecastDataset.map(d => d.date);
        labels = [...labels, ...forecastLabels];
        
        actualData = [...actualData, ...Array(forecastDataset.length).fill(null)];
        
        forecastData = Array(dataset.length - 1).fill(null);
        if (dataset.length > 0) {
            forecastData.push(dataset[dataset.length - 1].cost);
        }
        forecastDataset.forEach(d => forecastData.push(d.cost));
    }

    const datasets = [{
        label: 'Actual Spend ($ USD)',
        data: actualData,
        borderColor: chartColors.indigo,
        backgroundColor: 'rgba(99, 102, 241, 0.08)',
        borderWidth: 2,
        fill: true,
        tension: 0.35,
        pointRadius: 2,
        pointHoverRadius: 6
    }];

    if (forecastDataset && forecastDataset.length > 0) {
        datasets.push({
            label: 'Projected Forecast ($ USD)',
            data: forecastData,
            borderColor: chartColors.purple,
            borderDash: [5, 5],
            borderWidth: 2,
            fill: false,
            tension: 0.35,
            pointRadius: 2,
            pointHoverRadius: 6
        });
    }

    AppState.charts[chartKey] = new Chart(ctx, {
        type: 'line',
        data: {
            labels,
            datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { 
                    display: forecastDataset && forecastDataset.length > 0,
                    position: 'top',
                    labels: { color: chartColors.text, font: { family: 'Inter', size: 11 } }
                },
                tooltip: {
                    backgroundColor: '#121522',
                    titleColor: '#cbd5e1',
                    bodyColor: '#ffffff',
                    borderColor: 'rgba(255, 255, 255, 0.1)',
                    borderWidth: 1,
                    displayColors: true,
                    callbacks: {
                        label: function(context) { return ` ${context.dataset.label.split(' ')[0]}: ${formatCurrency(context.parsed.y)}`; }
                    }
                }
            },
            scales: {
                x: {
                    grid: { color: chartColors.grid },
                    ticks: { color: chartColors.text, maxTicksLimit: 12 }
                },
                y: {
                    grid: { color: chartColors.grid },
                    ticks: { color: chartColors.text }
                }
            }
        }
    });
}

// 2. Spend by provider doughnut chart
function renderProviderBreakdown(canvasId, dataset) {
    destroyChart('byProvider');
    const ctx = document.getElementById(canvasId).getContext('2d');

    const labels = dataset.map(d => d.provider);
    const data = dataset.map(d => d.cost);
    const colors = dataset.map(d => {
        if (d.provider === 'AWS') return chartColors.aws;
        if (d.provider === 'AZURE') return chartColors.azure;
        if (d.provider === 'OCI') return chartColors.oci;
        return chartColors.indigo;
    });

    AppState.charts.byProvider = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels,
            datasets: [{
                data,
                backgroundColor: colors,
                borderWidth: 1,
                borderColor: '#121522'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { color: chartColors.text, font: { family: 'Inter', size: 11 } }
                },
                tooltip: {
                    backgroundColor: '#121522',
                    callbacks: {
                        label: function(context) {
                            const val = context.parsed;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const pct = Math.round((val / total) * 100);
                            return ` ${context.label}: ${formatCurrency(val)} (${pct}%)`;
                        }
                    }
                }
            },
            cutout: '70%'
        }
    });
}

// 3. Spend by service bar chart
function renderServiceBreakdown(canvasId, dataset) {
    destroyChart('byService');
    const ctx = document.getElementById(canvasId).getContext('2d');

    // Slice to top 8 services to keep chart clean
    const sorted = [...dataset].sort((a, b) => b.cost - a.cost).slice(0, 8);
    const labels = sorted.map(d => d.service);
    const data = sorted.map(d => d.cost);

    AppState.charts.byService = new Chart(ctx, {
        type: 'bar',
        data: {
            labels,
            datasets: [{
                data,
                backgroundColor: chartColors.palette,
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: '#121522',
                    callbacks: {
                        label: function(context) { return `Cost: ${formatCurrency(context.parsed.y)}`; }
                    }
                }
            },
            scales: {
                x: {
                    grid: { display: false },
                    ticks: { color: chartColors.text, font: { size: 10 } }
                },
                y: {
                    grid: { color: chartColors.grid },
                    ticks: { color: chartColors.text }
                }
            }
        }
    });
}

// 4. Spend by resource type bar chart (Horizontal)
function renderResourceTypeBreakdown(canvasId, dataset) {
    destroyChart('byResource');
    const ctx = document.getElementById(canvasId).getContext('2d');

    const labels = dataset.map(d => d.resourceType);
    const data = dataset.map(d => d.cost);

    AppState.charts.byResource = new Chart(ctx, {
        type: 'bar',
        data: {
            labels,
            datasets: [{
                data,
                backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#ec4899'],
                borderRadius: 4
            }]
        },
        options: {
            indexAxis: 'y',
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: '#121522',
                    callbacks: {
                        label: function(context) { return `Cost: ${formatCurrency(context.parsed.x)}`; }
                    }
                }
            },
            scales: {
                x: {
                    grid: { color: chartColors.grid },
                    ticks: { color: chartColors.text }
                },
                y: {
                    grid: { display: false },
                    ticks: { color: chartColors.text }
                }
            }
        }
    });
}

// ----------------------------------------------------
// PANEL DATA LOADERS
// ----------------------------------------------------

// 1. Dashboard Tab Data
async function loadDashboardData() {
    try {
        // Fetch stats
        const statsRes = await fetch('/api/dashboard/stats');
        if (statsRes.ok) {
            const stats = await statsRes.json();
            els.kpiMonthlySpent.textContent = formatCurrency(stats.thisMonthSpent);
            initRealtimeCosts(stats.thisMonthSpent);
            els.kpiProjectedSpent.textContent = formatCurrency(stats.forecastedSpend);
            els.kpiActiveAccounts.textContent = stats.activeAccountsCount;
            els.kpiUnreadAlerts.textContent = stats.unreadAlertsCount;
            
            // Alerts count badge
            if (stats.unreadAlertsCount > 0) {
                els.badgeUnreadAlerts.textContent = stats.unreadAlertsCount;
                els.badgeUnreadAlerts.classList.remove('hidden');
            } else {
                els.badgeUnreadAlerts.classList.add('hidden');
            }

            // MoM KPI Change formatting
            const chgEl = els.kpiChangeRate;
            const absolutePct = Math.abs(stats.changePercentage);
            if (stats.changePercentage > 0) {
                chgEl.className = 'kpi-change up';
                chgEl.innerHTML = `<i class="fa-solid fa-arrow-trend-up"></i> ${absolutePct}%`;
            } else if (stats.changePercentage < 0) {
                chgEl.className = 'kpi-change down';
                chgEl.innerHTML = `<i class="fa-solid fa-arrow-trend-down"></i> ${absolutePct}%`;
            } else {
                chgEl.className = 'kpi-change';
                chgEl.innerHTML = `0%`;
            }
        }

        // Fetch charts & forecasts
        const chartsRes = await fetch('/api/dashboard/charts');
        let forecastDataset = [];
        try {
            const forecastRes = await fetch('/api/dashboard/forecast');
            if (forecastRes.ok) {
                forecastDataset = await forecastRes.json();
            }
        } catch (e) {
            console.warn("Forecast aggregate failure", e);
        }

        if (chartsRes.ok) {
            const charts = await chartsRes.json();
            renderDailyTrendChart('chart-daily-trend', charts.dailyTrend, forecastDataset, 'dailyTrend');
            renderProviderBreakdown('chart-by-provider', charts.byProvider);
            renderServiceBreakdown('chart-by-service', charts.byService);
            renderResourceTypeBreakdown('chart-by-resource', charts.byResourceType);
        }

        // Fetch accounts details (to sync AppState.accounts for other tabs)
        const accsRes = await fetch('/api/accounts');
        if (accsRes.ok) {
            AppState.accounts = await accsRes.json();
        }

        // Load recent alerts
        loadRecentAlerts();
        
        // Load detected cost anomalies
        loadAnomaliesData();

    } catch (e) {
        showToast('Error retrieving dashboard aggregates.', 'error');
    }
}

// Render recent alerts
async function loadRecentAlerts() {
    try {
        const res = await fetch('/api/budgets/alerts');
        if (res.ok) {
            AppState.alerts = await res.json();
            const listEl = els.dashboardAlertsList;
            listEl.innerHTML = '';
            
            const unreadAlerts = AppState.alerts.filter(a => !a.read);
            
            if (unreadAlerts.length > 0) {
                els.btnReadAllAlerts.classList.remove('hidden');
                unreadAlerts.slice(0, 5).forEach(alert => {
                    const thresholdPct = alert.thresholdPercentage;
                    const limitVal = alert.limitAmount;
                    const spentVal = alert.spentAmount;
                    const dateStr = formatDate(alert.triggeredAt);
                    
                    listEl.innerHTML += `
                        <div class="alert-item">
                            <div class="alert-info">
                                <i class="fa-solid fa-triangle-exclamation text-rose" style="font-size: 1.3rem;"></i>
                                <div class="alert-info-desc">
                                    <h4>Threshold Breached: ${alert.budget.name}</h4>
                                    <p>Budget reached ${Math.round((spentVal/limitVal)*100)}% of its limit ($${spentVal} of $${limitVal})</p>
                                </div>
                            </div>
                            <div class="alert-actions">
                                <span class="alert-time">${dateStr}</span>
                                <button class="btn btn-secondary btn-sm py-1 px-2" onclick="dismissAlert(${alert.id})">Dismiss</button>
                            </div>
                        </div>
                    `;
                });
            } else {
                els.btnReadAllAlerts.classList.add('hidden');
                listEl.innerHTML = `
                    <div class="empty-state">
                        <i class="fa-regular fa-bell-slash"></i>
                        <p>No active budget breaches detected.</p>
                    </div>
                `;
            }
        }
    } catch (e) {
        console.error("Alerts sync failed", e);
    }
}

async function dismissAlert(alertId) {
    try {
        const res = await fetch(`/api/budgets/alerts/${alertId}/read`, { method: 'POST' });
        if (res.ok) {
            showToast('Alert dismissed.', 'success');
            loadDashboardData();
        }
    } catch (e) {
        showToast('Dismiss alert request failed.', 'error');
    }
}

els.btnReadAllAlerts.addEventListener('click', async () => {
    try {
        const res = await fetch('/api/budgets/alerts/read-all', { method: 'POST' });
        if (res.ok) {
            showToast('All alerts dismissed.', 'success');
            loadDashboardData();
        }
    } catch (e) {
        showToast('Could not clear alerts.', 'error');
    }
});

// 2. Cloud Accounts Tab Data
async function loadAccountsData() {
    try {
        const res = await fetch('/api/accounts');
        if (res.ok) {
            AppState.accounts = await res.json();
            const tbody = els.accountsTableBody;
            tbody.innerHTML = '';
            
            // Summarize counts for providers status cards
            const stats = { AWS: { count: 0, cost: 0 }, AZURE: { count: 0, cost: 0 }, OCI: { count: 0, cost: 0 } };

            // We can retrieve cost averages per provider from dashboard/charts endpoints if loaded, or fetch month aggregates.
            // Let's compute this month's provider spend client side using a quick analytics fetch
            const end = new Date().toISOString().slice(0,10);
            const start = end.slice(0, 8) + "01";
            const billingRes = await fetch(`/api/dashboard/charts?startDate=${start}&endDate=${end}`);
            if (billingRes.ok) {
                const bdata = await billingRes.json();
                bdata.byProvider.forEach(p => {
                    if (stats[p.provider]) stats[p.provider].cost = p.cost;
                });
            }

            if (AppState.accounts.length > 0) {
                AppState.accounts.forEach(acc => {
                    const prov = acc.provider.toUpperCase();
                    if (stats[prov]) stats[prov].count++;
                    
                    const statusClass = acc.status.toLowerCase();
                    const icon = acc.provider === 'AWS' ? 'fa-brands fa-aws text-aws' 
                               : acc.provider === 'AZURE' ? 'fa-brands fa-microsoft text-azure' 
                               : 'fa-solid fa-circle-nodes text-oci';
                    
                    tbody.innerHTML += `
                        <tr>
                            <td><i class="${icon}" style="font-size: 1.2rem; margin-right: 6px;"></i> <strong>${acc.provider}</strong></td>
                            <td>${acc.name}</td>
                            <td><code>${acc.accountIdentifier}</code></td>
                            <td><span class="badge-status ${statusClass}">${acc.status}</span></td>
                            <td>${formatDate(acc.lastSyncTime)}</td>
                            <td>
                                <button class="btn btn-secondary btn-sm py-1 px-2 mr-2" onclick="syncSingleAccount(${acc.id})" title="Force Sync">
                                    <i class="fa-solid fa-rotate"></i>
                                </button>
                                <button class="btn btn-danger btn-sm py-1 px-2" onclick="deleteSingleAccount(${acc.id})" title="Delete connection">
                                    <i class="fa-solid fa-trash-can"></i>
                                </button>
                            </td>
                        </tr>
                    `;
                });
            } else {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="6" class="text-center">No accounts added yet. Click "Add Account" to connect provider billing.</td>
                    </tr>
                `;
            }

            // Update cards stats
            document.getElementById('aws-account-count').textContent = stats.AWS.count;
            document.getElementById('aws-monthly-cost').textContent = formatCurrency(stats.AWS.cost);
            document.getElementById('azure-account-count').textContent = stats.AZURE.count;
            document.getElementById('azure-monthly-cost').textContent = formatCurrency(stats.AZURE.cost);
            document.getElementById('oci-account-count').textContent = stats.OCI.count;
            document.getElementById('oci-monthly-cost').textContent = formatCurrency(stats.OCI.cost);
        }
    } catch (e) {
        showToast('Error syncing accounts configuration.', 'error');
    }
}

async function syncSingleAccount(id) {
    showToast('Force refreshing account metrics...', 'warning');
    try {
        const res = await fetch(`/api/accounts/${id}/sync`, { method: 'POST' });
        if (res.ok) {
            showToast('Account synchronized successfully.', 'success');
            loadAccountsData();
        } else {
            showToast('Synchronization failed.', 'error');
        }
    } catch (e) {
        showToast('Network error during force sync.', 'error');
    }
}

async function deleteSingleAccount(id) {
    if (!confirm('Are you sure you want to delete this cloud account? All historical cost records will be removed.')) {
        return;
    }
    try {
        const res = await fetch(`/api/accounts/${id}`, { method: 'DELETE' });
        if (res.ok) {
            showToast('Cloud account connection terminated.', 'success');
            loadAccountsData();
        } else {
            showToast('Could not delete account.', 'error');
        }
    } catch (e) {
        showToast('Network error during deletion.', 'error');
    }
}

// 3. Budgets Tab Data
async function loadBudgetsData() {
    try {
        const res = await fetch('/api/budgets');
        if (res.ok) {
            AppState.budgets = await res.json();
            
            // Build Budgets table
            const tbody = els.budgetsTableBody;
            tbody.innerHTML = '';

            // Build visual progress bars
            const progressContainer = els.budgetProgressCards;
            progressContainer.innerHTML = '';

            if (AppState.budgets.length > 0) {
                AppState.budgets.forEach(b => {
                    const scope = b.cloudAccount ? `${b.cloudAccount.name} (${b.cloudAccount.provider})` : 'Global (Multi-Cloud)';
                    const spentPct = Math.round((b.currentSpent / b.limitAmount) * 100);
                    const alertThreshold = b.thresholdPercentage;
                    
                    tbody.innerHTML += `
                        <tr>
                            <td><strong>${b.name}</strong></td>
                            <td>${scope}</td>
                            <td>${formatCurrency(b.limitAmount)}</td>
                            <td>${b.thresholdPercentage}% ($${Math.round(b.limitAmount * (b.thresholdPercentage/100))})</td>
                            <td><strong>${formatCurrency(b.currentSpent)}</strong></td>
                            <td><i class="fa-solid ${b.emailNotification ? 'fa-circle-check text-emerald' : 'fa-circle-xmark text-muted'}" style="font-size: 1.1rem;"></i></td>
                            <td>
                                <button class="btn btn-secondary btn-sm py-1 px-2 mr-2" onclick="triggerBudgetEmail(${b.id})" title="Trigger simulated email alert" style="background: rgba(99, 102, 241, 0.15); border: 1px solid rgba(99, 102, 241, 0.3); color: #818cf8;">
                                    <i class="fa-solid fa-bell"></i> Trigger Alert
                                </button>
                                <button class="btn btn-danger btn-sm py-1 px-2" onclick="deleteSingleBudget(${b.id})" title="Delete budget">
                                    <i class="fa-solid fa-trash-can"></i>
                                </button>
                            </td>
                        </tr>
                    `;

                    // Progress Fill colors
                    let statusClass = 'safe';
                    if (b.currentSpent >= b.limitAmount) {
                        statusClass = 'breached';
                    } else if (b.currentSpent >= b.limitAmount * (b.thresholdPercentage/100)) {
                        statusClass = 'warn';
                    }

                    // Render grid card
                    progressContainer.innerHTML += `
                        <div class="budget-monitor-card">
                            <div class="bm-header">
                                <h3>${b.name}</h3>
                                <span class="bm-scope">${b.cloudAccount ? b.cloudAccount.provider : 'Global'}</span>
                            </div>
                            <div class="bm-amounts">
                                <span class="bm-spent">${formatCurrency(b.currentSpent)}</span>
                                <span class="bm-limit">of ${formatCurrency(b.limitAmount)}</span>
                            </div>
                            <div class="progress-track">
                                <div class="progress-fill ${statusClass}" style="width: ${Math.min(spentPct, 100)}%;"></div>
                            </div>
                            <div class="bm-footer">
                                <span>${spentPct}% Consumed</span>
                                <span>Threshold Alert: ${alertThreshold}%</span>
                            </div>
                            <div style="display: flex; justify-content: flex-end; margin-top: 10px;">
                                <button class="btn btn-secondary btn-sm" onclick="triggerBudgetEmail(${b.id})" title="Simulate alert email" style="font-size: 0.72rem; padding: 3px 8px; border-radius: 4px; background: rgba(99, 102, 241, 0.15); border: 1px solid rgba(99, 102, 241, 0.3); color: #818cf8;">
                                    <i class="fa-solid fa-envelope"></i> Trigger Email Alert
                                </button>
                            </div>
                        </div>
                    `;
                });
            } else {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="7" class="text-center">No budgets configured yet. Click "Set Budget" to monitor spend thresholds.</td>
                    </tr>
                `;
                progressContainer.innerHTML = `
                    <div class="empty-state" style="grid-column: span 3;">
                        <i class="fa-solid fa-piggy-bank" style="font-size: 3rem; margin-bottom:12px;"></i>
                        <p>Configure budgets to monitor spending limits across accounts</p>
                    </div>
                `;
            }
        }
    } catch (e) {
        showToast('Error syncing cost budgets data.', 'error');
    }
}

async function deleteSingleBudget(id) {
    if (!confirm('Are you sure you want to delete this budget?')) return;
    try {
        const res = await fetch(`/api/budgets/${id}`, { method: 'DELETE' });
        if (res.ok) {
            showToast('Budget monitoring policy deleted.', 'success');
            loadBudgetsData();
        }
    } catch (e) {
        showToast('Could not delete budget.', 'error');
    }
}

// 4. Analytics Tab Data
async function loadAnalyticsData() {
    // Populate filter account select
    const select = els.filterAccount;
    select.innerHTML = '<option value="">All Accounts</option>';
    AppState.accounts.forEach(acc => {
        select.innerHTML += `<option value="${acc.id}">${acc.name} (${acc.provider})</option>`;
    });

    // Populate dates defaults
    const todayStr = new Date().toISOString().slice(0,10);
    const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().slice(0,10);
    els.filterStartDate.value = thirtyDaysAgo;
    els.filterEndDate.value = todayStr;

    triggerAnalyticsFilter();
}

async function triggerAnalyticsFilter() {
    const accId = els.filterAccount.value;
    const start = els.filterStartDate.value;
    const end = els.filterEndDate.value;

    let url = `/api/dashboard/charts?startDate=${start}&endDate=${end}`;
    if (accId) url += `&accountId=${accId}`;

    try {
        const res = await fetch(url);
        if (res.ok) {
            const data = await res.json();
            
            // Render Trend
            renderDailyTrendChart('analytics-daily-trend', data.dailyTrend, null, 'analyticsTrend');

            // Render Service utilization records
            // We can fetch granular details by query or extract services.
            // Let's populate the service utilizing tables from the service breakdown
            const tbody = els.analyticsServiceTableBody;
            tbody.innerHTML = '';
            
            // Generate pseudo service details based on the breakdown dataset to populate table
            if (data.byService.length > 0) {
                data.byService.forEach(s => {
                    // Match provider & type properties based on name to show correct tags
                    const isAws = s.service.startsWith('Amazon') || s.service.includes('EC2') || s.service.includes('S3') || s.service.includes('RDS') || s.service.includes('CloudFront');
                    const isAzure = s.service.startsWith('Azure') || s.service.includes('Virtual') || s.service.includes('Blob') || s.service.includes('SQL');
                    const provider = isAws ? 'AWS' : (isAzure ? 'AZURE' : 'OCI');
                    
                    const isCompute = s.service.includes('EC2') || s.service.includes('Virtual Machines') || s.service.includes('Compute');
                    const isStorage = s.service.includes('S3') || s.service.includes('Blob') || s.service.includes('Object');
                    const isDb = s.service.includes('RDS') || s.service.includes('SQL Database') || s.service.includes('Autonomous');
                    const category = isCompute ? 'Compute' : (isStorage ? 'Storage' : (isDb ? 'Database' : 'Network'));

                    tbody.innerHTML += `
                        <tr>
                            <td><strong>${s.service}</strong></td>
                            <td><span class="badge-status connected">${provider}</span></td>
                            <td>${category}</td>
                            <td>${start} to ${end}</td>
                            <td>${Math.round(s.cost * 12.5)}</td>
                            <td>Units</td>
                            <td><strong>${formatCurrency(s.cost)}</strong></td>
                        </tr>
                    `;
                });
            } else {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="7" class="text-center">No utilization records found for selected period.</td>
                    </tr>
                `;
            }
        }
    } catch (e) {
        showToast('Failed to apply filter query.', 'error');
    }
}

els.btnApplyFilters.addEventListener('click', triggerAnalyticsFilter);

// 5. Reports Tab Data
async function loadReportsData() {
    const tbody = els.reportsTableBody;
    tbody.innerHTML = '<tr><td colspan="7" class="text-center">Aggregating historical ledger reports...</td></tr>';

    try {
        const res = await fetch('/api/dashboard/reports');
        if (res.ok) {
            const reports = await res.json();
            tbody.innerHTML = '';
            
            reports.forEach(r => {
                tbody.innerHTML += `
                    <tr>
                        <td><strong>${r.month}</strong></td>
                        <td>${formatCurrency(r.awsCost)}</td>
                        <td>${formatCurrency(r.azureCost)}</td>
                        <td>${formatCurrency(r.ociCost)}</td>
                        <td><strong>${formatCurrency(r.totalCost)}</strong></td>
                        <td>${r.activeAccountsCount}</td>
                        <td>${formatCurrency(r.avgDailyCost)}</td>
                    </tr>
                `;
            });
        } else {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center">Failed to fetch ledger reports from server.</td></tr>';
        }
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center">Connection error while retrieving reports.</td></tr>';
    }
}

// Simulated PDF/CSV Exports
els.btnExportCsv.addEventListener('click', () => {
    showToast('Compiling cost ledger... Exporting CSV sheet (download initialized).', 'success');
    window.location.href = '/api/dashboard/export';
});

els.btnExportPdf.addEventListener('click', () => {
    showToast('Assembling billing records... Exporting PDF statement (download initialized).', 'success');
});

// ----------------------------------------------------
// EXTENDED WIDGET LOADING FUNCTIONS
// ----------------------------------------------------

async function loadAnomaliesData() {
    try {
        const res = await fetch('/api/dashboard/anomalies');
        if (res.ok) {
            const anomalies = await res.json();
            const listEl = els.dashboardAnomaliesList;
            listEl.innerHTML = '';
            
            if (anomalies.length > 0) {
                anomalies.slice(0, 5).forEach(a => {
                    const isCritical = a.severity === 'CRITICAL';
                    const iconClass = isCritical ? 'fa-solid fa-circle-radiation' : 'fa-solid fa-triangle-exclamation';
                    const colorClass = isCritical ? 'text-rose' : 'text-warning';
                    
                    listEl.innerHTML += `
                        <div class="alert-item" style="background: rgba(245, 158, 11, 0.03); border-color: rgba(245, 158, 11, 0.15);">
                            <div class="alert-info">
                                <i class="${iconClass} ${colorClass}" style="font-size: 1.3rem;"></i>
                                <div class="alert-info-desc">
                                    <h4 style="color: #f59e0b; display: flex; align-items: center; gap: 8px;">
                                        Cost Anomaly Spurred 
                                        <span class="alert-tag" style="background: ${isCritical ? 'rgba(239, 68, 68, 0.2)' : 'rgba(245, 158, 11, 0.2)'}; color: ${isCritical ? '#fca5a5' : '#fde047'}; font-size: 0.68rem; padding: 1px 6px;">${a.severity}</span>
                                    </h4>
                                    <p>${a.description}</p>
                                </div>
                            </div>
                            <div class="alert-actions">
                                <span class="alert-time">${a.date}</span>
                            </div>
                        </div>
                    `;
                });
            } else {
                listEl.innerHTML = `
                    <div class="empty-state">
                        <i class="fa-regular fa-circle-check text-emerald" style="font-size: 2.2rem;"></i>
                        <p>No abnormal cost deviations detected in the past 90 days.</p>
                    </div>
                `;
            }
        }
    } catch (e) {
        console.error("Failed to load anomalies logs", e);
    }
}

async function loadResourcesData() {
    const select = els.resFilterAccount;
    select.innerHTML = '<option value="">All Accounts</option>';
    AppState.accounts.forEach(acc => {
        select.innerHTML += `<option value="${acc.id}">${acc.name} (${acc.provider})</option>`;
    });

    triggerResourcesFilter();
}

async function triggerResourcesFilter() {
    const accId = els.resFilterAccount.value;
    const resType = els.resFilterType.value;

    let url = '/api/resources';
    if (accId) {
        url = `/api/resources/account/${accId}`;
    }

    try {
        const res = await fetch(url);
        if (res.ok) {
            let resources = await res.json();

            if (resType) {
                resources = resources.filter(r => r.resourceType.toLowerCase() === resType.toLowerCase());
            }

            const tbody = els.resourcesTableBody;
            tbody.innerHTML = '';

            if (resources.length > 0) {
                resources.forEach(r => {
                    let tagsHtml = '';
                    if (r.tags) {
                        r.tags.split(',').forEach(t => {
                            const parts = t.split('=');
                            const key = parts[0] || '';
                            const val = parts[1] || '';
                            tagsHtml += `<span class="alert-tag" style="background: rgba(255, 255, 255, 0.05); color: var(--text-secondary); margin-right: 4px; padding: 2px 6px; font-size: 0.72rem; border-radius: 4px; border: 1px solid var(--border-light);">${key}:${val}</span>`;
                        });
                    }

                    const isRunning = r.status.toLowerCase() === 'running';
                    const statusBadge = isRunning ? '<span class="badge-status connected">RUNNING</span>' 
                                                  : '<span class="badge-status failed">STOPPED</span>';

                    tbody.innerHTML += `
                        <tr>
                            <td><strong>${r.name}</strong></td>
                            <td><code>${r.resourceId}</code></td>
                            <td>${r.resourceType}</td>
                            <td><code>${r.sizeType}</code></td>
                            <td>${r.region}</td>
                            <td>${statusBadge}</td>
                            <td><strong>${formatCurrency(r.dailyCost)}</strong></td>
                            <td>${tagsHtml}</td>
                        </tr>
                    `;
                });
            } else {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="8" class="text-center">No active cloud resources match selected criteria.</td>
                    </tr>
                `;
            }
        }
    } catch (e) {
        showToast('Encountered errors loading resource inventories.', 'error');
    }
}

if (els.btnApplyResFilters) {
    els.btnApplyResFilters.addEventListener('click', triggerResourcesFilter);
}

async function triggerBudgetEmail(budgetId) {
    showToast('Dispatching budget alert simulation...', 'success');
    try {
        const res = await fetch(`/api/budgets/${budgetId}/trigger-email`, {
            method: 'POST'
        });
        const data = await res.json();
        if (res.ok) {
            showToast(data.message || 'Alert email simulated! Check "Simulated Inbox" tab to view.', 'success');
            // Refresh alert count and dashboard alerts list
            loadDashboardData();
        } else {
            showToast(data.message || 'Failed to trigger email alert.', 'error');
        }
    } catch (err) {
        showToast('Connection to server failed while dispatching email alert.', 'error');
    }
}

let liveCounterInterval = null;
let currentDisplaySpend = 0.0;
let eventSource = null;

function initRealtimeCosts(initialSpent) {
    if (liveCounterInterval) clearInterval(liveCounterInterval);
    if (eventSource) {
        eventSource.close();
        eventSource = null;
    }

    currentDisplaySpend = initialSpent;
    const kpiCard = els.kpiMonthlySpent;
    if (!kpiCard) return;

    let liveBadge = kpiCard.parentElement.querySelector('.live-indicator');
    if (!liveBadge) {
        liveBadge = document.createElement('div');
        liveBadge.className = 'live-indicator';
        liveBadge.innerHTML = '<span class="pulse-green" style="display:inline-block; width:8px; height:8px; background:#10b981; border-radius:50%; margin-right:6px; animation: live-blink 1s infinite alternate;"></span> LIVE';
        liveBadge.style = 'position: absolute; top: 12px; right: 12px; font-size: 0.68rem; background: rgba(16, 185, 129, 0.15); color: #10b981; padding: 2px 8px; border-radius: 99px; display: flex; align-items: center; font-weight: 600;';
        kpiCard.parentElement.style.position = 'relative';
        kpiCard.parentElement.appendChild(liveBadge);
    }

    if (!document.getElementById('live-blink-style')) {
        const style = document.createElement('style');
        style.id = 'live-blink-style';
        style.innerHTML = `
            @keyframes live-blink {
                0% { opacity: 0.4; transform: scale(0.9); }
                100% { opacity: 1; transform: scale(1.1); box-shadow: 0 0 6px #10b981; }
            }
        `;
        document.head.appendChild(style);
    }

    // Local client tick: ~$4000/month rate increments ($0.00018 per 100ms)
    liveCounterInterval = setInterval(() => {
        currentDisplaySpend += 0.00018;
        kpiCard.textContent = formatCurrency(currentDisplaySpend);
    }, 100);

    // Sync periodically with backend Server-Sent Events
    try {
        eventSource = new EventSource('/api/dashboard/realtime-stream');
        
        eventSource.addEventListener('cost-update', (e) => {
            try {
                const data = JSON.parse(e.data);
                currentDisplaySpend = data.liveMonthlySpent;
                kpiCard.textContent = formatCurrency(currentDisplaySpend);

                // Quick visual pulse effect on AWS stat as a sign of live data sync
                const awsVal = document.getElementById('aws-monthly-cost');
                if (awsVal) {
                    awsVal.style.transition = 'color 0.3s ease';
                    awsVal.style.color = '#ff9900';
                    setTimeout(() => { awsVal.style.color = ''; }, 400);
                }
            } catch (err) {
                console.error("Failed to parse live event data", err);
            }
        });

        eventSource.addEventListener('sync-progress', (e) => {
            try {
                const data = JSON.parse(e.data);
                handleSyncProgressUpdate(data);
            } catch (err) {
                console.error("Failed to parse sync progress event", err);
            }
        });

        eventSource.onerror = (err) => {
            console.warn("Live stream interrupted, attempting reconnection...");
            eventSource.close();
            setTimeout(() => { initRealtimeCosts(currentDisplaySpend); }, 6000);
        };
    } catch (err) {
        console.error("Could not build EventSource connection", err);
    }
}

function handleSyncProgressUpdate(data) {
    const statusText = data.status.replace(/_/g, ' ');
    const displayMsg = `🔄 Real-time Integration for "${data.name}": [${data.progress}%] ${statusText}`;
    
    if (data.status === 'COMPLETED') {
        showToast(`✅ "${data.name}" integrated successfully! Refreshing dashboard.`, 'success');
        loadDashboardData();
        if (AppState.activePanel === 'panel-accounts') loadAccountsData();
        if (AppState.activePanel === 'panel-resources') loadResourcesData();
    } else if (data.status === 'FAILED') {
        showToast(`❌ Sync failed for "${data.name}". Check credentials.`, 'error');
        if (AppState.activePanel === 'panel-accounts') loadAccountsData();
    } else {
        showToast(displayMsg, 'info');
    }
}

let selectedEmailId = null;

async function loadSandboxEmails() {
    const list = els.emailInboxList;
    if (!list) return;
    list.innerHTML = '<div class="text-center text-muted" style="padding: 40px 10px; font-size: 0.8rem;">Loading messages...</div>';

    try {
        const res = await fetch('/api/emails/sent');
        if (res.ok) {
            const emails = await res.json();
            list.innerHTML = '';

            if (emails.length === 0) {
                list.innerHTML = '<div class="text-center text-muted" style="padding: 40px 10px; font-size: 0.8rem;">Your simulated inbox is empty. Trigger a budget alert to see notifications here.</div>';
                els.emailSubjectView.textContent = "Select an email to read";
                els.emailSenderView.textContent = "From: Maheshwari Cloud Alerts";
                els.emailDateView.textContent = "";
                els.emailBodyView.textContent = "Please select a budget alert email from the list on the left to read its contents.";
                return;
            }

            emails.forEach((email, idx) => {
                const card = document.createElement('div');
                card.style = `padding: 12px; border-radius: 6px; border: 1px solid rgba(255, 255, 255, 0.08); background: ${selectedEmailId === email.id || (!selectedEmailId && idx === 0) ? 'rgba(99, 102, 241, 0.15)' : 'rgba(255,255,255,0.02)'}; border-color: ${selectedEmailId === email.id || (!selectedEmailId && idx === 0) ? '#6366f1' : 'rgba(255,255,255,0.08)'}; cursor: pointer; transition: all 0.2s ease; margin-bottom: 8px;`;
                
                const date = new Date(email.sentAt);
                const timeStr = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) + " " + date.toLocaleDateString();

                card.innerHTML = `
                    <div style="font-weight: 600; font-size: 0.82rem; color: #f8fafc; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${email.subject}</div>
                    <div style="font-size: 0.72rem; color: var(--text-secondary); margin-bottom: 2px;">To: ${email.recipient}</div>
                    <div style="font-size: 0.65rem; color: var(--text-muted); text-align: right;">${timeStr}</div>
                `;

                card.addEventListener('click', () => {
                    selectedEmailId = email.id;
                    Array.from(list.children).forEach(c => {
                        c.style.background = 'rgba(255,255,255,0.02)';
                        c.style.borderColor = 'rgba(255,255,255,0.08)';
                    });
                    card.style.background = 'rgba(99, 102, 241, 0.15)';
                    card.style.borderColor = '#6366f1';

                    displaySingleEmail(email);
                });

                list.appendChild(card);
            });

            const firstEmail = emails.find(e => e.id === selectedEmailId) || emails[0];
            selectedEmailId = firstEmail.id;
            displaySingleEmail(firstEmail);
        } else {
            list.innerHTML = '<div class="text-center text-muted" style="padding: 40px 10px; font-size: 0.8rem;">Failed to fetch messages.</div>';
        }
    } catch (e) {
        list.innerHTML = '<div class="text-center text-muted" style="padding: 40px 10px; font-size: 0.8rem;">Connection error.</div>';
    }
}

function displaySingleEmail(email) {
    if (!email) return;
    els.emailSubjectView.textContent = email.subject;
    els.emailSenderView.textContent = `To: ${email.recipient}`;
    
    const date = new Date(email.sentAt);
    els.emailDateView.textContent = date.toLocaleString();
    
    els.emailBodyView.textContent = email.body;
}
if (els.btnClearInbox) {
    els.btnClearInbox.addEventListener('click', async () => {
        try {
            const res = await fetch('/api/emails/sent/clear', { method: 'DELETE' });
            if (res.ok) {
                showToast('Inbox cleared!', 'success');
                selectedEmailId = null;
                loadSandboxEmails();
            } else {
                showToast('Failed to clear inbox.', 'error');
            }
        } catch (err) {
            showToast('Network error while clearing inbox.', 'error');
        }
    });
}

// ----------------------------------------------------
// ON WINDOW LOAD INIT
// ----------------------------------------------------
window.addEventListener('DOMContentLoaded', () => {
    checkAuthStatus();
});
