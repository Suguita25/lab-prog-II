const out = document.getElementById('output');

async function postJson(url, data) {
  const res = await fetch(url, {
    method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify(data)
  });
  const text = await res.text();
  try { return { ok: res.ok, json: JSON.parse(text) }; }
  catch { return { ok: res.ok, json: { raw: text } }; }
}

document.getElementById('registerForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = e.target;
  const payload = { username: f.username.value, email: f.email.value, password: f.password.value };
  const r = await postJson('/api/auth/register', payload);
  out.textContent = JSON.stringify(r.json, null, 2);
});

document.getElementById('loginForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = e.target;
  const payload = { email: f.email.value, password: f.password.value };
  const r = await postJson('/api/auth/login', payload);
  out.textContent = JSON.stringify(r.json, null, 2);
});
