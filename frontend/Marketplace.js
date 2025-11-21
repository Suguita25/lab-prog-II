// ===== Helpers HTTP =====
async function getJson(url) {
  const res = await fetch(url, { credentials: 'same-origin' });
  if (res.status === 401) { location.href = '/'; return null; }
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: 'Erro desconhecido' }));
    console.error('GET Error:', err);
    return null;
  }
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
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    alert(data.error || 'Erro na operação');
    return null;
  }
  return data;
}

async function deleteReq(url) {
  const res = await fetch(url, { method: 'DELETE', credentials: 'same-origin' });
  if (res.status === 401) { location.href = '/'; return false; }
  if (!res.ok && res.status !== 204) {
    const err = await res.json().catch(() => ({}));
    alert(err.error || 'Erro ao remover');
    return false;
  }
  return true;
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

function formatCondition(cond) {
  const map = {
    'MINT': 'Perfeita', 'NEAR_MINT': 'Quase Perfeita', 'EXCELLENT': 'Excelente',
    'GOOD': 'Boa', 'LIGHT_PLAYED': 'Pouco Jogada', 'PLAYED': 'Jogada', 'POOR': 'Ruim'
  };
  return map[cond] || cond;
}

function formatPrice(val) {
  return parseFloat(val || 0).toFixed(2);
}

// ===== Auth =====
async function ensureAuth() {
  try {
    const me = await getJson('/api/auth/me');
    return !!me;
  } catch { return false; }
}

// ===== Tabs =====
function setupTabs() {
  document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
      document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
      tab.classList.add('active');
      document.getElementById('tab-' + tab.dataset.tab)?.classList.add('active');
      loadTabData(tab.dataset.tab);
    });
  });
}

function loadTabData(tabName) {
  switch (tabName) {
    case 'browse': loadAvailableCards(); break;
    case 'my-sales': loadMySales(); break;
    case 'offers-received': loadOffersReceived(); break;
    case 'offers-made': loadOffersMade(); break;
    case 'sell': loadMyFolders(); break;
  }
}

// ===== Browse: Cartas disponíveis =====
async function loadAvailableCards() {
  const container = document.getElementById('cardsForSale');
  container.innerHTML = '<div class="empty-state">Carregando...</div>';

  const cards = await getJson('/api/marketplace/available');
  if (!cards || cards.length === 0) {
    container.innerHTML = '<div class="empty-state">Nenhuma carta disponível no momento</div>';
    return;
  }

  container.innerHTML = cards.map(renderCardForSale).join('');
  container.querySelectorAll('[data-offer]').forEach(btn => {
    btn.onclick = () => openOfferModal(btn.dataset.offer, btn.dataset.name, btn.dataset.price);
  });
}

function renderCardForSale(card) {
  const item = card.cardItem || {};
  const name = escapeHtml(item.pokemonName || 'Desconhecido');
  const img = item.imagePath || null;
  const price = formatPrice(card.askingPrice);

  return `
    <article class="card-item">
      <div class="card-thumb">
        ${img ? `<img src="${img}" alt="${name}">` : `<div class="card-no-img">${name}</div>`}
      </div>
      <div class="card-body">
        <h3 class="card-title">${name}</h3>
        <div class="card-price">R$ ${price}</div>
        <div class="card-meta">
          <div><strong>Condição:</strong> ${formatCondition(card.cardCondition)}</div>
          <div><strong>Qtd:</strong> ${card.quantity}</div>
          ${card.sellerNotes ? `<div><em>${escapeHtml(card.sellerNotes)}</em></div>` : ''}
        </div>
        <button class="btn btn-primary btn-full" data-offer="${card.id}" data-name="${name}" data-price="${price}">
          Fazer Proposta
        </button>
      </div>
    </article>
  `;
}

// ===== Search =====
async function searchCards() {
  const query = document.getElementById('searchInput').value.trim();
  if (!query) { alert('Digite um nome para buscar'); return; }

  const container = document.getElementById('cardsForSale');
  container.innerHTML = '<div class="empty-state">Buscando...</div>';

  const cards = await getJson(`/api/marketplace/search?name=${encodeURIComponent(query)}`);
  if (!cards || cards.length === 0) {
    container.innerHTML = '<div class="empty-state">Nenhuma carta encontrada</div>';
    return;
  }

  container.innerHTML = cards.map(renderCardForSale).join('');
  container.querySelectorAll('[data-offer]').forEach(btn => {
    btn.onclick = () => openOfferModal(btn.dataset.offer, btn.dataset.name, btn.dataset.price);
  });
}

// ===== Minhas Vendas =====
async function loadMySales() {
  const container = document.getElementById('mySalesList');
  container.innerHTML = '<div class="empty-state">Carregando...</div>';

  const sales = await getJson('/api/marketplace/my-sales');
  if (!sales || sales.length === 0) {
    container.innerHTML = '<div class="empty-state">Você não tem cartas à venda</div>';
    return;
  }

  container.innerHTML = sales.map(sale => {
    const item = sale.cardItem || {};
    const name = escapeHtml(item.pokemonName || 'Desconhecido');
    return `
      <div class="list-item">
        <div class="list-item-header">
          <div class="list-item-title">${name}</div>
          <span class="badge badge-success">À venda</span>
        </div>
        <div class="list-item-info">
          <div><strong>Preço:</strong> R$ ${formatPrice(sale.askingPrice)}</div>
          <div><strong>Condição:</strong> ${formatCondition(sale.cardCondition)}</div>
          <div><strong>Qtd:</strong> ${sale.quantity}</div>
        </div>
        <div class="list-item-actions">
          <button class="btn btn-sm btn-danger" data-remove="${sale.id}">Remover da venda</button>
        </div>
      </div>
    `;
  }).join('');

  container.querySelectorAll('[data-remove]').forEach(btn => {
    btn.onclick = async () => {
      if (!confirm('Remover esta carta da venda?')) return;
      if (await deleteReq(`/api/marketplace/remove-from-sale/${btn.dataset.remove}`)) loadMySales();
    };
  });
}

// ===== Propostas Recebidas =====
async function loadOffersReceived() {
  const container = document.getElementById('offersReceivedList');
  container.innerHTML = '<div class="empty-state">Carregando...</div>';

  const offers = await getJson('/api/marketplace/received-offers');
  if (!offers || offers.length === 0) {
    container.innerHTML = '<div class="empty-state">Nenhuma proposta recebida</div>';
    return;
  }

  container.innerHTML = offers.map(o => {
    const statusClass = o.status === 'PENDING' ? 'badge-warning' : o.status === 'ACCEPTED' ? 'badge-success' : '';
    return `
      <div class="list-item">
        <div class="list-item-header">
          <div class="list-item-title">Proposta: R$ ${formatPrice(o.offerAmount)}</div>
          <span class="badge ${statusClass}">${o.status}</span>
        </div>
        <div class="list-item-info">
          <div><strong>Qtd:</strong> ${o.quantity}</div>
          <div><strong>Data:</strong> ${new Date(o.createdAt).toLocaleDateString()}</div>
          ${o.buyerMessage ? `<div><strong>Mensagem:</strong> ${escapeHtml(o.buyerMessage)}</div>` : ''}
        </div>
        ${o.status === 'PENDING' ? `
          <div class="list-item-actions">
            <button class="btn btn-sm btn-success" data-accept="${o.id}">Aceitar</button>
            <button class="btn btn-sm btn-danger" data-reject="${o.id}">Rejeitar</button>
          </div>
        ` : ''}
      </div>
    `;
  }).join('');

  container.querySelectorAll('[data-accept]').forEach(btn => {
    btn.onclick = async () => {
      const r = await postJson(`/api/marketplace/accept-offer/${btn.dataset.accept}`, {});
      if (r) { alert('Proposta aceita! Transação criada.'); loadOffersReceived(); }
    };
  });

  container.querySelectorAll('[data-reject]').forEach(btn => {
    btn.onclick = async () => {
      const reason = prompt('Motivo da rejeição (opcional):');
      await postJson(`/api/marketplace/reject-offer/${btn.dataset.reject}`, { reason });
      loadOffersReceived();
    };
  });
}

// ===== Minhas Propostas =====
async function loadOffersMade() {
  const container = document.getElementById('offersMadeList');
  container.innerHTML = '<div class="empty-state">Carregando...</div>';

  const offers = await getJson('/api/marketplace/made-offers');
  if (!offers || offers.length === 0) {
    container.innerHTML = '<div class="empty-state">Você não fez nenhuma proposta ainda</div>';
    return;
  }

  container.innerHTML = offers.map(o => {
    const statusClass = o.status === 'PENDING' ? 'badge-warning' : o.status === 'ACCEPTED' ? 'badge-success' : o.status === 'REJECTED' ? 'badge-danger' : '';
    return `
      <div class="list-item">
        <div class="list-item-header">
          <div class="list-item-title">Sua proposta: R$ ${formatPrice(o.offerAmount)}</div>
          <span class="badge ${statusClass}">${o.status}</span>
        </div>
        <div class="list-item-info">
          <div><strong>Qtd:</strong> ${o.quantity}</div>
          <div><strong>Data:</strong> ${new Date(o.createdAt).toLocaleDateString()}</div>
          ${o.sellerResponse ? `<div><strong>Resposta:</strong> ${escapeHtml(o.sellerResponse)}</div>` : ''}
        </div>
        ${o.status === 'PENDING' ? `
          <div class="list-item-actions">
            <button class="btn btn-sm btn-danger" data-cancel="${o.id}">Cancelar</button>
          </div>
        ` : ''}
      </div>
    `;
  }).join('');

  container.querySelectorAll('[data-cancel]').forEach(btn => {
    btn.onclick = async () => {
      if (!confirm('Cancelar esta proposta?')) return;
      if (await deleteReq(`/api/marketplace/cancel-offer/${btn.dataset.cancel}`)) loadOffersMade();
    };
  });
}

// ===== Vender: Carregar pastas do usuário =====
async function loadMyFolders() {
  const select = document.getElementById('sellFolderSelect');
  select.innerHTML = '<option value="">Carregando...</option>';

  const folders = await getJson('/api/collections/folders');
  if (!folders || folders.length === 0) {
    select.innerHTML = '<option value="">Nenhuma pasta encontrada</option>';
    return;
  }

  select.innerHTML = '<option value="">-- Selecione uma pasta --</option>' +
    folders.map(f => `<option value="${f.id}">${escapeHtml(f.name)}</option>`).join('');
}

// ===== Vender: Carregar cartas da pasta =====
async function loadFolderCardsForSell() {
  const folderId = document.getElementById('sellFolderSelect').value;
  const container = document.getElementById('myCardsForSell');

  if (!folderId) {
    container.innerHTML = '<div class="muted">Selecione uma pasta</div>';
    return;
  }

  container.innerHTML = '<div class="muted">Carregando cartas...</div>';

  const data = await getJson(`/api/collections/folders/${folderId}`);
  if (!data || !data.items || data.items.length === 0) {
    container.innerHTML = '<div class="muted">Nenhuma carta nesta pasta</div>';
    return;
  }

  container.innerHTML = data.items.map(it => {
    const name = escapeHtml(it.pokemonName || 'Desconhecido');
    const img = it.imagePath || null;
    return `
      <div class="mini-card" data-card-id="${it.id}" data-card-name="${name}" data-card-img="${img || ''}">
        <div class="mini-card-thumb">
          ${img ? `<img src="${img}" alt="${name}">` : ''}
        </div>
        <div class="mini-card-name">${name}</div>
      </div>
    `;
  }).join('');

  container.querySelectorAll('.mini-card').forEach(card => {
    card.onclick = () => selectCardForSale(card);
  });
}

let selectedCardId = null;

function selectCardForSale(cardEl) {
  // Remove seleção anterior
  document.querySelectorAll('.mini-card.selected').forEach(c => c.classList.remove('selected'));
  cardEl.classList.add('selected');

  const id = cardEl.dataset.cardId;
  const name = cardEl.dataset.cardName;
  const img = cardEl.dataset.cardImg;

  selectedCardId = id;
  document.getElementById('sellCardId').value = id;

  const preview = document.getElementById('selectedCardPreview');
  preview.innerHTML = `
    ${img ? `<img src="${img}" alt="${name}">` : ''}
    <div class="name">${name}</div>
  `;

  const btn = document.getElementById('btnMarkForSale');
  btn.disabled = false;
  btn.textContent = `Colocar "${name}" à venda`;
}

// ===== Vender: Marcar para venda =====
async function markForSale() {
  const cardItemId = parseInt(document.getElementById('sellCardId').value);
  const price = parseFloat(document.getElementById('sellPrice').value);
  const condition = document.getElementById('sellCondition').value;
  const quantity = parseInt(document.getElementById('sellQuantity').value) || 1;
  const notes = document.getElementById('sellNotes').value.trim();

  if (!cardItemId) { alert('Selecione uma carta'); return; }
  if (!price || price <= 0) { alert('Informe um preço válido'); return; }

  const result = await postJson('/api/marketplace/mark-for-sale', {
    cardItemId, price, currency: 'BRL', condition, quantity, notes: notes || null
  });

  if (result) {
    alert('Carta colocada à venda com sucesso!');
    document.getElementById('sellPrice').value = '';
    document.getElementById('sellNotes').value = '';
    document.getElementById('selectedCardPreview').innerHTML = '<div class="muted">Selecione uma carta ao lado</div>';
    document.getElementById('btnMarkForSale').disabled = true;
    document.getElementById('btnMarkForSale').textContent = 'Selecione uma carta primeiro';
    document.querySelectorAll('.mini-card.selected').forEach(c => c.classList.remove('selected'));
    selectedCardId = null;
  }
}

// ===== Modal de Oferta =====
function openOfferModal(cardForSaleId, cardName, askingPrice) {
  document.getElementById('offerCardForSaleId').value = cardForSaleId;
  document.getElementById('offerCardInfo').innerHTML = `
    <strong>${cardName}</strong><br>
    Preço pedido: <strong>R$ ${askingPrice}</strong>
  `;
  document.getElementById('offerAmount').value = askingPrice;
  document.getElementById('offerModal').classList.add('active');
}

function closeOfferModal() {
  document.getElementById('offerModal').classList.remove('active');
  document.getElementById('offerAmount').value = '';
  document.getElementById('offerQuantity').value = '1';
  document.getElementById('offerMessage').value = '';
}

async function submitOffer() {
  const cardForSaleId = parseInt(document.getElementById('offerCardForSaleId').value);
  const amount = parseFloat(document.getElementById('offerAmount').value);
  const quantity = parseInt(document.getElementById('offerQuantity').value) || 1;
  const message = document.getElementById('offerMessage').value.trim();

  if (!amount || amount <= 0) { alert('Informe um valor válido'); return; }

  const result = await postJson('/api/marketplace/make-offer', {
    cardForSaleId, amount, quantity, message: message || null
  });

  if (result) {
    alert('Proposta enviada com sucesso!');
    closeOfferModal();
  }
}

// ===== Event Listeners =====
document.getElementById('btnSearch')?.addEventListener('click', searchCards);
document.getElementById('searchInput')?.addEventListener('keypress', e => { if (e.key === 'Enter') searchCards(); });
document.getElementById('btnShowAll')?.addEventListener('click', loadAvailableCards);
document.getElementById('btnLoadFolderCards')?.addEventListener('click', loadFolderCardsForSell);
document.getElementById('sellFolderSelect')?.addEventListener('change', loadFolderCardsForSell);
document.getElementById('btnMarkForSale')?.addEventListener('click', markForSale);
document.getElementById('btnCloseModal')?.addEventListener('click', closeOfferModal);
document.getElementById('btnSubmitOffer')?.addEventListener('click', submitOffer);
document.getElementById('logoutBtn')?.addEventListener('click', async () => {
  await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' });
  location.href = '/';
});

// Fechar modal clicando fora
document.getElementById('offerModal')?.addEventListener('click', e => {
  if (e.target.id === 'offerModal') closeOfferModal();
});

// ===== Init =====
(async function init() {
  if (await ensureAuth()) {
    setupTabs();
    loadAvailableCards();
  }
})();