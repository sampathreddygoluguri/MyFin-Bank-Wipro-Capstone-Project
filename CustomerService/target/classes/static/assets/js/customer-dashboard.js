// customer-dashboard.js
import { authFetch, getCurrentCustomerId } from "/assets/js/auth.js";

let primaryAccountNumber = null;
let primaryBalanceValue = null;
let customerIdGlobal = null;

/* ------------------ small helpers ------------------ */
function q(id){ return document.getElementById(id); }
function safeText(id, text){ const el = q(id); if(el) el.textContent = text; }

/* ------------------ balance toggle ------------------ */
function initBalanceToggle() {
  const eye = q("balanceEye");
  const balanceEl = q("balance");
  if (!eye || !balanceEl) return;

  if (eye._balanceInit) return;
  eye._balanceInit = true;

  eye.dataset.hidden = "true";

  function setMasked(masked) {
    if (masked) {
      balanceEl.textContent = "₹••••••";
      eye.textContent = "🙈";
      eye.dataset.hidden = "true";
    } else {
      balanceEl.textContent = primaryBalanceValue != null ? `₹${primaryBalanceValue}` : "₹0.00";
      eye.textContent = "👁️";
      eye.dataset.hidden = "false";
    }
  }

  eye.addEventListener("click", () => {
    const currentlyHidden = eye.dataset.hidden === "true";
    setMasked(!currentlyHidden);
  });

  setMasked(true);
}

/* ------------------ logout ------------------ */
window.logoutNow = function logoutNow() {
  try {
    localStorage.removeItem('myfin_customer_token');
    localStorage.removeItem('myfin_customer_id');
  } catch (e) {}
  window.location.href = "/customer/login.html";
};

/* ------------------ open / close helpers ------------------ */
function openOverlay(id){ const m = q(id); if (m) m.style.display = "flex"; }
function closeOverlay(id){ const m = q(id); if (m) m.style.display = "none"; }

window.openDepositModal = () => { const el = q("depositModal"); if (el) openOverlay("depositModal"); else window.location.href = "/customer/deposit.html"; };
window.closeDepositModal = () => closeOverlay("depositModal");

window.openWithdrawModal = () => { const el = q("withdrawModal"); if (el) openOverlay("withdrawModal"); else window.location.href = "/customer/withdraw.html"; };
window.closeWithdrawModal = () => closeOverlay("withdrawModal");

window.openTransferModal = () => { const el = q("transferModal"); if (el) openOverlay("transferModal"); else window.location.href = "/customer/transfer.html"; };
window.closeTransferModal = () => closeOverlay("transferModal");

window.openCreateFdModal = () => { const el = q("fdModal"); if (el) openOverlay("fdModal"); else window.location.href = "/customer/dashboard.html#fd"; };
window.closeFdModal = () => closeOverlay("fdModal");

window.openCreateRdModal = () => { const el = q("rdModal"); if (el) openOverlay("rdModal"); else window.location.href = "/customer/dashboard.html#rd"; };
window.closeRdModal = () => closeOverlay("rdModal");

/* ------------------ clock ------------------ */
function startDashboardClock() {
  const clock = q("dashboardClock");
  if (!clock) return;
  function update(){ clock.textContent = new Date().toLocaleTimeString('en-GB'); }
  update();
  setInterval(update, 1000);
}

/* ------------------ main load ------------------ */
async function loadDashboard() {
  startDashboardClock();
  initBalanceToggle();

  const customerId = getCurrentCustomerId();
  customerIdGlobal = customerId;
  if (!customerId) { window.location.href = "/customer/login.html"; return; }

  try {
    // USER INFO
    const infoRes = await authFetch(`/api/customers/${customerId}`, {}, "CUSTOMER");
    if (!infoRes.ok) throw new Error('failed-user-info');
    const info = await infoRes.json();

    const fullName = (info.firstName || "") + (info.lastName ? ` ${info.lastName}` : "");
    safeText("welcome", `Welcome, ${fullName || info.email || "Customer"}`);
    safeText("emailLine", info.email || "-");
    safeText("phoneLine", info.phone || "-");

    const nameEl = q("userFullName");
    if (nameEl) nameEl.textContent = fullName.trim() || info.email || "";

    // ACCOUNT SECTION
    const accRes = await authFetch(`/api/customers/accountdetails/${customerId}`, {}, "CUSTOMER");

    if (!accRes.ok) {
      safeText("acctNumber", "-"); safeText("balance", "₹0.00");
      q("noAccountArea").style.display = "block";
      q("acctActions").style.display = "none";
      return;
    }

    const accounts = await accRes.json();
    if (!accounts || accounts.length === 0) {
      safeText("acctNumber", "-"); safeText("balance", "₹0.00");
      q("noAccountArea").style.display = "block";
      q("acctActions").style.display = "none";
      return;
    }

    const acct = accounts[0];
    primaryAccountNumber = acct.accountNumber;
    primaryBalanceValue = acct.balance ?? 0;

    safeText("acctNumber", acct.accountNumber);
    safeText("balance", "₹••••••");

    q("balanceEye").dataset.hidden = "true";
    q("balanceEye").textContent = "👁️";

    q("noAccountArea").style.display = "none";
    q("acctActions").style.display = "block";

    // fill modals
    if(q("depAcctNum")) q("depAcctNum").textContent = primaryAccountNumber;
    if(q("wdAcctNum")) q("wdAcctNum").textContent = primaryAccountNumber;
    if(q("trFrom")) q("trFrom").textContent = primaryAccountNumber;

    initBalanceToggle();

    // INVESTMENTS + TRANSACTIONS
    await loadInvestments();
    await loadTransactions(primaryAccountNumber);

    // initialize interest dropdown behavior
    initFDInterestLogic();
    initRDInterestLogic();

  } catch (e) {
    console.error("Dashboard load error", e);
    if(q("msg")) q("msg").textContent = "Failed to load dashboard.";
  }
}

/* ------------------ create account handler ------------------ */
function attachCreateHandler(customerId) {
  const btn = q("btnCreateAccount");
  if (!btn) return;

  btn.onclick = () => {
    const m = q("createAccountModal");
    if (m) openOverlay("createAccountModal");
    else window.location.href = "/customer/register.html";
  };

  const cancel = q("cancelCreateAccount");
  if (cancel) cancel.onclick = () => closeOverlay("createAccountModal");

  const confirm = q("confirmCreateAccount");
  if (!confirm) return;

  confirm.onclick = async () => {
    confirm.disabled = true;
    const type = q("acctType")?.value || "SAVINGS";
    const initial = Number(q("initialDeposit")?.value || 0);

    try {
      const res = await authFetch(`/api/customers/accounts`, {
        method: "POST",
        body: JSON.stringify({ customerId, type, initial })
      }, "CUSTOMER");

      const d = await res.json().catch(()=>({}));
      if (!res.ok) {
        if(q("modalMsg")) q("modalMsg").textContent = d.message || "Failed to create account";
        confirm.disabled = false;
        return;
      }

      closeOverlay("createAccountModal");
      primaryAccountNumber = d.accountNumber;
      primaryBalanceValue = d.balance || 0;
      safeText("acctNumber", d.accountNumber);
      safeText("balance", `₹${primaryBalanceValue}`);
      loadTransactions(d.accountNumber);

    } catch (err) {
      if(q("modalMsg")) q("modalMsg").textContent = "Network error";
    }

    confirm.disabled = false;
  };
}

/* ------------------ investments ------------------ */
async function loadInvestments() {
  try {
    const res = await authFetch(`/api/customers/investments`, {}, "CUSTOMER");
    if (!res.ok) return;

    const data = await res.json();
    const fds = data.fds || [];
    const rds = data.rds || [];

    if (fds.length === 0 && rds.length === 0) {
      q("noInvestmentsBox").style.display = "block";
    }

    // FD LIST
    if (fds.length > 0) {
      q("fdSection").style.display = "block";
      const tbody = q("fdTable");
      tbody.innerHTML = "";
      fds.forEach(fd => {
        const css = fd.status === "ACTIVE" ? "status-active" :
                    fd.status === "MATURED" ? "status-matured" : "status-closed";
        tbody.innerHTML += `
          <tr>
            <td>${fd.id}</td>
            <td>₹${fd.principal}</td>
            <td>${fd.annualRatePercent}%</td>
            <td>${fd.months}</td>
            <td>₹${fd.maturityAmount}</td>
            <td class="${css}">${fd.status}</td>
          </tr>`;
      });
    }

    // RD LIST
    if (rds.length > 0) {
      q("rdSection").style.display = "block";
      const tbody = q("rdTable");
      tbody.innerHTML = "";
      rds.forEach(rd => {
        const css =
          rd.status === "ACTIVE" ? "status-active" :
          rd.status === "COMPLETED" ? "status-completed" :
          rd.status === "OVERDUE" ? "status-overdue" :
          "status-closed";

        const payBtn = rd.status === "ACTIVE"
          ? `<button class='btn btn-sm btn-primary' onclick='payRd(${rd.id}, ${rd.monthlyInstallment})'>Pay</button>`
          : "-";

        tbody.innerHTML += `
          <tr>
            <td>${rd.id}</td>
            <td>₹${rd.monthlyInstallment}</td>
            <td>₹${rd.totalPaid}</td>
            <td>${rd.nextInstallmentDate}</td>
            <td class="${css}">${rd.status}</td>
            <td>${payBtn}</td>
          </tr>`;
      });
    }

  } catch (e) {
    console.warn("loadInvestments error", e);
  }
}

/* ------------------ transactions ------------------ */
async function loadTransactions(acctNum) {
  try {
    if (!acctNum) return;

    const res = await authFetch(`/api/customers/accounts/${acctNum}/transactions`, {}, "CUSTOMER");
    if (!res.ok) return;

    const txs = await res.json();
    const tbody = document.querySelector("#txTable tbody");
    tbody.innerHTML = "";

    if (!txs || txs.length === 0) {
      tbody.innerHTML = `<tr><td colspan="4">No recent transactions</td></tr>`;
      return;
    }

    txs.forEach(t => {
      const date = new Date(t.createdAt).toLocaleString();
      tbody.innerHTML += `
        <tr>
          <td>${date}</td>
          <td>${t.type}</td>
          <td>${t.remark || "-"}</td>
          <td>₹${t.amount}</td>
        </tr>`;
    });

  } catch (e) {
    console.warn("loadTransactions error", e);
  }
}

/* ------------------ Phone Update Modal ------------------ */
window.openPhoneUpdateModal = function () {
    q("phoneModal").style.display = "flex";
    const current = q("phoneLine")?.textContent || "";
    q("currentPhone").value = current;
};

window.closePhoneUpdateModal = function () {
    q("phoneModal").style.display = "none";
    q("newPhone").value = "";
    q("phoneUpdateMsg").textContent = "";
};

window.submitPhoneUpdate = async function () {
    const newPhone = q("newPhone").value.trim();
    if (!newPhone) {
        q("phoneUpdateMsg").textContent = "Enter new mobile number";
        return;
    }

    try {
        const res = await authFetch(`/api/customers/${customerIdGlobal}`, {
            method: "PUT",
            body: JSON.stringify({ phone: newPhone })
        }, "CUSTOMER");

        const data = await res.json().catch(()=>({}));

        if (!res.ok) {
            q("phoneUpdateMsg").textContent = data.message || "Failed to update";
            return;
        }

        // Update UI immediately
        q("phoneLine").textContent = newPhone;

        alert("Mobile number updated successfully!");
        closePhoneUpdateModal();

    } catch (e) {
        q("phoneUpdateMsg").textContent = "Network error";
    }
};

/* ------------------ deposit / withdraw / transfer submit ------------------ */
window.submitDeposit = async () => {
  const amt = Number(q("depAmount").value || 0);
  const confirm = (q("depConfirm").value || "").trim();
  const remark = (q("depRemark") && q("depRemark").value) ? q("depRemark").value.trim() : null;

  if (confirm !== primaryAccountNumber) { alert("Account number does not match!"); return; }
  try {
    const res = await authFetch(`/api/customers/accounts/deposit`, {
      method: "POST",
      body: JSON.stringify({ accountNumber: primaryAccountNumber, confirmAccountNumber: confirm, amount: amt, remark })
    }, "CUSTOMER");
    const d = await res.json().catch(()=>({}));
    if (!res.ok) { alert(d.message || "Deposit failed"); return; }
    alert("Deposit successful!");
    closeOverlay("depositModal");
    await loadDashboard();
  } catch (e) { alert("Network error"); }
};

window.submitWithdraw = async () => {
  const amt = Number(q("wdAmount").value || 0);
  const confirm = (q("wdConfirm").value || "").trim();
  const remark = (q("wdRemark") && q("wdRemark").value) ? q("wdRemark").value.trim() : null;

  if (confirm !== primaryAccountNumber) { alert("Account number does not match!"); return; }
  try {
    const res = await authFetch(`/api/customers/accounts/withdraw`, {
      method: "POST",
      body: JSON.stringify({ accountNumber: primaryAccountNumber, confirmAccountNumber: confirm, amount: amt, remark })
    }, "CUSTOMER");
    const d = await res.json().catch(()=>({}));
    if (!res.ok) { alert(d.message || "Withdraw failed"); return; }
    alert("Withdraw successful!");
    closeOverlay("withdrawModal");
    await loadDashboard();
  } catch (e) { alert("Network error"); }
};

window.submitTransfer = async () => {
  const to = (q("trTo").value || "").trim();
  const amt = Number(q("trAmount").value || 0);
  const remark = (q("trRemark") && q("trRemark").value) ? q("trRemark").value.trim() : null;

  if (!to) { alert("Enter recipient account number."); return; }
  try {
    const res = await authFetch(`/api/customers/accounts/transfer`, {
      method: "POST",
      body: JSON.stringify({ fromAccountNumber: primaryAccountNumber, toAccountNumber: to, amount: amt, remark })
    }, "CUSTOMER");
    const d = await res.json().catch(()=>({}));
    if (!res.ok) { alert(d.message || "Transfer failed"); return; }
    alert("Transfer successful!");
    closeOverlay("transferModal");
    await loadDashboard();
  } catch (e) { alert("Network error"); }
};

/* ------------------ FD submit ------------------ */
if (q("fdSubmit")) q("fdSubmit").onclick = async () => {
  const months = Number(q("fdMonths").value || 0);
  const initial = Number(q("fdAmount").value || 0);
  const useFull = q("fdUseFull").checked;

  try {
    const res = await authFetch(`/api/customers/investments/fd`, {
      method: "POST",
      body: JSON.stringify({ 
        accountNumber: primaryAccountNumber, 
        months, 
        useFullBalance: useFull, 
        initial: useFull ? null : initial 
      })
    }, "CUSTOMER");

    const d = await res.json().catch(()=>({}));
    if (!res.ok) { alert(d.message || "Failed to create FD"); return; }

    alert("FD created!");
    closeOverlay("fdModal");
    loadDashboard();

  } catch (e) { alert("Network error"); }
};

/* ------------------ RD submit ------------------ */
if (q("rdSubmit")) q("rdSubmit").onclick = async () => {
  const months = Number(q("rdMonths").value || 0);
  const monthly = Number(q("rdAmount").value || 0);

  try {
    const res = await authFetch(`/api/customers/investments/rd`, {
      method: "POST",
      body: JSON.stringify({ accountNumber: primaryAccountNumber, months, monthlyInstallment: monthly })
    }, "CUSTOMER");

    const d = await res.json().catch(()=>({}));
    if (!res.ok) { alert(d.message || "Failed to create RD"); return; }

    alert("RD created!");
    closeOverlay("rdModal");
    loadDashboard();

  } catch (e) { alert("Network error"); }
};

/* ------------------ pay RD ------------------ */
window.payRd = async (rdId, amount) => {
  try {
    const res = await authFetch(`/api/customers/investments/rd/${rdId}/pay`, {
      method: "POST",
      body: JSON.stringify({ rdId, amount })
    }, "CUSTOMER");

    if (!res.ok) { alert("Payment failed"); return; }

    alert("Installment paid!");
    loadDashboard();

  } catch (e) { alert("Network error"); }
};

/* ------------------ LOAN page navigation ------------------ */
window.goToLoanPage = () => {
  window.location.href = "/customer/loans.html";
};

/* ------------------------------------------------------------
  FD interest auto update
------------------------------------------------------------ */
function initFDInterestLogic() {
  const monthsEl = q("fdMonths");
  const rateLabel = q("fdInterest");

  if (!monthsEl || !rateLabel) return;

  function updateRate() {
    const selected = monthsEl.value;

    const rate =
      selected === "6"  ? 6 :
      selected === "12" ? 7 : 0;

    rateLabel.textContent = `Interest Rate: ${rate}% P.A`;
  }

  monthsEl.addEventListener("change", updateRate);
  updateRate();
}

/* ------------------------------------------------------------
   RD interest auto update
------------------------------------------------------------ */
function initRDInterestLogic() {
  const monthsEl = q("rdMonths");
  const rateLabel = q("rdInterest");

  if (!monthsEl || !rateLabel) return;

  function updateRate() {
    const selected = monthsEl.value;

    const rate =
      selected === "6"  ? 5 :
      selected === "12" ? 6 : 0;

    rateLabel.textContent = `Interest Rate: ${rate}% P.A`;
  }

  monthsEl.addEventListener("change", updateRate);
  updateRate();
}

/* ------------------ boot ------------------ */
window.addEventListener('DOMContentLoaded', () => {
  const cid = getCurrentCustomerId();
  if (cid) attachCreateHandler(cid);

  const topLogout = q("topLogoutBtn");
  if (topLogout) topLogout.addEventListener("click", logoutNow);

  initBalanceToggle();

  loadDashboard();
});
