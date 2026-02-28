# StockControl — Controle de Estoque (JavaFX + SQLite)

Aplicação desktop de controle de estoque construída com **Java 21**, **JavaFX 21** e **SQLite**.  
O projeto consolida conceitos de **CRUD**, **movimentação de estoque**, **integridade referencial (FK)**, **migrações de banco**, e uma UI com estilo “ERP” usando **FXML + CSS**.

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
- **Estoque controlado exclusivamente por movimentações**:
  - O campo **Quantidade** não é editado manualmente no cadastro (evita inconsistências).
- Destaque visual na tabela (JavaFX pseudo-class + CSS):
  - **Estoque zerado** (vermelho)
  - **Estoque baixo** (laranja) quando `quantidade <= estoque mínimo`
- **Ativar/Inativar**:
  - Produto pode ser inativado (em vez de excluído).
  - Filtro **“Mostrar inativos”** na listagem.
- **Exclusão protegida**:
  - Não permite excluir produto que já teve movimentações (integridade via Foreign Key).
  - Quando a exclusão não é possível, a interface sugere **inativar**.

### Categorias
- Cadastro de categorias.
- Categoria padrão/fallback (ex.: “Geral”) para facilitar uso inicial.

### Pessoas (Clientes/Fornecedores)
- Cadastro de pessoas com:
  - Tipo: **Cliente** ou **Fornecedor**
  - Documento **CPF/CNPJ** com validação (apenas dígitos + validação)
  - Contato e endereço
- **Ativar/Inativar**:
  - Pessoas podem ser inativadas para “sair da operação” sem perder histórico.
  - Filtro **“Mostrar inativos”** na listagem.
- **Exclusão protegida**:
  - Não permite excluir pessoa com movimentações vinculadas (integridade via Foreign Key).
  - Quando a exclusão não é possível, a interface sugere **inativar**.

### Movimentações (Entrada/Saída)
- Registro de movimentações com cabeçalho e itens:
  - **Entrada** vinculada a **Fornecedor**
  - **Saída** vinculada a **Cliente**
- Itens: produto, quantidade, valor unitário e subtotal.
- Atualização de estoque conforme movimentação.

### Dashboard
- Tela de visão geral do sistema (base para KPIs e evolução do projeto).

---

## Regras de negócio (importantes)

### 1) Estoque não é “digitado”: é calculado por movimentações
Para evitar divergências, o estoque do produto não é alterado diretamente pelo usuário no cadastro.  
O estoque é impactado somente por:
- **Entrada**: soma no estoque
- **Saída**: subtrai do estoque

Isso facilita auditoria e mantém consistência com o histórico.

### 2) Integridade referencial: histórico é preservado
O projeto usa **Foreign Keys** no SQLite (`PRAGMA foreign_keys = ON`) para garantir integridade:
- Se um **produto** já foi usado em movimentações, ele **não pode ser excluído**
- Se uma **pessoa** (cliente/fornecedor) já foi usada em movimentações, ela **não pode ser excluída**

Em ambos os casos, o caminho recomendado é:
- **inativar** (campo `active = 0`) para remover da operação sem apagar dados.

### 3) Ativo/Inativo
- Registros inativos não aparecem por padrão
- É possível marcar “**Mostrar inativos**”
- O usuário pode **Reativar** a qualquer momento

### 4) Estoque mínimo e alerta visual
Cada produto tem um **estoque mínimo**. A listagem aplica destaque visual:
- `quantidade == 0` → **estoque zerado**
- `quantidade <= estoqueMinimo` → **estoque baixo**

---

## Tecnologias
- **Java 21**
- **JavaFX 21**
- **SQLite** (JDBC)
- **Maven**
- UI com **FXML** e tema em **CSS** (`app.css`)

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
- O projeto versiona o schema com:
  - `PRAGMA user_version`

Ao iniciar a aplicação:
1. Verifica a versão do banco
2. Executa migrações necessárias
3. Atualiza `user_version`

E também garante:
- `PRAGMA foreign_keys = ON` em cada conexão

---

## Estrutura do projeto (resumo)

- `src/main/java/.../ui/`  
  Telas (Views) JavaFX
- `src/main/java/.../ui/controller/`  
  Controllers dos formulários e ações
- `src/main/java/.../dao/`  
  DAOs (acesso ao SQLite)
- `src/main/java/.../db/`  
  Conexão e migrações do banco
- `src/main/resources/.../ui/`  
  FXML e CSS (`app.css`)
- `docs/images/`  
  Screenshots para documentação

---

## Próximos passos (ideias)
- Relatórios (CSV/PDF)
- Gráficos no dashboard (KPI)
- Exportação/importação
- Testes automatizados para DAOs/migrações
- Melhorias de validação (telefone, CEP, etc.)