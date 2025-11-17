// ---- helpers ----
const out = document.getElementById('output');
const meEmail = document.getElementById('meEmail');
const bar = document.getElementById('bar');

let currentFolderId = null;

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
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  if (res.status === 401) { location.href = '/'; return null; }
  return res.json();
}

// ---- auth ----

async function ensureAuth() {
  try {
    const me = await getJson('/api/auth/me');
    if (!me) return false;
    if (meEmail) meEmail.textContent = me.email || '(sem email)';
    return true;
  } catch (e) {
    console.error(e);
    return false;
  }
}


async function ensureAuth() {
  const res = await fetch('/api/auth/me', { credentials: 'same-origin' });
  if (res.status === 401) { location.href = '/'; return null; }
  const me = await res.json();
  window.meId = me?.id;
  const span = document.getElementById('userEmail');
  if (span) span.textContent = me.email || me.username || '';

  applyAvatar(me);
  return me;
}

function applyAvatar(me) {
  const img = document.getElementById('avatarImg');
  const initialSpan = document.getElementById('avatarInitial');
  if (!img || !initialSpan) return;

  const url = me.profileImagePath;
  if (url) {
    img.src = url;
    img.style.display = 'block';
    initialSpan.style.display = 'none';
  } else {
    const base = (me.username || me.email || '?').trim();
    const initial = base ? base[0].toUpperCase() : '?';
    initialSpan.textContent = initial;
    initialSpan.style.display = 'block';
    img.style.display = 'none';
  }
}



// ---- pastas ----

async function loadFolders() {
  const list = document.getElementById('foldersList');
  if (!list) return;

  list.innerHTML = '<li class="muted">Carregando...</li>';

  try {
    const folders = await getJson('/api/collections/folders');
    if (!folders || !folders.length) {
      list.innerHTML = '<li class="muted">Nenhuma pasta criada ainda.</li>';
      setOut(folders || {});
      return;
    }

    list.innerHTML = '';
    folders.forEach(f => {
      const li = document.createElement('li');
      li.innerHTML = `
        <div class="folder-name">
          ${f.name}
        </div>
        <div class="folder-actions">
          <button class="btn btn-ghost btn-xs" data-open="${f.id}">Abrir</button>
          <button class="btn-icon btn-danger" data-del-folder="${f.id}">
            Remover
          </button>
        </div>
      `;
      list.appendChild(li);
    });

    // abrir pasta
    list.querySelectorAll('[data-open]').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = parseInt(btn.getAttribute('data-open'), 10);
        openFolder(id);
      });
    });

    // remover pasta
    list.querySelectorAll('[data-del-folder]').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = parseInt(btn.getAttribute('data-del-folder'), 10);
        deleteFolder(id);
      });
    });

    setOut(folders);
  } catch (e) {
    console.error(e);
    list.innerHTML = '<li class="muted">Erro ao carregar pastas.</li>';
  }
}

async function createFolder() {
  const nameInput = document.getElementById('folderName');
  if (!nameInput) return;
  const name = nameInput.value.trim();
  if (!name) {
    alert('Informe um nome para a pasta.');
    return;
  }
  const r = await postJson('/api/collections/folders', { name });
  setOut(r);
  nameInput.value = '';
  await loadFolders();
}

// remover pasta
async function deleteFolder(folderId) {
  if (!confirm('Deseja realmente remover esta pasta? Todas as cartas dela serão apagadas.')) {
    return;
  }

  try {
    const res = await fetch(`/api/collections/folders/${folderId}`, {
      method: 'DELETE',
      credentials: 'same-origin'
    });

    if (res.status === 401) {
      location.href = '/';
      return;
    }

    if (!res.ok) {
      alert('Erro ao remover pasta.');
      return;
    }

    // se a pasta removida era a que estava aberta, limpa a área de cards
    if (currentFolderId === folderId) {
      currentFolderId = null;
      const head = document.getElementById('folderHeader');
      const grid = document.getElementById('cardsGrid');
      if (head) head.textContent = 'Selecione uma pasta para visualizar.';
      if (grid) grid.innerHTML = '';
    }

    setOut({ deletedFolderId: folderId });
    await loadFolders();
  } catch (e) {
    console.error(e);
    alert('Erro ao remover pasta.');
  }
}

// ---- cards / conteúdo da pasta ----

async function openFolder(folderId) {
  try {
    const r = await getJson(`/api/collections/folders/${folderId}`);
    if (!r) return;

    currentFolderId = folderId;
    setOut(r);

    const head = document.getElementById('folderHeader');
    if (head) {
      // não mostramos mais o ID numérico, só o nome
      head.textContent = `Pasta — ${r.name}`;
    }

    // preencher NOME da pasta no scanner (apenas visual)
    const inpScan = document.getElementById('folderNameScan');
    if (inpScan) inpScan.value = r.name || '';


    const grid = document.getElementById('cardsGrid');
    if (!grid) return;

    if (!r.items || r.items.length === 0) {
      grid.innerHTML = '<div class="muted" style="padding:10px;">Sem cartas nesta pasta.</div>';
      return;
    }

    grid.innerHTML = r.items.map(it => {
      const pokemonName = it.pokemonName || 'Desconhecido';
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
            <div class="card-meta">
              <div class="card-actions">
                ${
                  hasImage
                    ? `<a href="${it.imagePath}" target="_blank" class="link">Abrir imagem</a>`
                    : ''
                }
                <button
                  class="btn-icon btn-secondary"
                  data-edit="${it.id}"
                  data-folder="${folderId}"
                  data-name="${(it.pokemonName || '').replace(/"/g, '&quot;')}"
                >
                  Editar nome
                </button>
                <button class="btn-icon btn-danger" data-del="${it.id}" data-folder="${folderId}">
                  Remover
                </button>
              </div>
            </div>
          </div>
        </article>
      `;
    }).join('');


    // remover carta
    grid.querySelectorAll('[data-del]').forEach(btn => {
      btn.addEventListener('click', () => {
        const cardId = parseInt(btn.getAttribute('data-del'), 10);
        const fid = parseInt(btn.getAttribute('data-folder'), 10);
        deleteCard(cardId, fid);
      });
    });

    // editar nome da carta
    grid.querySelectorAll('[data-edit]').forEach(btn => {
      btn.addEventListener('click', () => {
        const cardId = parseInt(btn.getAttribute('data-edit'), 10);
        const fid = parseInt(btn.getAttribute('data-folder'), 10);
        const currentName = btn.getAttribute('data-name') || '';
        editCardName(cardId, fid, currentName);
      });
    });

  } catch (e) {
    console.error(e);
    alert('Erro ao carregar pasta.');
  }
}

// remover carta
async function deleteCard(cardId, folderId) {
  if (!confirm('Tem certeza que deseja remover esta carta da pasta?')) {
    return;
  }

  try {
    const res = await fetch(`/api/collections/cards/${cardId}`, {
      method: 'DELETE',
      credentials: 'same-origin'
    });

    if (res.status === 401) {
      location.href = '/';
      return;
    }

    if (!res.ok) {
      alert('Erro ao remover carta.');
      return;
    }

    setOut({ deletedCardId: cardId });
    await openFolder(folderId);

  } catch (e) {
    console.error(e);
    alert('Erro ao remover carta.');
  }
}

// editar nome da carta (pokemonName)
async function editCardName(cardId, folderId, currentName) {
  const novoNome = prompt('Novo nome da carta:', currentName || '');
  if (novoNome === null) return;        // cancelou
  if (!novoNome.trim()) {
    alert('O nome não pode ser vazio.');
    return;
  }

  try {
    const res = await fetch(`/api/collections/cards/${cardId}`, {
      method: 'PATCH',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ pokemonName: novoNome.trim() })
    });

    if (res.status === 401) {
      location.href = '/';
      return;
    }

    if (!res.ok) {
      alert('Erro ao editar nome da carta.');
      return;
    }

    const json = await res.json();
    setOut(json);
    await openFolder(folderId); // recarrega a pasta
  } catch (e) {
    console.error(e);
    alert('Erro ao editar nome da carta.');
  }
}

// upload / scan
async function scanUpload() {
  const inp = document.getElementById('folderNameScan');
  const folderName = inp ? inp.value.trim() : '';
  const fileInput = document.getElementById('scanFile');

  if (!folderName) {
    alert('Informe o NOME da pasta (ou abra uma pasta na lista, que o nome é preenchido automaticamente).');
    return;
  }
  if (!fileInput || !fileInput.files.length) {
    alert('Selecione uma imagem.');
    return;
  }

  const form = new FormData();
  form.append('folderName', folderName);
  form.append('file', fileInput.files[0]);

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

      // se você quiser recarregar a pasta, precisa do id atual:
      if (currentFolderId != null) {
        openFolder(currentFolderId);
      }
      fileInput.value = '';
    } catch (e) {
      console.error(e);
    }
  };

  xhr.onerror = () => {
    if (bar) bar.style.width = '0%';
    alert('Erro no upload.');
  };

  xhr.send(form);
}


const avatarInput = document.getElementById('avatarFile');
if (avatarInput) {
  avatarInput.addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const fd = new FormData();
    fd.append('file', file);

    const r = await fetch('/api/profile/avatar', {
      method: 'POST',
      body: fd,
      credentials: 'same-origin'
    });

    if (r.status === 401) {
      location.href = '/';
      return;
    }

    const txt = await r.text();
    let json;
    try { json = JSON.parse(txt); } catch { json = { raw: txt }; }

    if (!r.ok) {
      alert(json.error || 'Falha ao enviar foto de perfil.');
      return;
    }

    const url = json.avatarUrl;
    const img = document.getElementById('avatarImg');
    const initialSpan = document.getElementById('avatarInitial');
    if (img && initialSpan && url) {
      img.src = url + '?v=' + Date.now(); // bust cache
      img.style.display = 'block';
      initialSpan.style.display = 'none';
    }
  });
}



// logout
const btnLogout = document.getElementById('btnLogout');
if (btnLogout) {
  btnLogout.onclick = async () => {
    await fetch('/api/auth/logout', { method:'POST', credentials:'same-origin' });
    location.href = '/';
  };
}


// ---- listeners ----
document.getElementById('btnReloadFolders')?.addEventListener('click', loadFolders);
document.getElementById('btnCreateFolder')?.addEventListener('click', createFolder);
document.getElementById('btnScan')?.addEventListener('click', scanUpload);
document.getElementById('logoutBtn')?.addEventListener('click', doLogout);

// ---- boot ----
(async function init() {
  if (await ensureAuth()) {
    await loadFolders();
  }
})();
