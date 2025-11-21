let currentFriendId = null;
let currentFriendName = null;

let pollTimer = null;
let lastIso = null;
let chatMsgs = [];
let seenIds = new Set();
window.meId = undefined;

/* ---------------- helpers HTTP ---------------- */
function escapeHtml(s){
  return s.replace(/[&<>"']/g, ch => (
    {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]
  ));
}

async function getJson(url){
  const r = await fetch(url, { credentials:'same-origin' });
  if (r.status === 401) { location.href = '/'; return; }
  return r.json();
}

async function postJson(url, data){
  const r = await fetch(url, {
    method:'POST',
    headers:{'Content-Type':'application/json'},
    credentials:'same-origin',
    body: JSON.stringify(data)
  });
  if (r.status === 401) { location.href='/'; return; }
  const txt = await r.text();
  let json; 
  try { json = JSON.parse(txt); } catch { json = { raw: txt }; }
  if (!r.ok) { alert(json.error || 'Falha'); return null; }
  return json;
}

async function patch(url){
  const r = await fetch(url, { method:'PATCH', credentials:'same-origin' });
  if (r.status === 401) { location.href='/'; return; }
  const txt = await r.text();
  let json; 
  try { json = JSON.parse(txt); } catch { json = { raw: txt }; }
  if (!r.ok) { alert(json.error || 'Falha'); return null; }
  return json;
}

async function ensureAuth() {
  const res = await fetch('/api/auth/me', { credentials: 'same-origin' });
  if (res.status === 401) {
    // sem sessão -> volta para o login (index)
    location.href = '/';
    return null;
  }
  const me = await res.json();
  window.meId = me?.id;
  return me;
}

// substitui loadMe()
async function loadMe(){
  if (window.meId) return window.meId;
  const me = await ensureAuth();
  return me?.id;
}

function updateChatHeader() {
  const title = document.getElementById('chatTitle');
  const subtitle = document.getElementById('chatSubtitle');
  if (!title || !subtitle) return;

  if (currentFriendId && currentFriendName) {
    title.textContent = `Chat com ${currentFriendName}`;
    subtitle.textContent = ''; // some com o “Selecione um amigo...”
  } else {
    title.textContent = 'Chat';
    subtitle.textContent = 'Selecione um amigo para conversar.';
  }
}


/* ---------------- UI chat ---------------- */
function setChat(messages) {
  const box = document.getElementById('chat');
  box.innerHTML = messages.map(m => `
    <div class="msg ${m.mine ? 'me':''}">
      ${m.text ? `<div>${escapeHtml(m.text)}</div>`:''}
      ${m.imagePath ? `<div><a href="${m.imagePath}" target="_blank">[imagem]</a></div>`:''}
      <div class="muted">${new Date(m.createdAt).toLocaleString()}</div>
    </div>
  `).join('');
  box.scrollTop = box.scrollHeight;
}

/* ---------------- listas ---------------- */
async function loadPending(){
  const arr = await getJson('/api/social/friends/pending') || [];
  const ul = document.getElementById('pending');

  if (!Array.isArray(arr) || arr.length === 0) {
    ul.innerHTML = '<li class="muted">Nada pendente</li>';
    return;
  }

  ul.innerHTML = arr.map(f => {
    const name = f.requesterUsername || f.requesterEmail || (`Usuário ${f.requesterId}`);
    return `
      <li>
        <span>De: ${escapeHtml(name)} → Você</span>
        <span>
          <button data-accept="${f.friendshipId}">Aceitar</button>
          <button data-decline="${f.friendshipId}">Recusar</button>
        </span>
      </li>
    `;
  }).join('');

  ul.querySelectorAll('[data-accept]').forEach(b => b.onclick = async () => {
    const ok = await patch(`/api/social/friends/${b.dataset.accept}/accept`);
    if (ok) { loadPending(); loadFriends(); }
  });

  ul.querySelectorAll('[data-decline]').forEach(b => b.onclick = async () => {
    const ok = await patch(`/api/social/friends/${b.dataset.decline}/decline`);
    if (ok) loadPending();
  });
}


async function loadFriends() {
  const arr = await getJson('/api/social/friends') || [];
  const ul = document.getElementById('friends');

  if (!Array.isArray(arr) || arr.length === 0) {
    ul.innerHTML = '<li class="muted">Sem amigos ainda</li>';
    return;
  }

  ul.innerHTML = arr.map(f => {
    const name = f.friendUsername || f.friendEmail || ('Usuário ' + f.friendId);
    const avatarUrl = f.friendAvatarUrl || '';
    const initial = (name && name[0]) ? name[0].toUpperCase() : '?';

    // nome seguro para usar em atributo HTML
    const safeNameAttr = name.replace(/"/g, '&quot;');

    const avatarHtml = avatarUrl
      ? `<img class="friend-avatar-img" src="${avatarUrl}" alt="${escapeHtml(name)}">`
      : `<div class="friend-avatar-placeholder">${escapeHtml(initial)}</div>`;

    return `
      <li class="friend-item">
        <div class="friend-avatar">
          ${avatarHtml}
        </div>
        <span class="friend-name">${escapeHtml(name)}</span>
        <div class="friend-actions">
          <button class="btn-secondary"
                  data-open="${f.friendId}"
                  data-name="${safeNameAttr}">
            Conversar
          </button>
          <button class="btn-danger" data-remove="${f.friendshipId}">Remover</button>
        </div>
      </li>
    `;
  }).join('');

  // abrir chat (passa id + nome do amigo)
  ul.querySelectorAll('[data-open]').forEach(b => {
    b.onclick = () => openChat(
      Number(b.dataset.open),
      b.dataset.name || ''
    );
  });

  // remover amigo
  ul.querySelectorAll('[data-remove]').forEach(b =>
    b.onclick = async () => {
      const friendshipId = b.dataset.remove;
      if (!confirm('Tem certeza que deseja remover este amigo?')) return;

      const r = await fetch(`/api/social/friends/${friendshipId}`, {
        method: 'DELETE',
        credentials: 'same-origin'
      });

      if (r.status === 401) {
        location.href = '/';
        return;
      }

      if (!r.ok) {
        const txt = await r.text();
        try {
          const err = JSON.parse(txt);
          alert(err.error || 'Falha ao remover amigo.');
        } catch {
          alert('Falha ao remover amigo.');
        }
        return;
      }

      // recarrega lista de amigos após remover
      loadFriends();
    }
  );
}




async function loadFriendFolders(friendId){
  const ul = document.getElementById('friendFolders');
  const grid = document.getElementById('friendCards');
  if (!ul || !grid) return;

  ul.innerHTML = '<li class="muted">Carregando pastas...</li>';
  grid.innerHTML = '<div class="muted">Selecione uma pasta para ver as cartas.</div>';

  const arr = await getJson(`/api/social/friends/${friendId}/folders`) || [];

  if (!Array.isArray(arr) || arr.length === 0) {
    ul.innerHTML = '<li class="muted">Esse amigo ainda não tem pastas.</li>';
    return;
  }

  ul.innerHTML = arr.map(f => `
    <li>
      <span>${escapeHtml(f.name)}</span>
      <button data-open-folder="${f.id}">Ver cartas</button>
    </li>
  `).join('');

  ul.querySelectorAll('[data-open-folder]').forEach(b => {
    b.onclick = () => {
      const folderId = Number(b.dataset.openFolder);
      loadFriendFolder(friendId, folderId);
    };
  });
}

async function loadFriendFolder(friendId, folderId){
  const grid = document.getElementById('friendCards');
  if (!grid) return;

  grid.innerHTML = '<div class="muted">Carregando cartas...</div>';

  const data = await getJson(`/api/social/friends/${friendId}/folders/${folderId}`) || {};
  const items = Array.isArray(data.items) ? data.items : [];

  if (items.length === 0) {
    grid.innerHTML = '<div class="muted">Nenhuma carta nesta pasta.</div>';
    return;
  }

  grid.innerHTML = items.map(it => {
    const pokemonName = it.pokemonName || 'Desconhecido';
    const hasImage = !!it.imagePath;

    return `
      <article class="friend-card">
        <div class="friend-card-thumb">
          ${
            hasImage
              ? `<img src="${it.imagePath}" alt="${escapeHtml(pokemonName)}">`
              : '<div class="card-no-img">Sem imagem</div>'
          }
        </div>
        <div class="friend-card-body">
          <div class="friend-card-title">${escapeHtml(pokemonName)}</div>
          ${ hasImage ? `<a href="${it.imagePath}" target="_blank" class="link">Abrir imagem</a>` : '' }
        </div>
      </article>
    `;
  }).join('');
}



/* ---------------- chat/polling ---------------- */
async function openChat(friendId, friendName) {
  currentFriendId = friendId;
  currentFriendName = friendName || null;
  lastIso = null;
  chatMsgs = [];
  seenIds = new Set();

  // atualiza o título/subtítulo do chat
  updateChatHeader();

  // carrega as pastas do amigo
  loadFriendFolders(friendId);

  // carrega histórico de mensagens
  const hist = await getJson(`/api/social/messages?withUserId=${friendId}`) || [];
  hist.forEach(m => seenIds.add(m.id));
  chatMsgs = hist.map(m => ({ ...m, mine: m.senderId === window.meId }));
  setChat(chatMsgs);

  lastIso = hist.length ? hist[hist.length - 1].createdAt : new Date().toISOString();

  // polling para novas mensagens
  if (pollTimer) clearInterval(pollTimer);
  pollTimer = setInterval(async () => {
    if (!currentFriendId || !lastIso) return;
    const newer = await getJson(
      `/api/social/messages?withUserId=${currentFriendId}&since=${encodeURIComponent(lastIso)}`
    ) || [];
    if (newer.length) {
      newer.forEach(m => {
        if (!seenIds.has(m.id)) {
          seenIds.add(m.id);
          chatMsgs.push({ ...m, mine: m.senderId === window.meId });
        }
      });
      lastIso = newer[newer.length - 1].createdAt;
      setChat(chatMsgs);
    }
  }, 2000);
}


/* ---------------- handlers ---------------- */
document.getElementById('btnRequest').onclick = async () => {
  const email = document.getElementById('friendEmail').value.trim();
  if (!email) { alert('Informe o email'); return; }
  const res = await postJson('/api/social/friends/request', { email });
  if (res) {
    document.getElementById('friendEmail').value='';
    await loadPending();
    await loadFriends();
    alert('Solicitação enviada (ou já existente).');
  }
};

document.getElementById('btnSend').onclick = async () => {
  if (!currentFriendId) {
    alert('Escolha um amigo primeiro.');
    return;
  }

  const textInput = document.getElementById('msgText');
  const text = (textInput.value || '').trim();
  if (!text) {
    alert('Digite uma mensagem.');
    return;
  }

  const fd = new FormData();
  fd.append('toUserId', currentFriendId);
  fd.append('text', text);

  const xhr = new XMLHttpRequest();
  xhr.open('POST','/api/social/messages', true);
  xhr.withCredentials = true;
  xhr.onload = () => {
    if (xhr.status === 201 || xhr.status === 200) {
      try {
        const msg = JSON.parse(xhr.responseText);
        if (!seenIds.has(msg.id)) {
          seenIds.add(msg.id);
          chatMsgs.push({ ...msg, mine: true });
          if (!lastIso || msg.createdAt > lastIso) lastIso = msg.createdAt;
          setChat(chatMsgs);
        }
      } catch(e) {
        console.error('JSON inválido no envio', e, xhr.responseText);
      }
      textInput.value = '';
    } else if (xhr.status === 401) {
      location.href = '/';
    } else {
      try {
        const err = JSON.parse(xhr.responseText);
        alert(err.error || 'Falha ao enviar');
      } catch {
        alert('Falha ao enviar');
      }
    }
  };
  xhr.onerror = () => alert('Erro de rede ao enviar.');
  xhr.send(fd);
};


/* ---------------- boot ---------------- */
(async function boot(){
  const me = await ensureAuth();
  if (!me) return;
  await loadPending();
  await loadFriends();
  updateChatHeader();
})();
