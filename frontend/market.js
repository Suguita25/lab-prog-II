async function ensureAuthMarket() {
  const res = await fetch('/api/auth/me', { credentials: 'same-origin' });
  if (res.status === 401) {
    location.href = '/';
    return null;
  }
  const me = await res.json();
  const span = document.getElementById('userEmailMarket');
  if (span) span.textContent = me.email || me.username || '';
  return me;
}

function escapeHtml(s) {
  return (s || '').replace(/[&<>"']/g, ch => ({
    '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
  }[ch]));
}

/* ---- render helpers ---- */

function renderMyListings(list) {
  const box = document.getElementById('myListings');
  if (!box) return;
  if (!Array.isArray(list) || list.length === 0) {
    box.innerHTML = '<p class="muted">Você ainda não anunciou nenhuma carta.</p>';
    return;
  }

  box.innerHTML = list.map(m => `
    <article class="market-card">
      <div class="market-thumb">
        ${m.imagePath ? `<img src="${m.imagePath}" alt="${escapeHtml(m.pokemonName)}">` : ''}
      </div>
      <div class="market-title">${escapeHtml(m.pokemonName || 'Desconhecido')}</div>
      <!-- cardName NÃO é mais exibido -->
      <div class="market-price">R$ ${Number(m.price).toFixed(2)}</div>
      <div class="market-actions">
        <button class="btn-outline" data-open-img="${m.imagePath || ''}">Abrir imagem</button>
        <button class="btn-outline" data-edit-name="${m.id}" data-current-name="${escapeHtml(m.pokemonName || '')}">Editar nome</button>
        <button class="btn-outline" data-edit-price="${m.id}" data-current-price="${m.price}">Editar preço</button>
        <button class="btn-danger" data-cancel="${m.id}">Cancelar</button>
      </div>
    </article>
  `).join('');

  // abrir imagem
  box.querySelectorAll('[data-open-img]').forEach(b => {
    b.onclick = () => {
      const url = b.dataset.openImg;
      if (url) window.open(url, '_blank');
    };
  });

  // cancelar anúncio
  box.querySelectorAll('[data-cancel]').forEach(b => {
    b.onclick = async () => {
      if (!confirm('Cancelar este anúncio?')) return;
      const id = b.dataset.cancel;
      const r = await fetch(`/api/market/listings/${id}`, {
        method: 'DELETE',
        credentials: 'same-origin'
      });
      if (r.status === 401) { location.href = '/'; return; }
      if (!r.ok) {
        const txt = await r.text();
        try { alert(JSON.parse(txt).error || 'Falha ao cancelar anúncio.'); }
        catch { alert('Falha ao cancelar anúncio.'); }
        return;
      }
      loadMyListings();
    };
  });

  // editar nome do Pokémon
  box.querySelectorAll('[data-edit-name]').forEach(b => {
    b.onclick = async () => {
      const id = b.dataset.editName;
      const current = b.dataset.currentName || '';
      const novo = prompt('Novo nome do Pokémon:', current);
      if (!novo) return;
      await patchListing(id, { pokemonName: novo });
    };
  });

  // editar preço
  box.querySelectorAll('[data-edit-price]').forEach(b => {
    b.onclick = async () => {
      const id = b.dataset.editPrice;
      const current = b.dataset.currentPrice || '';
      const valor = prompt('Novo preço (R$):', current ? Number(current).toFixed(2) : '');
      if (!valor) return;
      await patchListing(id, { price: valor });
    };
  });
}


function renderNotifications(list) {
  const box = document.getElementById('notifications');
  if (!box) return;
  if (!Array.isArray(list) || list.length === 0) {
    box.innerHTML = '<p class="muted">Nenhuma venda ainda.</p>';
    return;
  }
  box.innerHTML = list.map(m => `
    <div class="notification-item">
      Você vendeu <strong>${escapeHtml(m.pokemonName || 'carta')}</strong>
      por <strong>R$ ${Number(m.price).toFixed(2)}</strong>.
    </div>
  `).join('');
}

function renderSearchResults(list) {
  const box = document.getElementById('searchResults');
  if (!box) return;
  if (!Array.isArray(list) || list.length === 0) {
    box.innerHTML = '<p class="muted">Nenhum anúncio encontrado para esta busca.</p>';
    return;
  }
  box.innerHTML = list.map(m => `
    <article class="market-card">
      <div class="market-thumb">
        ${m.imagePath ? `<img src="${m.imagePath}" alt="${escapeHtml(m.pokemonName)}">` : ''}
      </div>
      <div class="market-title">${escapeHtml(m.pokemonName || 'Desconhecido')}</div>
      
      <div class="market-price">R$ ${Number(m.price).toFixed(2)}</div>
      <div class="market-actions">
        <button class="btn-outline" data-open-img="${m.imagePath || ''}">Abrir imagem</button>
        <button class="primary" data-buy="${m.id}">Comprar</button>
      </div>
    </article>
  `).join('');

  box.querySelectorAll('[data-open-img]').forEach(b => {
    b.onclick = () => {
      const url = b.dataset.openImg;
      if (url) window.open(url, '_blank');
    };
  });

  box.querySelectorAll('[data-buy]').forEach(b => {
    b.onclick = async () => {
      if (!confirm('Confirmar compra desta carta?')) return;
      const id = b.dataset.buy;
      const r = await fetch(`/api/market/listings/${id}/buy`, {
        method: 'POST',
        credentials: 'same-origin'
      });
      if (r.status === 401) { location.href = '/'; return; }
      const txt = await r.text();
      let json;
      try { json = JSON.parse(txt); } catch { json = { raw: txt }; }
      if (!r.ok) {
        alert(json.error || 'Falha ao comprar.');
        return;
      }
      alert('Compra realizada!');
      // recarrega resultados e notificações
      doSearch();
      loadNotifications();
    };
  });
}

async function patchListing(id, payload) {
  const r = await fetch(`/api/market/listings/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'same-origin',
    body: JSON.stringify(payload)
  });

  const txt = await r.text();
  let json;
  try { json = JSON.parse(txt); } catch { json = { raw: txt }; }

  if (r.status === 401) {
    location.href = '/';
    return null;
  }

  if (!r.ok) {
    alert(json.error || 'Falha ao atualizar anúncio.');
    return null;
  }

  // recarrega a lista do vendedor (e opcionalmente busca)
  loadMyListings();
  // se quiser atualizar resultados de busca também:
  // doSearch();

  return json;
}


/* ---- chamadas à API ---- */

async function loadMyListings() {
  const r = await fetch('/api/market/listings/mine', { credentials: 'same-origin' });
  if (r.status === 401) { location.href = '/'; return; }
  const json = await r.json();
  renderMyListings(json);
}

async function loadNotifications() {
  const r = await fetch('/api/market/notifications', { credentials: 'same-origin' });
  if (r.status === 401) { location.href = '/'; return; }
  const json = await r.json();
  renderNotifications(json);
}

async function doSearch() {
  const q = document.getElementById('searchInput').value.trim();
  const url = '/api/market/listings/search' + (q ? ('?q=' + encodeURIComponent(q)) : '');
  const r = await fetch(url, { credentials: 'same-origin' });
  if (r.status === 401) { location.href = '/'; return; }
  const json = await r.json();
  renderSearchResults(json);
}

/* ---- anunciar via scanner ---- */

async function handleSellScan() {
  const fileInput = document.getElementById('sellFile');
  const priceInput = document.getElementById('sellPrice');
  const debug = document.getElementById('sellDebug');

  const file = fileInput.files[0];
  const price = priceInput.value.trim();

  if (!file) { alert('Escolha a imagem da carta.'); return; }
  if (!price) { alert('Informe um preço.'); return; }

  const fd = new FormData();
  fd.append('file', file);
  fd.append('price', price);

  debug.textContent = 'Enviando...';

  const r = await fetch('/api/market/listings/scan', {
    method: 'POST',
    body: fd,
    credentials: 'same-origin'
  });

  const txt = await r.text();
  let json;
  try { json = JSON.parse(txt); } catch { json = { raw: txt }; }

  if (r.status === 401) { location.href = '/'; return; }

  if (!r.ok) {
    debug.textContent = 'Erro: ' + (json.error || txt);
    alert(json.error || 'Falha ao anunciar.');
    return;
  }

  debug.textContent = JSON.stringify(json, null, 2);
  fileInput.value = '';
  priceInput.value = '';
  loadMyListings();
}

/* ---- boot ---- */

(async function boot() {
  const me = await ensureAuthMarket();
  if (!me) return;

  document.getElementById('btnSellScan').onclick = handleSellScan;
  document.getElementById('btnSearch').onclick = doSearch;
  document.getElementById('searchInput').addEventListener('keydown', e => {
    if (e.key === 'Enter') doSearch();
  });

  loadMyListings();
  loadNotifications();
  doSearch(); // carrega alguns anúncios logo de cara
})();
