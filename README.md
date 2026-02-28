# StockControl — Controle de Estoque (JavaFX + SQLite)

Aplicação desktop de controle de estoque construída com **Java 21**, **JavaFX 21** e **SQLite**.  
O objetivo do projeto é entregar uma base **simples e consistente** (estilo ERP) com foco em:

- **CRUD** com UI JavaFX (FXML + CSS)
- **Movimentações** (entrada/saída) controlando o estoque
- **Integridade referencial (Foreign Keys)** para preservar histórico
- **Migrações** de banco (`PRAGMA user_version`)

Repositório: https://github.com/MatheusO-Machado/stockcontrol

---

## Demonstração (telas)

> As imagens ficam em `docs/images/`.

### Dashboard
![Dashboard](docs/images/dashboard.png)

### Produtos (com destaque de baixo estoque)
![Produtos](docs/images/products.png)

### Categorias
![Categorias](docs/images/categories.png)

### Pessoas (Clientes/Fornecedores)
![Pessoas](docs/images/people.png)

### Movimentações (Entrada/Saída)
![Movimentações](docs/images/movements.png)

---

## Funcionalidades

### Produtos
- Cadastro e edição de produtos com:
  - Nome, SKU, categoria
  - Preço de venda
  - Estoque mínimo
- **SKU automático** no cadastro (ex.: `SKU-001`, `SKU-002`, ...).
- **Estoque controlado por movimentações** (quantidade não é editada manualmente no cadastro).
- Destaque visual na tabela (pseudo-classes JavaFX + CSS):
  - **Estoque zerado** (destaque vermelho)
  - **Estoque baixo** (destaque laranja) quando `quantidade <= estoque mínimo`
- **Ativar / Inativar** produto e filtro **“Mostrar inativos”**.
- **Exclusão protegida**:
  - Produto com movimentações não pode ser excluído (FK).
  - A interface sugere **inativar** quando a exclusão é bloqueada.

### Categorias
- Cadastro de categorias.
- Categoria padrão/fallback (ex.: “Geral”) para facilitar uso inicial.

### Pessoas (Clientes/Fornecedores)
- Cadastro com:
  - Tipo: **Cliente** ou **Fornecedor**
  - Documento **CPF/CNPJ** com validação
  - Contato e endereço
- **Ativar / Inativar** e filtro **“Mostrar inativos”**.
- **Exclusão protegida**:
  - Pessoa com movimentações não pode ser excluída (FK).
  - A interface sugere **inativar** quando a exclusão é bloqueada.

### Movimentações (Entrada/Saída)
- Movimentações com cabeçalho e itens:
  - **Entrada** vinculada a **Fornecedor**
  - **Saída** vinculada a **Cliente**
- Registro de itens com quantidade, valor unitário e subtotal.
- Atualização de estoque conforme movimentação.

### Dashboard
- Tela de visão geral do sistema (base para KPIs e evolução do projeto).

---

## Regras de negócio (importantes)

### 1) Estoque não é “digitado”: é calculado por movimentações
Para evitar divergências, o estoque do produto **não é alterado diretamente** no cadastro.  
O estoque é impactado somente por movimentações:

- **Entrada**: soma no estoque
- **Saída**: subtrai do estoque

Benefícios:
- Menos inconsistência
- Melhor auditoria (histórico de como chegou naquele número)
- Facilidade para evoluir com relatórios

### 2) Integridade referencial: histórico é preservado
O projeto usa **Foreign Keys** no SQLite (com `PRAGMA foreign_keys = ON`) para impedir exclusões que quebrariam o histórico:

- Produto usado em movimentações → **não pode excluir**
- Pessoa (cliente/fornecedor) usada em movimentações → **não pode excluir**

Quando isso acontece, o comportamento esperado é:
- **Manter o registro**
- **Inativar** (`active = 0`) para tirar da operação sem perder histórico

### 3) Ativo/Inativo (soft delete)
Em vez de apagar dados que já se conectam ao histórico, o sistema trabalha com status:

- **Ativo**: aparece por padrão nas listagens
- **Inativo**: só aparece se marcar **“Mostrar inativos”**
- É possível **Reativar** quando necessário

### 4) Estoque mínimo e alerta visual
O estoque mínimo serve como “ponto de atenção” para reposição. Na listagem:

- `quantidade == 0` → **estoque zerado** (destaque vermelho)
- `quantidade <= estoqueMinimo` → **estoque baixo** (destaque laranja)

---

## Arquitetura (visão geral)

Organização em camadas simples:

1. **UI (Views JavaFX)**  
   Telas como `ProductsView`, `MovementsView`, `PartiesView`, etc.

2. **Controllers (Forms)**  
   Controllers FXML como `ProductFormController`, responsáveis por:
   - Ler dados do formulário
   - Validar entrada
   - Chamar DAOs
   - Fechar modal e retornar status de “salvou/cancelou”

3. **DAO (Acesso a dados)**  
   Classes como `ProductDao`, `PartyDao`, `StockMovementDao`:
   - Executam SQL
   - Aplicam regras de integridade e validação
   - Retornam modelos (`Product`, `Party`, ...)

4. **DB (Conexão + Migrações)**  
   `Database` concentra:
   - conexão SQLite
   - `PRAGMA foreign_keys`
   - migrações versionadas via `user_version`

---

## Diagramas (fluxo e arquitetura)

### Fluxo de estoque (Entrada/Saída → Atualiza estoque)

```mermaid
flowchart LR
  A[Usuário cria Movimentação] --> B{Tipo}
  B -->|Entrada| C[Seleciona Fornecedor]
  B -->|Saída| D[Seleciona Cliente]
  C --> E[Adiciona Itens (produto, qtd, valor)]
  D --> E
  E --> F[Salvar Movimentação]
  F --> G[Persistir cabeçalho + itens (SQLite)]
  G --> H[Atualizar estoque do produto]
  H --> I[Produtos exibem alerta: baixo/zerado]
```

### Camadas do projeto (UI → DAO → DB)

```mermaid
flowchart TB
  UI[UI JavaFX (Views)] --> CT[Controllers (Forms)]
  CT --> DAO[DAO (SQL / Regras)]
  DAO --> DB[Database (SQLite + Migrações)]
  DB --> DAO
  DAO --> CT
  CT --> UI
```

---

## Tecnologias
- **Java 21**
- **JavaFX 21**
- **SQLite** (JDBC)
- **Maven**
- UI com **FXML** + tema em **CSS** (`app.css`)

---

## Como baixar e executar

### Opção 1 — Baixar pelo GitHub (ZIP)
1. Acesse: https://github.com/MatheusO-Machado/stockcontrol
2. Clique em **Code → Download ZIP**
3. Extraia a pasta
4. Abra um terminal na pasta do projeto
5. Execute:
```bash
mvn clean javafx:run
```

### Opção 2 — Clonar com Git
```bash
git clone https://github.com/MatheusO-Machado/stockcontrol.git
cd stockcontrol
mvn clean javafx:run
```

### Requisitos
- Java **21+** (`java -version`)
- Maven **3.9+** (`mvn -version`)

---

## Banco de dados e migrações

- O SQLite é criado automaticamente em: `./data/stockcontrol.db`
- O projeto usa versionamento de schema via:
  - `PRAGMA user_version`

Ao iniciar:
1. Verifica a versão do banco
2. Executa migrações necessárias
3. Atualiza a versão

E também garante:
- `PRAGMA foreign_keys = ON` em cada conexão

---

## Estrutura do projeto
- `src/main/java/.../ui/` telas (Views)
- `src/main/java/.../ui/controller/` controllers (forms)
- `src/main/java/.../dao/` DAOs (SQLite)
- `src/main/java/.../db/` conexão e migrações
- `src/main/resources/.../ui/` FXML e CSS
- `docs/images/` screenshots do projeto

---

## Próximos passos (ideias)
- Relatórios (CSV/PDF)
- KPIs e gráficos no dashboard
- Exportação/importação de dados
- Testes automatizados (DAO/migrações)
- Melhorias de validação (telefone, CEP etc.)

---

## Licença
Este projeto está sob a licença **CC BY-NC 4.0 (Atribuição — Não Comercial)**.

- Você pode **usar, modificar e redistribuir** para fins **não comerciais** (ex.: estudos).
- É **proibido** qualquer uso **comercial**.

Veja: [LICENSE](LICENSE).