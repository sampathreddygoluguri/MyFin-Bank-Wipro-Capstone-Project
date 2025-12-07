// chat-widget.js
import { getAdminToken, getCustomerToken, getCurrentAdminId, getCurrentCustomerId } from "/assets/js/auth.js";

const CHAT_BASE = window.CHAT_SERVER || "http://localhost:8095";
const WS_ENDPOINT = CHAT_BASE.replace(/^http/, 'ws') + "/ws"; // will be overridden for SockJS below
let stompClient = null;

let conversationId = null;
let currentUserId = null;
let isAdmin = false;
let activeToId = null; // the other party id
let conversations = []; // admin conversation list
let activeSubscription = null;

function el(html) { const d = document.createElement('div'); d.innerHTML = html; return d.firstElementChild; }

export async function initChatWidget() {
	// ensure DOM only once
	if (document.getElementById('chat-floating-btn')) return;

	// determine user
	const adminId = getCurrentAdminId();
	const custId = getCurrentCustomerId();
	if (adminId) {
		isAdmin = true; currentUserId = Number(adminId);
	} else if (custId) {
		isAdmin = false; currentUserId = Number(custId);
	} else {
		// not logged in — don't show widget
		return;
	}

	// build DOM
	const btn = el(`<div id="chat-floating-btn" title="Chat" aria-label="Open chat">💬</div>`);
	document.body.appendChild(btn);

	const widget = el(`<div class="chat-widget" id="chat-widget" role="dialog" aria-hidden="true">
      <div class="chat-header">
        <div class="title">Chat with Customers</div>
        <div class="controls">
          <button id="chat-max" aria-label="Maximize">⬜</button>
          <button id="chat-min" aria-label="Minimize">—</button>
          <button id="chat-close" aria-label="Close">✕</button>
        </div>
      </div>
      <div class="chat-body">
        <div class="chat-convos" id="chat-convos" style="display:${isAdmin ? 'block' : 'none'}"></div>
        <div class="chat-messages" id="chat-messages-panel">
          <div class="messages-list" id="messages-list"></div>
          <div class="chat-composer">
            <input id="chat-input" placeholder="Type a message..." aria-label="Type a message" />
            <button id="chat-send" aria-label="Send message">Send</button>
          </div>
        </div>
      </div>
    </div>`);
	document.body.appendChild(widget);

	// controls
	btn.addEventListener('click', async () => {
		widget.style.display = widget.style.display === 'block' ? 'none' : 'block';
		widget.setAttribute('aria-hidden', widget.style.display !== 'block');
		// If opening, initialize connection
		if (widget.style.display === 'block') {
			await connectAndInit();
		}
	});

	document.getElementById('chat-close').addEventListener('click', () => {
		widget.style.display = 'none';
		widget.setAttribute('aria-hidden', 'true');
	});

	document.getElementById('chat-min').addEventListener('click', () => {
		widget.style.display = 'none';
		widget.setAttribute('aria-hidden', 'true');
	});

	document.getElementById('chat-max').addEventListener('click', () => {
		widget.classList.toggle('fullscreen');
		const floatBtn = document.getElementById('chat-floating-btn');
		if (widget.classList.contains('fullscreen')) {
			// hide floating button while fullscreen so it doesn't obstruct UI
			if (floatBtn) floatBtn.style.display = 'none';
			widget.setAttribute('aria-hidden', 'false');
		} else {
			if (floatBtn) floatBtn.style.display = 'flex';
			widget.setAttribute('aria-hidden', widget.style.display !== 'block');
		}
	});

	document.getElementById('chat-send').addEventListener('click', async () => {
		const input = document.getElementById('chat-input');
		const text = input.value.trim();
		if (!text) return;
		if (!activeToId) {
			alert("Select a conversation (admin) or wait for agent (customer).");
			return;
		}
		await sendMessage(activeToId, text);
		input.value = '';
	});
}
window.openChatRoomFromOutside = async function(convId, customerId) {
	console.log("Externally requested chat:", convId);

	conversationId = convId;
	activeToId = customerId;

	// ensure socket is ready
	if (!stompClient) await connectWebSocket();

	// subscribe to this conversation
	subscribeToConversation(convId);

	// mark read (admin only)
	if (isAdmin) {
		await fetch(`${CHAT_BASE}/api/chat/${convId}/mark-all-seen?userId=${currentUserId}`, {
			method: 'PUT',
			headers: { 'Authorization': 'Bearer ' + (getAdminToken() || '') }
		});

		await loadConversations(); // update unread count
	}

	await loadMessages(convId);
};

async function connectAndInit() {
	if (!stompClient) await connectWebSocket();
	// load conversation list for admin
	if (isAdmin) {
		await loadConversations();
		// auto open first conversation if none selected
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
		const sockjs = new SockJS(CHAT_BASE + '/ws');
		const client = Stomp.over(sockjs);
		client.debug = () => { };
		const headers = {};
		const token = isAdmin ? getAdminToken() : getCustomerToken();
		if (token) headers.Authorization = 'Bearer ' + token;

		client.connect(headers, frame => {
			stompClient = client;
			console.log('Chat WS connected');
			resolve();
		}, err => {
			console.error('WS connect error', err);
			reject(err);
		});
	});
}

function subscribeToConversation(convId) {
	if (!stompClient) return;

	// unsubscribe old subscription
	if (activeSubscription) {
		activeSubscription.unsubscribe();
		activeSubscription = null;
	}

	// subscribe new
	activeSubscription = stompClient.subscribe('/topic/chat.' + convId, message => {
		const payload = JSON.parse(message.body);
		onIncomingMessage(payload);
	});
}


// load and render the admin conversation list
async function loadConversations() {
	const convEl = document.getElementById('chat-convos');
	convEl.innerHTML = '<div style="padding:10px">Loading...</div>';
	try {
		const res = await fetch(`${CHAT_BASE}/api/chat/conversations`, {
			headers: { 'Authorization': 'Bearer ' + (getAdminToken() || '') }
		});
		if (!res.ok) { convEl.innerHTML = '<div style="padding:10px">Failed to load</div>'; return; }
		const data = await res.json();
		conversations = data;
		convEl.innerHTML = '';
		data.forEach(c => {
			const item = document.createElement('div');
			item.className = 'conv-item ' + (c.unreadCount && c.unreadCount > 0 ? 'unread' : '');
			item.innerHTML = `<div>Customer: ${c.customerId}</div>
                        <div class="meta">${c.latestMessage ? c.latestMessage.slice(0, 60) : ''}</div>`;
			item.addEventListener('click', () => {
				openConversation(c.conversationId, c.customerId);
			});
			convEl.appendChild(item);
		});
	} catch (e) {
		convEl.innerHTML = '<div style="padding:10px">Network error</div>';
	}
}

async function openConversation(convId, customerId) {
	conversationId = convId;
	activeToId = customerId;
	// subscribe
	subscribeToConversation(convId);
	// mark all seen by admin
	if (isAdmin) {
		await fetch(`${CHAT_BASE}/api/chat/${convId}/mark-all-seen?userId=${currentUserId}`, {
			method: 'PUT',
			headers: { 'Authorization': 'Bearer ' + (getAdminToken() || '') }
		});
		// reload conv list to update unread counts
		await loadConversations();
	}
	await loadMessages(convId);
}

async function loadMessages(convId) {
	const listEl = document.getElementById('messages-list');
	listEl.innerHTML = '<div style="padding:12px">Loading...</div>';
	try {
		const res = await fetch(`${CHAT_BASE}/api/chat/${convId}/messages?limit=200`, {
			headers: { 'Authorization': 'Bearer ' + (getAdminToken() || getCustomerToken() || '') }
		});
		if (!res.ok) { listEl.innerHTML = '<div style="padding:12px">Failed to load messages</div>'; return; }
		const msgs = await res.json();
		listEl.innerHTML = '';
		msgs.forEach(m => {
			const d = document.createElement('div');
			d.className = 'message ' + (m.fromId === currentUserId ? 'me' : 'them');
			d.innerHTML = `<div class="bubble">${escapeHtml(m.content)}</div>
                     <div style="font-size:11px;color:#888">${new Date(m.timestamp).toLocaleString()}</div>`;
			listEl.appendChild(d);
		});
		listEl.scrollTop = listEl.scrollHeight;
	} catch (e) {
		listEl.innerHTML = '<div style="padding:12px">Network error</div>';
	}
}

async function sendMessage(toId, text) {
	if (!stompClient) {
		console.warn('not connected');
		return;
	}
	const msg = {
		conversationId: conversationId,
		fromId: currentUserId,
		toId: toId,
		content: text
	};
	stompClient.send('/app/chat.send', {}, JSON.stringify(msg));
	// optimistic render
	const listEl = document.getElementById('messages-list');
	const d = document.createElement('div');
	d.className = 'message me';
	d.innerHTML = `<div class="bubble">${escapeHtml(text)}</div>
                 <div style="font-size:11px;color:#888">${new Date().toLocaleString()}</div>`;
	listEl.appendChild(d);
	listEl.scrollTop = listEl.scrollHeight;
}

function onIncomingMessage(payload) {
	// payload is ChatMessageDto
	// if not current conversation, reload conversation list (admin)
	if (payload.conversationId !== conversationId) {
		if (isAdmin) loadConversations();
		return;
	}
	// append to messages
	const listEl = document.getElementById('messages-list');
	if (payload.fromId === currentUserId) {
		return;
	}

	const d = document.createElement('div');
	d.className = 'message them';

	d.innerHTML = `<div class="bubble">${escapeHtml(payload.content)}</div>
                 <div style="font-size:11px;color:#888">${new Date(payload.timestamp).toLocaleString()}</div>`;
	listEl.appendChild(d);
	listEl.scrollTop = listEl.scrollHeight;
}

function escapeHtml(s) { return String(s || '').replace(/[&<>"'`]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;', '`': '&#96;' }[c])); }

