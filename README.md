# StudyAI

O StudyAI é um assistente de estudos em português. Você cadastra um conteúdo, usa a IA para revisá-lo e consulta as gerações salvas no histórico daquele material.

O projeto foi desenvolvido para uma atividade acadêmica de integração com IA generativa. Ele reúne uma aplicação web com páginas renderizadas no servidor, persistência no MySQL e uma chamada à API da OpenAI por meio do Spring AI.

## Funcionalidades

- Cadastrar, listar, visualizar, editar e excluir conteúdos com título, disciplina, texto e data de criação automática.
- Gerar um resumo com os conceitos principais.
- Gerar cinco questões de múltipla escolha, com quatro alternativas por questão e gabarito no final.
- Solicitar uma explicação em linguagem simples com exemplos práticos.
- Consultar gerações anteriores sem chamar a API novamente.

## Tecnologias

| Tecnologia | Uso |
| --- | --- |
| Java 21 e Spring Boot 4.2.0 | Aplicação web e configuração |
| Spring AI 2.1.0 e OpenAI | Geração de textos com `ChatClient` |
| Spring Data JPA e Hibernate | Persistência e criação das tabelas |
| MySQL e Connector/J | Banco de dados local |
| Thymeleaf, HTML, CSS, Bootstrap 5.3.8 e JavaScript | Interface web |
| Maven 3.9.16 via Maven Wrapper | Compilação, testes e execução |

As versões do Spring Boot e do Spring AI são compatíveis entre si. O `pom.xml` importa o BOM do Spring AI para gerenciar a versão do starter oficial `spring-ai-starter-model-openai`. [Versões do Spring AI](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-GA-available-now/) · [Requisitos do Spring Boot](https://docs.spring.io/spring-boot/4.2/system-requirements.html)

## Banco de dados

| Tabela | Campos |
| --- | --- |
| `conteudos` | `id`, `titulo`, `disciplina`, `texto`, `data_criacao` |
| `geracoes_ia` | `id`, `conteudo_id`, `tipo`, `resposta`, `data_criacao` |

Um conteúdo pode ter várias gerações. `geracoes_ia.conteudo_id` referencia o conteúdo e remove suas gerações em cascata quando ele é excluído. Os campos `texto` e `resposta` usam `LONGTEXT`. A tabela de gerações registra o tipo `RESUMO`, `QUESTOES` ou `EXPLICACAO`.

## Executar no Windows

### 1. Criar o banco no MySQL local

Com o serviço MySQL ativo, abra [database/create-database.sql](database/create-database.sql) no MySQL Workbench e execute o script. Pelo cliente de linha de comando, execute o equivalente abaixo na raiz do projeto. Use o caminho completo para `mysql.exe` se ele estiver fora do `PATH`.

```powershell
mysql -h 127.0.0.1 -P 3306 -u root -p -e 'source database/create-database.sql'
```

O Hibernate cria as tabelas no banco `studyai` na primeira execução. O usuário configurado precisa ter permissão para criar e atualizar as tabelas e manipular seus registros.

### 2. Definir as variáveis de ambiente

Na janela do PowerShell em que você vai executar a aplicação, configure a conexão. Substitua `root` caso use outro usuário no seu MySQL.

```powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/studyai'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host -AsSecureString 'Senha do MySQL')).Password
```

Para habilitar as gerações, crie uma chave no painel da OpenAI para um projeto com acesso à API. Configure a chave nessa mesma janela. Use `OPENAI_MODEL` para escolher outro modelo compatível com Chat Completions. O padrão é `gpt-4.1-mini`. [Criar chave da API](https://platform.openai.com/api-keys) · [Guia de configuração da OpenAI](https://developers.openai.com/api/docs/quickstart/) · [GPT-4.1 mini](https://developers.openai.com/api/docs/models/gpt-4.1-mini)

```powershell
$env:OPENAI_MODEL = 'gpt-4.1-mini'
$env:OPENAI_API_KEY = [System.Net.NetworkCredential]::new('', (Read-Host -AsSecureString 'Chave da OpenAI')).Password
```

O cadastro, a edição e a consulta ao histórico funcionam sem a chave. Quando ela está ausente, os botões de geração orientam o usuário a configurá-la. O arquivo [.env.example](.env.example) lista as variáveis com valores de exemplo e serve como referência para o terminal.

### 3. Iniciar a aplicação

Confirme que o JDK 21 está disponível com `java -version`. Execute na raiz do projeto:

```powershell
.\mvnw.cmd spring-boot:run
```

Abra [StudyAI](http://localhost:8080) no navegador. Na primeira execução, o Maven Wrapper baixa a distribuição e as dependências. Em Linux ou macOS, o comando equivalente é `./mvnw spring-boot:run`. [Documentação do Maven Wrapper](https://maven.apache.org/wrapper/)

Para compilar, executar os testes automatizados e gerar o JAR:

```powershell
.\mvnw.cmd verify
```

Os testes automatizados cobrem rotas, validação e fluxos da IA com dependências controladas. Para conferir a integração real, inicie a aplicação com as credenciais, cadastre um conteúdo e acione os três botões de geração. Reabra o conteúdo para conferir as respostas no histórico.

## Arquitetura e Spring AI

- `controller` recebe os formulários, valida os campos e seleciona as páginas Thymeleaf.
- `service` executa o CRUD e coordena as gerações. `OpenAiService` monta a chamada à OpenAI e `GeracaoIAService` salva a resposta e consulta o histórico.
- `repository` usa Spring Data JPA para as consultas e operações de persistência.
- `model` define as entidades JPA. `enums` define os tipos de geração.

Cada ação de IA usa instruções específicas e envia o título, a disciplina e o texto do conteúdo selecionado ao `ChatClient`. O serviço recebe a resposta síncrona e só grava o histórico quando ela contém texto. A interface exibe as respostas como texto escapado e preserva as quebras de linha. [Referência do ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html)

## Ilustração da página inicial

A imagem [study-desk.webp](src/main/resources/static/img/study-desk.webp) foi criada com a ferramenta integrada de geração de imagens e convertida para WebP.

<details>
<summary>Prompt da imagem</summary>

```text
Use case: photorealistic-natural. Create a landscape 3:2 editorial study-desk photograph for the right panel of an academic study assistant web homepage. Center a large open ivory notebook with subtle ruled blank pages, a diagonal lavender pencil, two closed aubergine and cream books, and one restrained lime green page tab. Art direct it as a tactile modern design magazine still life, on muted slate-aubergine paper with a soft oval of afternoon sunshine, visible grain and believable natural shadows. Keep the main subject in the central safe area for responsive cropping. Color palette: deep purple #251a43 on the corners, cream paper, touches of lavender and pale chartreuse #d9ef90. Visually rich and realistic yet uncluttered. Full bleed with no framing, no legible lettering, no logos, no people or watermark.
```

</details>
