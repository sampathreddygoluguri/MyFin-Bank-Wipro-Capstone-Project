const path = window.location.pathname;

// Pages that should use the minimal header/footer (no sidebar)
const SIMPLE_PAGES = [
  "/admin/login.html",
  "/admin/register.html",
  "/admin/admin-loans.html",
  "/customer/login.html",
  "/customer/register.html"
];

function insertSimpleHeaderFooter() {
  const h = document.getElementById("insert-header");
  if (h) {
    h.innerHTML = `
      <header class="bg-white shadow-sm p-3 text-center fw-bold text-primary">
        MyFin Bank
      </header>
    `;
  }

  const f = document.getElementById("insert-footer");
  if (f) {
    // fixed bottom footer — ensures it's always at bottom
    f.innerHTML = `
      <footer id="site-footer" class="bg-white border-top text-center p-3 small text-muted fixed-bottom">
        © ${new Date().getFullYear()} MyFin Bank • All rights reserved. • Developed For Capstone.
      </footer>
    `;
  }

  // add bottom padding so page content does not hide behind footer
  document.documentElement.style.setProperty('--app-footer-height', '72px'); // used by CSS
  document.body.style.paddingBottom = '72px';
}

function insertFullHeaderFooter() {
  const h = document.getElementById("insert-header");
  if (!h) return;

  const customerToken = localStorage.getItem('myfin_customer_token');
  const adminToken = localStorage.getItem('myfin_admin_token');

  const isLoggedIn = !!(customerToken || adminToken);

  h.innerHTML = `
    <div class="header">
      <div style="display:flex;justify-content:space-between;align-items:center;padding:12px 20px">
        <div class="brand fw-bold">MyFin Bank</div>
        <nav style="display:flex;align-items:center;gap:16px;">
          <a href="/" class="me-2">Home</a>
          <a href="/admin/login.html" class="me-2">Admin</a>
          ${adminToken ? `
            <div id="notif-area" style="position:relative; cursor:pointer;">
              <span id="notif-bell" style="font-size:20px; display:inline-block; padding:4px 6px;">🔔</span>
              <span id="notif-count" style="display:none; position:absolute; top:-6px; right:-6px; background:red; color:#fff; border-radius:50%; padding:2px 6px; font-size:12px;"></span>
              <span id="notif-dot" style="display:none; position:absolute; top:0px; right:-2px; width:10px; height:10px; background:red; border-radius:50%; box-shadow:0 0 0 2px white inset;"></span>
              <div id="notif-dropdown" style="display:none; position:absolute; right:0; top:32px; width:360px; background:white; border:1px solid #ddd; border-radius:8px; box-shadow:0 6px 18px rgba(0,0,0,0.12); z-index:100; max-height:420px; overflow-y:auto;"></div>
            </div>
          ` : ''}
          ${isLoggedIn ? `<button id="navLogout" class="btn btn-outline-secondary btn-sm">Logout</button>` : ''}
        </nav>
      </div>
    </div>
  `;

  const footerContainer = document.getElementById("insert-footer");
  if (footerContainer) {
    footerContainer.innerHTML = `
      <footer id="site-footer" class="bg-white border-top text-center p-3 small text-muted">
        © ${new Date().getFullYear()} MyFin Bank • Designed for Capstone project
      </footer>
    `;
  }

  // ensure page bottom padding equals footer height (for non-fixed footer we still add padding)
  document.documentElement.style.setProperty('--app-footer-height', '72px');
  document.body.style.paddingBottom = '72px';

  const logoutBtn = document.getElementById('navLogout');
  if (logoutBtn) {
    logoutBtn.onclick = () => {
      if (localStorage.getItem('myfin_admin_token')) {
        localStorage.removeItem('myfin_admin_token');
        localStorage.removeItem('myfin_admin_id');
      } else {
        localStorage.removeItem('myfin_customer_token');
        localStorage.removeItem('myfin_customer_id');
      }
      alert("You have been logged out successfully.");
      if (localStorage.getItem('myfin_admin_token')) {
        window.location.href = '/admin/login.html';
      } else {
        window.location.href = '/customer/login.html';
      }
    };
  }
}

// Run on DOM ready
document.addEventListener('DOMContentLoaded', () => {
  if (SIMPLE_PAGES.includes(path)) {
    insertSimpleHeaderFooter();
  } else {
    insertFullHeaderFooter();
  }
});
