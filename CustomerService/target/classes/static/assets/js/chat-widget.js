import { getAdminToken, getCustomerToken, getCurrentAdminId, getCurrentCustomerId } from "/assets/js/auth.js";

const CHAT_BASE = window.CHAT_SERVER || "http://localhost:8095";
let stompClient = null;

let conversationId = null;
let currentUserId = null;
let isAdmin = false;
let activeToId = null;
let conversations = [];
let activeSubscription = null;

function el(html) { const d = document.createElement("div"); d.innerHTML = html; return d.firstElementChild; }

export async function initChatWidget() {

    if (document.getElementById("chat-floating-btn")) return;

    const adminId = getCurrentAdminId();
    const custId = getCurrentCustomerId();

    if (adminId) {
        isAdmin = true;
        currentUserId = Number(adminId);
    } else if (custId) {
        isAdmin = false;
        currentUserId = Number(custId);
    } else {
        return;
    }

    const btn = el(`<div id="chat-floating-btn" title="Chat">💬</div>`);
    document.body.appendChild(btn);

    const widget = el(`
      <div class="chat-widget" id="chat-widget">

        <div class="chat-header">
          <div class="title">Chat with Admin</div>
          <div class="controls">
            <button id="chat-max">⬜</button>
            <button id="chat-min">—</button>
            <button id="chat-close">✕</button>
          </div>
        </div>

        <div class="chat-body">

            ${isAdmin ? `
              <div class="chat-convos" id="chat-convos"></div>
              <div class="chat-messages" id="chat-messages-panel">
            ` : `
              <div class="chat-messages" id="chat-messages-panel" style="width:100%">
            `}

                <div class="messages-list" id="messages-list"></div>

                <div class="chat-composer">
                  <input id="chat-input" placeholder="Type a message..." />
                  <button id="chat-send">Send</button>
                </div>

            </div>
        </div>
      </div>
    `);

    document.body.appendChild(widget);

    // ✅ THIS IS THE CORRECT PLACE TO APPLY FULL WIDTH FOR CUSTOMER
    if (!isAdmin) {
        const panel = document.getElementById("chat-messages-panel");
        if (panel) panel.style.width = "100%";
    }

    // -------------------- CONTROLS --------------------
    btn.addEventListener("click", async () => {
        widget.style.display = widget.style.display === "block" ? "none" : "block";
        if (widget.style.display === "block") {
            await connectAndInit();
        }
    });

    document.getElementById("chat-close").onclick = () => widget.style.display = "none";
    document.getElementById("chat-min").onclick = () => widget.style.display = "none";
    document.getElementById("chat-max").onclick = () => widget.classList.toggle("fullscreen");

    document.getElementById("chat-send").onclick = async () => {
        const input = document.getElementById("chat-input");
        const text = input.value.trim();
        if (!text) return;
        if (!activeToId) {
            alert("Select a conversation or wait for admin.");
            return;
        }
        await sendMessage(activeToId, text);
        input.value = "";
    };
}

// -------------------- WEBSOCKET CONNECT --------------------

async function connectAndInit() {
    if (!stompClient) await connectWebSocket();

    if (isAdmin) {
        await loadConversations();
        if (!conversationId && conversations.length > 0) {
            openConversation(conversations[0].conversationId, conversations[0].customerId);
        }
    } else {
		try {
		        const res = await fetch(`${CHAT_BASE}/api/chat/room/customer/${currentUserId}`, {
		            headers: { "Authorization": "Bearer " + (getCustomerToken() || '') }
		        });

		        if (res.status === 404) {
		            // room not found — create one by calling start endpoint (adminId is 1 here)
		            const startRes = await fetch(`${CHAT_BASE}/api/chat/start`, {
		                method: 'POST',
		                headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + (getCustomerToken() || '') },
		                body: JSON.stringify({ adminId: 1, customerId: currentUserId })
		            });
		            if (!startRes.ok) throw new Error('Failed to create chat room for customer');
		            const room = await startRes.json();
		            conversationId = room.id;
		            activeToId = room.adminId;
		        } else if (res.ok) {
		            const room = await res.json();
		            conversationId = room.id;
		            activeToId = room.adminId;
		        } else {
		            console.error('Failed to fetch chat room', res.status);
		            return;
		        }

		        // subscribe + load messages
		        subscribeToConversation(conversationId);
		        await loadMessages(conversationId);

		    } catch (err) {
		        console.error('Error initializing customer chat room', err);
		    }
		}
}

async function connectWebSocket() {
    return new Promise((resolve, reject) => {
        const sock = new SockJS(CHAT_BASE + "/ws");
        const client = Stomp.over(sock);
        client.debug = () => {};

        const headers = {};
        const token = isAdmin ? getAdminToken() : getCustomerToken();
        if (token) headers.Authorization = "Bearer " + token;

        client.connect(headers, () => {
            stompClient = client;
            console.log("Chat WS connected");
            resolve();
        }, err => {
            console.error("WS error", err);
            reject(err);
        });
    });
}

function subscribeToConversation(convId) {
    if (!stompClient) return;

    if (activeSubscription) activeSubscription.unsubscribe();

    activeSubscription = stompClient.subscribe(`/topic/chat.${convId}`, msg => {
        onIncomingMessage(JSON.parse(msg.body));
    });
}

// -------------------- CONVERSATION LIST (ADMIN) --------------------

async function loadConversations() {
    const convEl = document.getElementById("chat-convos");
    convEl.innerHTML = `<div style="padding:10px">Loading...</div>`;

    try {
        const res = await fetch(`${CHAT_BASE}/api/chat/conversations`, {
            headers: { Authorization: "Bearer " + getAdminToken() }
        });

        if (!res.ok) {
            convEl.innerHTML = `<div style="padding:10px">Failed to load</div>`;
            return;
        }

        const list = await res.json();
        conversations = list;
        convEl.innerHTML = "";

        list.forEach(c => {
            const div = document.createElement("div");
            div.className = "conv-item " + (c.unreadCount > 0 ? "unread" : "");
            div.innerHTML = `
                <div>Customer: ${c.customerId}</div>
                <div class="meta">${c.latestMessage || ""}</div>
            `;
            div.onclick = () => openConversation(c.conversationId, c.customerId);
            convEl.appendChild(div);
        });

    } catch {
        convEl.innerHTML = `<div style="padding:10px">Network error</div>`;
    }
}

// -------------------- OPEN CONVERSATION --------------------

async function openConversation(convId, customerId) {
    conversationId = convId;
    activeToId = customerId;

    subscribeToConversation(convId);

    if (isAdmin) {
        await fetch(`${CHAT_BASE}/api/chat/${convId}/mark-all-seen?userId=${currentUserId}`, {
            method: "PUT",
            headers: { Authorization: "Bearer " + getAdminToken() }
        });
        await loadConversations();
    }

    await loadMessages(convId);
}

// -------------------- LOAD MESSAGES --------------------

async function loadMessages(convId) {
	if (!convId) {
	    console.warn('loadMessages called with undefined convId');
	    return;
	  }
	  const listEl = document.getElementById('messages-list');
    try {
        const res = await fetch(`${CHAT_BASE}/api/chat/${convId}/messages?limit=200`, {
            headers: { Authorization: "Bearer " + (getAdminToken() || getCustomerToken()) }
        });

        if (!res.ok) {
            listEl.innerHTML = `<div style="padding:12px">Failed to load messages</div>`;
            return;
        }

        const msgs = await res.json();
        listEl.innerHTML = "";

        msgs.forEach(m => {
            const d = document.createElement("div");
            d.className = "message " + (m.fromId === currentUserId ? "me" : "them");
            d.innerHTML = `
                <div class="bubble">${escapeHtml(m.content)}</div>
                <div style="font-size:11px;color:#888">${new Date(m.timestamp).toLocaleString()}</div>
            `;
            listEl.appendChild(d);
        });

        listEl.scrollTop = listEl.scrollHeight;

    } catch {
        listEl.innerHTML = `<div style="padding:12px">Network error</div>`;
    }
}

// -------------------- SEND MESSAGE --------------------

async function sendMessage(toId, text) {
    if (!stompClient) return;

    const msg = {
        conversationId,
        fromId: currentUserId,
        toId,
        content: text
    };

    stompClient.send("/app/chat.send", {}, JSON.stringify(msg));

    // optimistic render
    const listEl = document.getElementById("messages-list");
    const d = document.createElement("div");
    d.className = "message me";
    d.innerHTML = `
        <div class="bubble">${escapeHtml(text)}</div>
        <div style="font-size:11px;color:#888">${new Date().toLocaleString()}</div>
    `;
    listEl.appendChild(d);
    listEl.scrollTop = listEl.scrollHeight;
}

// -------------------- INCOMING MESSAGE --------------------

function onIncomingMessage(m) {
    if (m.conversationId !== conversationId) {
        if (isAdmin) loadConversations();
        return;
    }

    if (m.fromId === currentUserId) return;

    const listEl = document.getElementById("messages-list");
    const d = document.createElement("div");
    d.className = "message them";
    d.innerHTML = `
        <div class="bubble">${escapeHtml(m.content)}</div>
        <div style="font-size:11px;color:#888">${new Date(m.timestamp).toLocaleString()}</div>
    `;
    listEl.appendChild(d);
    listEl.scrollTop = listEl.scrollHeight;
}

// HTML sanitizer
function escapeHtml(s) {
    return String(s || "").replace(/[&<>"'`]/g, c =>
        ({ "&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#39;","`":"&#96;" }[c])
    );
}
