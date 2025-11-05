// ---- helpers base ----
const out = document.getElementById('output');
const meEmail = document.getElementById('meEmail');
const bar = document.getElementById('bar');

function setOut(obj) {
  out && (out.textContent = typeof obj === 'string' ? obj : JSON.stringify(obj, null, 2));
}

async function getJson(url) {
  const res = await fetch(url, { credentials: 'same-origin' });
  if (res.status === 401) { location.href = '/'; return null; }
  return res.json();
}

async function postJson(url, body) {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'same-origin',
    body: JSON.stringify(body)
  });
  if (res.status === 401) { location.href = '/'; return null; }
  const text = await res.text();
  try { return { ok: res.ok, json: JSON.parse(text) }; }
  catch { return { ok: res.ok, json: { raw: text } }; }
}

// ---- sessão ----
async function ensureAuth() {
  const me = await getJson('/api/auth/me');
  if (!me) return false;
  if (meEmail) meEmail.textContent = me.email || '(sem e-mail)';
  return true;
}

// ---- pastas ----
async function loadFolders() {
  const arr = await getJson('/api/collections/folders');
  if (!arr) return;
  const ul = document.getElementById('foldersList');
  if (!ul) return;

  ul.innerHTML = arr.length
    ? arr.map(f => `
      <li>
        <strong>#${f.id}</strong> — ${f.name}
        <button data-open="${f.id}" style="margin-left:8px;">Ver</button>
      </li>`).join('')
    : '<li class="muted">Nenhuma pasta</li>';

  // delega cliques para abrir pasta
  ul.querySelectorAll('button[data-open]').forEach(btn => {
    btn.addEventListener('click', () => openFolder(Number(btn.dataset.open)));
  });

  setOut(arr);
}


async function openFolder(folderId) {
  const r = await getJson(`/api/collections/folders/${folderId}`);
  if (!r) return;

  // header
  const head = document.getElementById('folderHeader');
  if (head) head.textContent = `Pasta #${r.id} — ${r.name}`;

  // tabela
  const tbody = document.getElementById('cardsBody');
  if (!tbody) return;

  if (!r.items || r.items.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" style="padding:10px; color:#666;">Sem cartas nesta pasta.</td></tr>`;
  } else {
    tbody.innerHTML = r.items.map((it, idx) => `
      <tr>
        <td style="padding:8px; border-bottom:1px solid #f0f0f0;">${idx + 1}</td>
        <td style="padding:8px; border-bottom:1px solid #f0f0f0;">${it.cardName ?? ''}</td>
        <td style="padding:8px; border-bottom:1px solid #f0f0f0;">${it.pokemonName ?? ''}</td>
        <td style="padding:8px; border-bottom:1px solid #f0f0f0;">${it.source ?? ''}</td>
        <td style="padding:8px; border-bottom:1px solid #f0f0f0;">
          ${it.imagePath ? `<a href="${it.imagePath}" target="_blank">ver imagem</a>` : '—'}
        </td>
      </tr>
    `).join('');
  }

  setOut(r); // mostra JSON no painel de saída
}

async function createFolder() {
  const name = document.getElementById('folderName').value.trim();
  if (!name) return setOut({ error: 'Informe o nome da pasta' });
  const r = await postJson('/api/collections/folders', { name });
  if (r) setOut(r.json);
  loadFolders();
}

// ---- cartas ----
async function addManual() {
  const folderId = Number(document.getElementById('folderIdManual').value);
  const cardName = document.getElementById('cardName').value.trim();
  if (!folderId || !cardName) return setOut({ error: 'Preencha Folder ID e Nome da carta' });
  const r = await postJson('/api/collections/cards/manual', { folderId, cardName });
  if (r) setOut(r.json ?? r);
}

async function scanUpload() {
  const folderId = Number(document.getElementById('folderIdScan').value);
  const file = document.getElementById('fileScan').files[0];
  if (!folderId || !file) return setOut({ error: 'Preencha Folder ID e selecione uma imagem' });

  return new Promise((resolve, reject) => {
    const fd = new FormData();
    fd.append('folderId', folderId);
    fd.append('file', file);

    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/api/collections/cards/scan', true);
    xhr.withCredentials = true;

    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable && bar) {
        const pct = Math.round((e.loaded / e.total) * 100);
        bar.style.width = pct + '%';
      }
    };
    xhr.onload = () => {
      if (bar) bar.style.width = '0%';
      if (xhr.status === 401) { location.href = '/'; return; }
      try {
        const json = JSON.parse(xhr.responseText || '{}');
        setOut(json);
        resolve(json);
      } catch {
        setOut({ raw: xhr.responseText });
        resolve(xhr.responseText);
      }
    };
    xhr.onerror = () => { if (bar) bar.style.width = '0%'; reject(new Error('Falha no upload')); };
    xhr.send(fd);
  });
}

// ---- logout ----
async function doLogout() {
  await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' });
  location.href = '/';
}

// ---- listeners ----
document.getElementById('btnReloadFolders')?.addEventListener('click', loadFolders);
document.getElementById('btnCreateFolder')?.addEventListener('click', createFolder);
document.getElementById('btnAddManual')?.addEventListener('click', addManual);
document.getElementById('btnScan')?.addEventListener('click', scanUpload);
document.getElementById('logoutBtn')?.addEventListener('click', doLogout);

// ---- boot ----
(async function init() {
  if (await ensureAuth()) {
    await loadFolders();
  }
})();
