const out = document.getElementById('output');

async function postJson(url, data) {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'same-origin',
    body: JSON.stringify(data)
  });

  const text = await res.text();
  try {
    return { ok: res.ok, json: JSON.parse(text) };
  } catch {
    return { ok: res.ok, json: { raw: text } };
  }
}

/* --------- CADASTRO --------- */
document.getElementById('registerForm')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = e.target;

  const payload = {
    username: f.username.value,
    email: f.email.value,
    password: f.password.value
  };

  const r = await postJson('/api/auth/register', payload);

  if (r.ok) {
    // sucesso no cadastro
    alert('Cadastro feito com sucesso!');
    f.reset(); // limpa todos os campos do formulário
    if (out) out.textContent = ''; // limpa debug, se existir
  } else {
    // erro no cadastro
    const msg = r.json?.error || 'Erro ao cadastrar.';
    alert(msg);
    if (out) out.textContent = JSON.stringify(r.json, null, 2);
  }
});

/* --------- LOGIN --------- */
document.getElementById('loginForm')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = e.target;

  const payload = {
    email: f.email.value,
    password: f.password.value
  };

  const r = await postJson('/api/auth/login', payload);

  if (r?.ok) {
    // login ok -> vai para a área logada
    window.location.href = '/app.html';
  } else {
    const msg = r.json?.error || 'Falha no login.';
    alert(msg);
    if (out) out.textContent = JSON.stringify(r?.json ?? { error: 'Falha no login' }, null, 2);
  }
});
