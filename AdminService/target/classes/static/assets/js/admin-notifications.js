import { authFetch } from "/assets/js/auth.js";

let notifications = []; // global

document.addEventListener("DOMContentLoaded", () => {
    setupBellClickHandler();
    loadAndRenderNotifications();
});

async function loadAndRenderNotifications() {
    const dropdown = document.getElementById("notif-dropdown");
    const badge = document.getElementById("notif-count");
    const dot = document.getElementById("notif-dot");

    if (!dropdown) return;

    try {
        const res = await authFetch("/admin/notifications/all", {}, "ADMIN");
        notifications = await res.json();

        dropdown.innerHTML = "";

        if (notifications.length === 0) {
            dropdown.innerHTML = `<div class="notif-empty">No notifications</div>`;
        }

        notifications.slice(0, 10).forEach(n => {
            const item = document.createElement("div");
            item.className = "notif-item";
            item.innerHTML = `
                <div style="
                    font-weight:${n.seen ? 'normal' : 'bold'};
                    opacity:${n.seen ? '0.6' : '1'};
                    padding:8px;">
                    ${n.message}
                </div>
                <div class="notif-time" style="font-size:12px; color:#666; padding:0 8px 8px;">
                    ${new Date(n.timestamp).toLocaleString()}
                </div>
            `;

            // Click marks only this notification as read
            item.addEventListener("click", async () => {
                if (!n.seen) {
                    await markAsSeen(n.id);
                }
                await loadAndRenderNotifications();
            });

            dropdown.appendChild(item);
        });

        // Update badge + dot
        const unreadCount = notifications.filter(n => !n.seen).length;

        if (unreadCount > 0) {
            badge.style.display = "inline-block";
            badge.textContent = unreadCount;
            dot.style.display = "inline-block";
        } else {
            badge.style.display = "none";
            dot.style.display = "none";
        }

    } catch (err) {
        console.error("Failed to load notifications:", err);
    }
}

function setupBellClickHandler() {
    const bell = document.getElementById("notif-bell");
    const dropdown = document.getElementById("notif-dropdown");

    if (!bell || !dropdown) return;

    bell.addEventListener("click", async () => {
        // Toggle dropdown
        dropdown.style.display =
            dropdown.style.display === "block" ? "none" : "block";

        // When dropdown is opened → mark all as seen
        if (dropdown.style.display === "block") {
            await markAllAsSeen();
            await loadAndRenderNotifications();
        }
    });
}

async function markAsSeen(id) {
    try {
        await authFetch(`/admin/notifications/${id}/mark-seen`, {
            method: "PUT"
        }, "ADMIN");
    } catch (err) {
        console.error("Failed to mark notification as seen", err);
    }
}

async function markAllAsSeen() {
    for (const n of notifications) {
        if (!n.seen) {
            await markAsSeen(n.id);
        }
    }
}
