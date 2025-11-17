let currentFriendId = null;
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
  await loadMe();
  const arr = await getJson('/api/social/friends/pending') || [];
  const ul = document.getElementById('pending');
  ul.innerHTML = arr.length ? arr.map(f => `
    <li>
      <span>De: ${f.requesterId} → Você</span>
      <span>
        <button data-accept="${f.id}">Aceitar</button>
        <button data-decline="${f.id}">Recusar</button>
      </span>
    </li>`).join('') : '<li class="muted">Nada pendente</li>';

  ul.querySelectorAll('[data-accept]').forEach(b=>b.onclick=async ()=>{
    const ok = await patch(`/api/social/friends/${b.dataset.accept}/accept`);
    if (ok) { loadPending(); loadFriends(); }
  });
  ul.querySelectorAll('[data-decline]').forEach(b=>b.onclick=async ()=>{
    const ok = await patch(`/api/social/friends/${b.dataset.decline}/decline`);
    if (ok) loadPending();
  });
}

async function loadFriends(){
  const arr = await getJson('/api/social/friends') || [];
  const ul = document.getElementById('friends');

  if (!Array.isArray(arr) || arr.length === 0) {
    ul.innerHTML = '<li class="muted">Sem amigos ainda</li>';
    return;
  }

  ul.innerHTML = arr.map(f => {
    const display = f.username || f.email || (`Usuário ${f.userId}`);
    return `
      <li>
        <span>${escapeHtml(display)}</span>
        <button data-open="${f.userId}">Conversar/Ver pastas</button>
      </li>
    `;
  }).join('');

  ul.querySelectorAll('[data-open]').forEach(b => {
    b.onclick = () => openChat(Number(b.dataset.open));
  });
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
async function openChat(friendId){
  currentFriendId = friendId;
  lastIso = null;
  chatMsgs = [];
  seenIds = new Set();

    //carrega as pastas do amigo
  loadFriendFolders(friendId);

  const hist = await getJson(`/api/social/messages?withUserId=${friendId}`) || [];
  hist.forEach(m => seenIds.add(m.id));
  chatMsgs = hist.map(m => ({ ...m, mine: m.senderId === window.meId }));
  setChat(chatMsgs);

  lastIso = hist.length ? hist[hist.length - 1].createdAt : new Date().toISOString();

  if (pollTimer) clearInterval(pollTimer);
  pollTimer = setInterval(async () => {
    if (!currentFriendId || !lastIso) return;
    const newer = await getJson(`/api/social/messages?withUserId=${currentFriendId}&since=${encodeURIComponent(lastIso)}`) || [];
    if (newer.length){
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
  if (!currentFriendId) { alert('Escolha um amigo primeiro.'); return; }
  const text = document.getElementById('msgText').value;
  const file = document.getElementById('msgFile').files[0];

  const fd = new FormData();
  fd.append('toUserId', currentFriendId);
  if (text) fd.append('text', text);
  if (file) fd.append('file', file);

  const xhr = new XMLHttpRequest();
  xhr.open('POST','/api/social/messages', true);
  xhr.withCredentials = true;
  xhr.onload = () => {
    if (xhr.status===201 || xhr.status===200){
      try{
        const msg = JSON.parse(xhr.responseText);
        if (!seenIds.has(msg.id)) {
          seenIds.add(msg.id);
          chatMsgs.push({ ...msg, mine: true });
          if (!lastIso || msg.createdAt > lastIso) lastIso = msg.createdAt;
          setChat(chatMsgs);
        }
      } catch(e){
        console.error('JSON inválido no envio', e, xhr.responseText);
      }
      document.getElementById('msgText').value='';
      document.getElementById('msgFile').value='';
    } else if (xhr.status===401){
      location.href='/';
    } else {
      try{
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
})();
