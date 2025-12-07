import { authFetch } from "/assets/js/auth.js";

let allLoans = []; // master list

document.addEventListener("DOMContentLoaded", () => {
    loadLoans();

    document.getElementById("searchCustomer").addEventListener("input", filterLoans);
    document.getElementById("statusFilter").addEventListener("change", filterLoans);
});

async function loadLoans() {
    const tableBody = document.querySelector("#loansTable tbody");
    tableBody.innerHTML = "<tr><td colspan='8'>Loading...</td></tr>";

    try {
        const res = await authFetch("/api/admin/loans", {}, "ADMIN");

        if (!res.ok) {
            tableBody.innerHTML = "<tr><td colspan='8'>Failed to load loans</td></tr>";
            return;
        }

        allLoans = await res.json();
        renderLoans(allLoans);

    } catch (e) {
        tableBody.innerHTML = "<tr><td colspan='8'>Error loading loans</td></tr>";
    }
}

function renderLoans(loans) {
    const tableBody = document.querySelector("#loansTable tbody");
    tableBody.innerHTML = "";

    if (loans.length === 0) {
        tableBody.innerHTML = "<tr><td colspan='8'>No loans found</td></tr>";
        return;
    }

    loans.forEach(loan => {
        const row = document.createElement("tr");
		const statusClass =
		    loan.status === "APPROVED" ? "status-approved" :
		    loan.status === "DENIED" ? "status-denied" :
		    "status-pending";


        const actionButtons =
            loan.status === "PENDING"
                ? `
                    <button class='approveBtn' data-id='${loan.id}'>Approve</button>
                    <button class='denyBtn' data-id='${loan.id}'>Deny</button>
                `
                : `<span class='small'>${loan.status}</span>`;

        row.innerHTML = `
            <td>${loan.id}</td>
            <td>${loan.customerId}</td>
			<td>${loan.accountNumber}</td>
            <td>₹${loan.amount}</td>
            <td>${loan.annualInterestRate}%</td>
            <td>${loan.months}</td>
            <td>₹${loan.emi.toFixed(2)}</td>
			<td class="${statusClass}">${loan.status}</td>
            <td>${actionButtons}</td>
        `;

        tableBody.appendChild(row);
    });

    // Action listeners
    document.querySelectorAll(".approveBtn").forEach(btn =>
        btn.addEventListener("click", () => updateLoan(btn.dataset.id, true))
    );
    document.querySelectorAll(".denyBtn").forEach(btn =>
        btn.addEventListener("click", () => updateLoan(btn.dataset.id, false))
    );
}

// Filtering Logic
function filterLoans() {
    const searchValue = document.getElementById("searchCustomer").value.trim();
    const statusValue = document.getElementById("statusFilter").value;

    let filtered = allLoans;

    if (searchValue) {
        filtered = filtered.filter(loan => loan.customerId.toString().includes(searchValue));
    }
    if (statusValue) {
        filtered = filtered.filter(loan => loan.status === statusValue);
    }

    renderLoans(filtered);
}

// Approve / Deny Loan
async function updateLoan(loanId, approve) {
    const remark = approve ? "Approved by Admin" : "Denied by Admin";

    try {
        const res = await authFetch(
            `/api/admin/loans/${loanId}/decide?approve=${approve}&remark=${remark}`,
            { method: "PUT" },
            "ADMIN"
        );

        if (!res.ok) {
            alert("Failed to update loan");
            return;
        }

        alert("Loan updated successfully");
        loadLoans();

    } catch (e) {
        alert("Network error");
    }
}
