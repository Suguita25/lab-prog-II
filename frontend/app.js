// ---- helpers ----
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
        <div>
          <strong>#${f.id}</strong> — ${f.name}
        </div>
        <button class="btn btn-ghost btn-xs" data-open="${f.id}">Abrir</button>
      `;
      list.appendChild(li);
    });

    list.querySelectorAll('[data-open]').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = parseInt(btn.getAttribute('data-open'), 10);
        openFolder(id);
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

// ---- cards / conteúdo da pasta ----

async function openFolder(folderId) {
  try {
    const r = await getJson(`/api/collections/folders/${folderId}`);
    if (!r) return;

    setOut(r);

    const head = document.getElementById('folderHeader');
    if (head) {
      head.textContent = `Pasta #${r.id} — ${r.name}`;
    }

    // preencher ID do scanner
    const inpScan = document.getElementById('folderIdScan');
    if (inpScan) inpScan.value = folderId;

    const grid = document.getElementById('cardsGrid');
    if (!grid) return;

    if (!r.items || r.items.length === 0) {
      grid.innerHTML = '<div class="muted" style="padding:10px;">Sem cartas nesta pasta.</div>';
      return;
    }

    // monta cards com botão de remover
    grid.innerHTML = r.items.map(it => {
      const pokemonName = it.pokemonName || 'Desconhecido';
      const baseName = it.cardName && it.cardName !== pokemonName
        ? it.cardName
        : '';
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
                ? `<div class="card-subtitle">${baseName}</div>`
                : ''
            }
            <div class="card-meta">
            <span class="badge">${it.source || 'manual'}</span>
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
                data-name="${(it.pokemonName || it.cardName || '').replace(/"/g, '&quot;')}"

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

    // listeners de remover
    grid.querySelectorAll('[data-del]').forEach(btn => {
      btn.addEventListener('click', () => {
        const cardId = parseInt(btn.getAttribute('data-del'), 10);
        const fid = parseInt(btn.getAttribute('data-folder'), 10);
        deleteCard(cardId, fid);
      });
    });

      
    // adiciona listeners dos botões de editar nome
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

// ---- remover carta ----

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

// editar nome da carta
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
    await openFolder(folderId); // recarrega a pasta pra refletir o novo nome
  } catch (e) {
    console.error(e);
    alert('Erro ao editar nome da carta.');
  }
}



// upload / scan
async function scanUpload() {
  const folderId = parseInt(document.getElementById('folderIdScan')?.value || '0', 10);
  const fileInput = document.getElementById('scanFile');
  if (!folderId) { alert('Informe o ID da pasta.'); return; }
  if (!fileInput || !fileInput.files.length) {
    alert('Selecione uma imagem.');
    return;
  }

  const form = new FormData();
  form.append('folderId', folderId);
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
      openFolder(folderId);
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

// logout
async function doLogout() {
  try {
    await fetch('/api/auth/logout', {
      method: 'POST',
      credentials: 'same-origin'
    });
  } catch (e) {
    console.error(e);
  } finally {
    location.href = '/';
  }
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
