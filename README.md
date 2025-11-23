# PokApp – Plataforma de Cartas Pokémon

O **PokApp** é uma aplicação web desenvolvida para a disciplina de Laboratório de Programação II, com o objetivo de auxiliar colecionadores de cartas Pokémon na **organização da coleção**, **interação com outros usuários** e **compra/venda de cartas**.

---

## Funcionalidades principais

### 1. Autenticação de usuários
- Cadastro de usuário com **nome**, **e-mail** e **senha**.
- Login com validação de credenciais.
- Logout e verificação do usuário logado pelo endpoint `/api/auth/me`.
- Senhas armazenadas com **hash BCrypt**, nunca em texto puro no banco.

### 2. Coleção pessoal de cartas
- Criação de **pastas de coleção** (ex.: “Kanto”, “Lendárias”, etc.).
- Cadastro de cartas dentro de cada pasta, com:
  - Nome do Pokémon.
  - Nome bruto lido da carta.
  - Imagem da carta enviada pelo usuário.
- As imagens são salvas em disco em pastas organizadas por usuário.
- Listagem das pastas e das cartas de cada pasta no frontend (`app.html`).

### 3. Rede Social
- Sistema de **amizades**:
  - Enviar solicitação de amizade por e-mail.
  - Aceitar ou recusar solicitações pendentes.
  - Listar amigos já conectados.
- **Chat** entre amigos:
  - Envio de mensagens de texto.
  - Suporte a envio de imagem na conversa.
  - Atualização periódica (polling) para exibir novas mensagens.
- Visualização das **pastas e cartas dos amigos** diretamente na tela da rede social (`social.html`).

### 4. Marketplace (compra e venda)
- Tela de **anunciar carta para venda** (`market.html`):
  - Upload da imagem da carta.
  - Informar preço desejado.
- Listagem das cartas anunciadas pelo próprio usuário.
- Busca de anúncios de outros usuários, com:
  - Nome do Pokémon.
  - Preço.
  - E-mail do vendedor.
  - Imagem da carta.
- Registro de **notificações de venda**, indicando que um anúncio foi comprado.

### 5. Perfil e imagens
- Cada usuário possui:
  - Nome de usuário, e-mail e avatar.
  - Foto de perfil enviada via upload.
- As fotos de perfil e imagens de cartas são servidas pelo backend em rotas de arquivos estáticos.

---

## Arquitetura do sistema

O projeto segue uma **arquitetura em camadas**:

- **Frontend (apresentação)**  
  - Páginas HTML estáticas: `index.html`, `app.html`, `social.html`, `market.html`.  
  - Estilos em CSS e lógica em JavaScript puro, consumindo a API REST via `fetch`.

- **Backend (serviços REST)**  
  Aplicação Java 21 com **Spring Boot 3**, dividida em:
  - *Controllers* – expõem os endpoints REST (Auth, Collection, Social, Market, Profile).
  - *Services* – concentram as regras de negócio (coleção, amizades, chat, marketplace).
  - *Repositories* – acesso ao banco via **Spring Data JPA**.

- **Persistência**  
  - Banco de dados **PostgreSQL**.
  - Entidades principais: `User`, `CollectionFolder`, `CardItem`, `Friendship`, `DirectMessage`, `MarketListing`.

- **Sistema de arquivos**  
  - Imagens de cartas e fotos de perfil armazenadas em diretórios no servidor, organizadas por ID de usuário.
  - Os caminhos dos arquivos são guardados no banco de dados.

---

## Tecnologias utilizadas

- **Backend**
  - Java 21
  - Spring Boot 3
  - Spring Web (REST)
  - Spring Data JPA + Hibernate
  - PostgreSQL
  - BCrypt (hash de senhas)

- **Frontend**
  - HTML5
  - CSS3
  - JavaScript (ES6+), utilizando `fetch` para chamadas à API

---

## Como executar (resumo)

1. Configurar o banco PostgreSQL e atualizar o `application.yml`/`application.properties` com:
   - URL do banco  
   - Usuário  
   - Senha  

2. Rodar a aplicação Spring Boot (por IDE ou `mvn spring-boot:run`).

3. Acessar no navegador:
   - `http://localhost:8080/index.html` – tela de login/cadastro.
   - `http://localhost:8080/app.html` – coleção.
   - `http://localhost:8080/social.html` – rede social.
   - `http://localhost:8080/market.html` – marketplace.

---

## Autores

- Luana Vieira de Alcantara Garcia  
- Marcos Henrique Yukio Suguita  
- Ruan Pablo Rodrigues
