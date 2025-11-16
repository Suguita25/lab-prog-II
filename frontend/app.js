// ---- helpers base ----
const out = document.getElementById('output');
const meEmail = document.getElementById('meEmail');
const bar = document.getElementById('bar');

function setOut(obj) {
  if (!out) return;
  out.textContent = typeof obj === 'string'
    ? obj
    : JSON.stringify(obj, null, 2);
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
          <button data-open="${f.id}" style="margin-left:8px;">Abrir</button>
        </li>
      `).join('')
    : '<li class="muted">Nenhuma pasta</li>';

  ul.querySelectorAll('button[data-open]').forEach(btn => {
    btn.addEventListener('click', () => openFolder(Number(btn.dataset.open)));
  });

  setOut(arr);
}

// ---- abrir pasta + montar GRID de cartas ----
async function openFolder(folderId) {
  const r = await getJson(`/api/collections/folders/${folderId}`);
  if (!r) return;

  const head = document.getElementById('folderHeader');
  if (head) head.textContent = `Pasta #${r.id} — ${r.name}`;

  // preencher IDs dos formulários
  document.getElementById('folderIdManual').value = folderId;
  document.getElementById('folderIdScan').value   = folderId;

  const grid = document.getElementById('cardsGrid');
  if (!grid) return;

  if (!r.items || r.items.length === 0) {
    grid.innerHTML = '<div class="muted" style="padding:10px;">Sem cartas nesta pasta.</div>';
  } else {
    grid.innerHTML = r.items.map(it => {
      const pokemonName = it.pokemonName || 'Unknown';
      const baseName = (it.cardName && it.cardName !== it.pokemonName) ? it.cardName : '';
      const hasImage = !!it.imagePath;

      return `
        <article class="card-item">
          <div class="card-thumb">
            ${
              hasImage
                ? `<img src="${it.imagePath}" alt="${pokemonName}">`
                : `<div class="card-no-img">Sem imagem</div>`
            }
          </div>
          <div class="card-body">
            <h3 class="card-title">${pokemonName}</h3>
            ${
              baseName
                ? `<div class="card-sub">${baseName}</div>`
                : ''
            }
            <div class="card-meta">
              ${it.source ? `<span class="pill sm">${it.source}</span>` : ''}
              ${
                hasImage
                  ? `<a class="link" href="${it.imagePath}" target="_blank">abrir imagem</a>`
                  : ''
              }
            </div>
          </div>
        </article>
      `;
    }).join('');
  }

  setOut(r);
}

// ---- criar pasta ----
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
  const cardName  = document.getElementById('cardName').value.trim();
  if (!folderId || !cardName) {
    return setOut({ error:'Preencha Folder ID e Nome da carta' });
  }
  const r = await postJson('/api/collections/cards/manual', { folderId, cardName });
  if (r) setOut(r.json ?? r);
  if (folderId) openFolder(folderId);
}

async function scanUpload() {
  const folderId = Number(document.getElementById('folderIdScan').value);
  const file = document.getElementById('fileScan').files[0];
  if (!folderId || !file) {
    return setOut({ error:'Preencha Folder ID e selecione uma imagem' });
  }

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
        if (folderId) openFolder(folderId);   // recarrega grid com a nova carta
        resolve(json);
      } catch {
        setOut({ raw: xhr.responseText });
        resolve(xhr.responseText);
      }
    };
    xhr.onerror = () => {
      if (bar) bar.style.width = '0%';
      reject(new Error('Falha no upload'));
    };
    xhr.send(fd);
  });
}

// ---- logout ----
async function doLogout() {
  await fetch('/api/auth/logout', { method:'POST', credentials:'same-origin' });
  location.href = '/';
}

// ---- listeners ----
document.getElementById('btnReloadFolders')?.addEventListener('click', loadFolders);
document.getElementById('btnCreateFolder')?.addEventListener('click', createFolder);
document.getElementById('btnAddManual')?.addEventListener('click', addManual);
document.getElementById('btnScan')?.addEventListener('click', scanUpload);
document.getElementById('logoutBtn')?.addEventListener('click', doLogout);

// ---- boot ----
(async function init(){
  if (await ensureAuth()) {
    await loadFolders();
  }
})();
