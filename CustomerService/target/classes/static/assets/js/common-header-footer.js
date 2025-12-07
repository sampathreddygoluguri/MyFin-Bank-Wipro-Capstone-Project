// Detect which page we are on
function isAuthPage() {
  const path = window.location.pathname;
  return (
    path.includes("/customer/login") ||
    path.includes("/customer/register") ||
    path.includes("/admin/login") ||
    path.includes("/admin/register")
  );
}

const insertHeader = () => {
  const h = document.getElementById("insert-header");
  if (!h) return;

  const customerToken = localStorage.getItem("myfin_customer_token");
  const adminToken = localStorage.getItem("myfin_admin_token");
  const isLoggedIn = customerToken || adminToken;
  const authPage = isAuthPage();

  /* ============================================================
     HEADER FOR LOGIN / REGISTER PAGES  (CLEAN HEADER)
  ============================================================ */
  if (authPage) {
    h.innerHTML = `
      <div class="header" style="text-align:center; padding:20px;">
        <div class="brand" style="font-size:22px; font-weight:700;">
          MyFin Bank
        </div>
      </div>
    `;
    return;
  }

  /* ============================================================
     HEADER FOR LOGGED-IN USERS (DASHBOARD)
  ============================================================ */
  h.innerHTML = `
    <div class="header">
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <div class="brand">MyFin Bank</div>

        <nav>
          <a href="/customer/dashboard.html">Home</a>
          <a href="/customer/loans.html">Loans</a>
          <a href="/customer/profile.html">Profile</a>

          ${isLoggedIn ? `
            <button id="navLogout" class="btn secondary" style="margin-left:12px;">
              Logout
            </button>
          ` : ""}
        </nav>
      </div>
    </div>
  `;

  const logoutBtn = document.getElementById("navLogout");
  if (logoutBtn) {
    logoutBtn.onclick = () => {
      if (localStorage.getItem("myfin_admin_token")) {
        localStorage.removeItem("myfin_admin_token");
        localStorage.removeItem("myfin_admin_id");
        window.location.href = "/admin/login.html";
      } else {
        localStorage.removeItem("myfin_customer_token");
        localStorage.removeItem("myfin_customer_id");
        window.location.href = "/customer/login.html";
      }
    };
  }
};

const insertFooter = () => {
  const f = document.getElementById("insert-footer");
  if (!f) return;

  const authPage = isAuthPage();

  // Clean footer for login pages
  if (authPage) {
    f.innerHTML = `
      <div class="footer" style="text-align:center; padding:15px;">
        <div class="small">© 2025 MyFin Bank • Developed for Capstone Project</div>
      </div>
    `;
    return;
  }

  // Normal footer for dashboard pages
  f.innerHTML = `
    <div class="footer">
      <div class="container small">
        © 2025 MyFin Bank • Designed for Capstone Project
      </div>
    </div>
  `;
};

document.addEventListener("DOMContentLoaded", () => {
  insertHeader();
  insertFooter();
});
