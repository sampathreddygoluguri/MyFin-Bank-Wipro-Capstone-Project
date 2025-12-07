import { authFetch, getCurrentCustomerId } from "/assets/js/auth.js";

function showMessage(type, text) {
    const box = document.getElementById("alertBox");
    box.classList.remove("alert-success", "alert-error");

    if (type === "success") box.classList.add("alert-success");
    else box.classList.add("alert-error");

    box.textContent = text;
    box.style.display = "block";

    setTimeout(() => box.style.display = "none", 3000);
}

async function loadLoansForCustomer() {
    const id = getCurrentCustomerId();
    if (!id) return window.location.href = "/customer/login.html";

    try {
        const res = await authFetch(`/api/customers/loans/customer/${id}`, {}, "CUSTOMER");
        if (!res.ok) return showNoLoans();

        const loans = await res.json();
        renderLoans(loans || []);
    } catch (e) {
        console.error("Loan load error", e);
        showNoLoans();
    }
}

function showNoLoans() {
    document.getElementById("noLoansBox").style.display = "block";
    document.getElementById("loanSection").style.display = "none";
}

function renderLoans(loans) {
    if (loans.length === 0) return showNoLoans();

    const section = document.getElementById("loanSection");
    const tbody = document.getElementById("loanTable");
    section.style.display = "block";
    tbody.innerHTML = "";

    loans.forEach(l => {
        const css =
            l.status === "APPROVED" ? "status-approved" :
            l.status === "PENDING"  ? "status-pending" :
            l.status === "DENIED"   ? "status-denied" : "status-closed";

			const action =
			    l.status === "APPROVED"
			        ? `<button class="btn btn-primary btn-sm"
			              onclick="openPayLoanModal(${l.id}, ${l.remainingAmount || 0}, ${l.emi || 0}, ${l.totalPaid || 0})">
			              Pay
			           </button>`
			        : (l.status === "PENDING" ? "Processing" : "-");


        tbody.innerHTML += `
            <tr>
                <td>${l.id}</td>
                <td>₹${l.amount}</td>
                <td>${l.annualInterestRate}%</td>
                <td>${l.months}</td>
				<td>₹${(l.emi || 0).toFixed(2)}</td>
				<td>₹${(l.totalPaid || 0).toFixed(2)}</td>
				<td>₹${(l.remainingAmount || 0).toFixed(2)}</td>
                <td class="${css}">${l.status}</td>
                <td>${action}</td>
            </tr>
        `;
    });
}

/* APPLY LOAN MODAL */
window.openApplyLoanModal = () => {
    document.getElementById("applyLoanModal").style.display = "flex";
};
window.closeApplyLoanModal = () => {
    document.getElementById("applyLoanModal").style.display = "none";
};

window.submitLoanApplication = async () => {
    const id = getCurrentCustomerId();
    const amount = Number(document.getElementById("loanAmount").value);
    const rate = Number(document.getElementById("loanRate").value);
    const months = Number(document.getElementById("loanMonths").value);

    const res = await authFetch(`/api/customers/loans/apply`, {
        method: "POST",
        body: JSON.stringify({
            customerId: id,
            amount,
            annualInterestRate: rate,   // FIXED 🔥
            months
        })
    }, "CUSTOMER");

    const data = await res.json();

    if (!res.ok) {
        showMessage("error", data.message || "Failed to apply loan");
        return;
    }

    showMessage("success", "Loan applied successfully!");

    setTimeout(() => location.reload(), 1500);
};


/* PAY LOAN MODAL */
window.openPayLoanModal = (loanId, remaining, emi) => {
    document.getElementById("payLoanId").textContent = loanId;
    document.getElementById("payRemaining").textContent = "₹" + remaining;
    document.getElementById("payEmi").textContent = "₹" + emi;
    document.getElementById("payLoanModal").style.display = "flex";
};

window.closePayLoanModal = () => {
    document.getElementById("payLoanModal").style.display = "none";
};

window.submitLoanPayment = async () => {
    const loanId = Number(document.getElementById("payLoanId").textContent);
    const amount = Number(document.getElementById("payLoanAmount").value);

    const res = await authFetch(`/api/customers/loans/${loanId}/pay`, {
        method: "POST",
        body: JSON.stringify({ amount })
    }, "CUSTOMER");

    if (!res.ok) {
        showMessage("error", "Payment failed");
        return;
    }

    showMessage("success", "Payment successful!");
    setTimeout(() => location.reload(), 1500);
};

document.addEventListener("DOMContentLoaded", loadLoansForCustomer);
