// ---- SAFE HELPERS ----
function safeGet(key) {
  try { return localStorage.getItem(key); }
  catch (e) {
    console.warn("localStorage blocked:", e);
    return null;
  }
}

function safeSet(key, value) {
  try { localStorage.setItem(key, value); }
  catch (e) {
    console.warn("localStorage blocked:", e);
  }
}

function safeRemove(key) {
  try { localStorage.removeItem(key); }
  catch (e) {
    console.warn("localStorage blocked:", e);
  }
}

// ---- TOKEN STORAGE ----
export function saveToken(role, token, id) {
  if (role && role.toUpperCase().includes('ADMIN')) {
    safeSet('myfin_admin_token', token);
    if (id) safeSet('myfin_admin_id', id);
  } else {
    safeSet('myfin_customer_token', token);
    if (id) safeSet('myfin_customer_id', id);
  }
}

export function clearTokens() {
  safeRemove('myfin_admin_token');
  safeRemove('myfin_admin_id');
  safeRemove('myfin_customer_token');
  safeRemove('myfin_customer_id');
}

export function getCustomerToken() { return safeGet('myfin_customer_token'); }
export function getAdminToken() { return safeGet('myfin_admin_token'); }
export function getCurrentCustomerId() { return safeGet('myfin_customer_id'); }
export function getCurrentAdminId() { return safeGet('myfin_admin_id'); }

// ---- AUTH FETCH ----
export async function authFetch(url, opts = {}, role = 'AUTO') {
  opts.headers = opts.headers || {};

  let token = null;
  if (role === 'ADMIN') token = getAdminToken();
  else if (role === 'CUSTOMER') token = getCustomerToken();
  else token = getCustomerToken() || getAdminToken();

  if (token) opts.headers['Authorization'] = 'Bearer ' + token;

  if (opts.body && !(opts.headers['Content-Type'] || opts.headers['content-type'])) {
    opts.headers['Content-Type'] = 'application/json';
  }

  const res = await fetch(url, opts);

  if (res.status === 401 || res.status === 403) {
    clearTokens();
    if (role === 'ADMIN') window.location.href = '/admin/login.html';
    else window.location.href = '/customer/login.html';
  }

  return res;
}
