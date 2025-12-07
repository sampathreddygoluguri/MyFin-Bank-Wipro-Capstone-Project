import { authFetch, getCurrentAdminId } from '/assets/js/auth.js';

const token = localStorage.getItem('myfin_admin_token');
if(!token){
  window.location.href = '/admin/login.html';
}

document.addEventListener('DOMContentLoaded', () => {
  const welcomeEl = document.getElementById('welcome');
  if(welcomeEl) welcomeEl.textContent = "Welcome Admin (ID: " + (localStorage.getItem("myfin_admin_id") || "") + ")";
  loadCustomers();
});

async function loadCustomers(){
  const tableBody = document.querySelector("#customersTable tbody");
  if(!tableBody) return;
  tableBody.innerHTML = "<tr><td colspan='6' class='small'>Loading...</td></tr>";

  try {
    const res = await authFetch("/api/admin/customers", {}, "ADMIN");

    if(!res.ok){
      if(res.status === 401 || res.status === 403){
         localStorage.removeItem('myfin_admin_token');
         localStorage.removeItem('myfin_admin_id');
         window.location.href = '/admin/login.html';
         return;
      }
      tableBody.innerHTML = "<tr><td colspan='6' class='small'>Failed to load customers.</td></tr>";
      return;
    }

    const customers = await res.json();
    tableBody.innerHTML = '';

    customers.forEach(c => {
        const accountsSummary =
          (c.accounts && c.accounts.length > 0)
            ? c.accounts.map(a => `${a.accountNumber}: ₹${a.balance}`).join("<br>")
            : "<span class='small'>(no accounts)</span>";

        const row = document.createElement("tr");

        row.innerHTML = `
          <td>${c.id}</td>
          <td>${c.firstName || ''} ${c.lastName || ''}</td>
          <td>${c.email}</td>
          <td>${c.status}</td>
          <td>${accountsSummary}</td>
          <td style="display:flex;gap:6px;">
              ${
                c.status === "ACTIVE"
                ? `<button class="deactivate" data-id="${c.id}" data-action="deactivate">Deactivate</button>`
                : `<button class="activate" data-id="${c.id}" data-action="activate">Activate</button>`
              }
              <button class="chat-btn" data-id="${c.id}">Chat</button>
          </td>
        `;

        tableBody.appendChild(row);
    });

    // existing activate/deactivate listeners
    tableBody.querySelectorAll('button[data-action]').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const id = e.currentTarget.getAttribute('data-id');
        const action = e.currentTarget.getAttribute('data-action');
        await toggleCustomer(id, action);
      });
    });

    // NEW — chat button listener
    tableBody.querySelectorAll('.chat-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const customerId = e.currentTarget.getAttribute('data-id');
        await startChatFromDashboard(customerId);
      });
    });

  } catch(err){
    tableBody.innerHTML = "<tr><td colspan='6' class='small'>Network error.</td></tr>";
  }
}


async function toggleCustomer(id, action){
  try{
    const res = await authFetch(`/api/admin/customer/${id}/${action}`, { method: 'PUT' }, 'ADMIN');
    if(!res.ok){
      const d = await res.json().catch(()=>({}));
      alert('Failed: ' + (d.message || res.statusText));
      return;
    }
    alert('Operation successful');
    loadCustomers();
  } catch(e){
    alert('Network error');
  }
}

async function startChatFromDashboard(customerId) {
    const adminId = getCurrentAdminId();

    // 1. Create conversation via backend
    const res = await fetch("http://localhost:8095/api/chat/start", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ adminId, customerId })
    });

    const room = await res.json();
    const convId = room.id;

    // 2. Open chat widget
    const widget = document.getElementById("chat-widget");
    widget.style.display = "block";

    // 3. Call the chat-widget exposed global function
    if (window.openChatRoomFromOutside) {
        window.openChatRoomFromOutside(convId, customerId);
    } else {
        console.error("Chat widget function missing: openChatRoomFromOutside");
    }
}

