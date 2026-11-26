# StudyAI

O StudyAI é um assistente de estudos em português. Você cadastra um conteúdo, usa a IA para revisá-lo e consulta as gerações salvas no histórico daquele material.

O projeto foi desenvolvido para uma atividade acadêmica de integração com IA generativa. Ele reúne uma aplicação web com páginas renderizadas no servidor, persistência no MySQL e uma chamada à API do Google Gemini por meio do Spring AI.

## Funcionalidades

- Cadastrar, listar, visualizar, editar e excluir conteúdos com título, disciplina, texto e data de criação automática.
- Gerar um resumo com os conceitos principais.
- Gerar cinco questões de múltipla escolha, com quatro alternativas por questão e gabarito no final.
- Solicitar uma explicação em linguagem simples com exemplos práticos.
- Consultar gerações anteriores sem chamar a API novamente.
- Salvar a chave da API do Gemini somente no navegador, na tela **Chave da API**.

## Tecnologias

| Tecnologia | Uso |
| --- | --- |
| Java 21 e Spring Boot 4.2.0 | Aplicação web e configuração |
| Spring AI 2.1.0 e Google Gemini | Geração de textos com `ChatClient` e o módulo oficial Google GenAI |
| Spring Data JPA e Hibernate | Persistência e criação das tabelas |
| MySQL e Connector/J | Banco de dados local |
| H2 | Banco em memória do perfil `h2`, para testar sem MySQL |
| Thymeleaf, HTML, CSS, Bootstrap 5.3.8 e JavaScript | Interface web |
| Maven 3.9.16 via Maven Wrapper | Compilação, testes e execução |

As versões do Spring Boot e do Spring AI são compatíveis entre si. O `pom.xml` importa o BOM do Spring AI para gerenciar a versão do starter oficial `spring-ai-starter-model-google-genai`, que usa o SDK Java oficial do Google (`com.google.genai:google-genai`). [Versões do Spring AI](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-GA-available-now/) · [Requisitos do Spring Boot](https://docs.spring.io/spring-boot/4.2/system-requirements.html)

## Banco de dados

| Tabela | Campos |
| --- | --- |
| `conteudos` | `id`, `titulo`, `disciplina`, `texto`, `data_criacao` |
| `geracoes_ia` | `id`, `conteudo_id`, `tipo`, `resposta`, `data_criacao` |

Um conteúdo pode ter várias gerações. `geracoes_ia.conteudo_id` referencia o conteúdo e remove suas gerações em cascata quando ele é excluído. Os campos `texto` e `resposta` usam `LONGTEXT`. A tabela de gerações registra o tipo `RESUMO`, `QUESTOES` ou `EXPLICACAO`.

## Executar no Windows

Os comandos abaixo rodam na raiz do projeto, a pasta que contém o `pom.xml` e o `mvnw.cmd`.

Sem MySQL instalado? Pule os passos 1 e 2 e inicie a aplicação com o perfil `h2`, que usa um banco em memória:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'h2'
.\mvnw.cmd spring-boot:run
```

Tudo funciona da mesma forma, mas os conteúdos e o histórico ficam só na memória e somem quando a aplicação para. Para voltar ao MySQL, abra uma nova janela do PowerShell ou execute `Remove-Item Env:SPRING_PROFILES_ACTIVE`.

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

Opcionalmente, use `GEMINI_MODEL` para escolher outro modelo do Gemini. O padrão é `gemini-3.6-flash`, um modelo estável com camada gratuita, e vale também quando a variável está vazia. A chave do Gemini não é configurada aqui: ela é salva no navegador, como mostra o passo 4. [Modelos do Gemini](https://ai.google.dev/gemini-api/docs/models)

```powershell
$env:GEMINI_MODEL = 'gemini-3.6-flash'
```

O arquivo [.env.example](.env.example) lista as variáveis com valores de exemplo e serve como referência para o terminal.

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

Os testes automatizados cobrem rotas, validação, renderização das páginas, as proteções contra outros sites e os fluxos da IA com dependências controladas. Nenhum deles precisa do MySQL nem de chave do Gemini: um deles sobe a aplicação inteira com o H2 em memória e confirma que nenhum cliente do Google é criado na partida. Outro sobe um servidor HTTP local no lugar do Gemini para conferir que cada geração envia a chave recebida do navegador no cabeçalho `x-goog-api-key`, faz uma única chamada, devolve os erros sem demora e respeita o tempo limite. No Maven, os testes rodam com variáveis de ambiente do Google que tentariam trocar a chave, o modo e o endereço da API, para confirmar que elas não têm efeito. Para conferir a integração real, inicie a aplicação, salve sua chave, cadastre um conteúdo e acione os três botões de geração. Reabra o conteúdo para conferir as respostas no histórico.

### 4. Salvar a chave da API no navegador

Crie uma chave no Google AI Studio; para usar a camada gratuita, não é preciso cadastrar cartão. No StudyAI, abra **Chave da API** no menu superior, cole a chave e clique em **Salvar chave**. [Criar chave da API](https://aistudio.google.com/apikey) · [Preços e camada gratuita](https://ai.google.dev/gemini-api/docs/pricing)

- Com **Lembrar neste navegador** marcado, a chave fica no `localStorage` até você removê-la.
- Desmarcado, ela fica no `sessionStorage` e vale só para aquela aba. O navegador ainda pode restaurá-la se a aba fechada for reaberta (Ctrl+Shift+T) ou se a sessão for restaurada. Em computadores compartilhados, clique em **Remover chave** antes de sair ou use uma janela anônima.
- Mudar a opção com uma chave já salva vale na hora, sem precisar colar a chave de novo.
- **Remover chave** apaga a chave do `localStorage` e do `sessionStorage` e invalida as cópias que outras abas ainda tenham.

O servidor não guarda a chave. A cada geração, o JavaScript a envia no cabeçalho `X-Chave-Gemini` de um pedido em segundo plano (`fetch`), e o StudyAI a repassa ao Gemini no cabeçalho `x-goog-api-key` dessa mesma chamada. Nenhuma variável de ambiente do Google muda a chave ou o destino da chamada: `GEMINI_API_KEY` e `GOOGLE_API_KEY` não são usadas, e `GOOGLE_GENAI_USE_VERTEXAI`, `GOOGLE_GENAI_USE_ENTERPRISE` e `GOOGLE_GEMINI_BASE_URL` não desviam o pedido para outro serviço. Vale sempre a chave salva na tela, enviada à API do Gemini. A chave nunca vai no corpo de um formulário de navegação, para não ficar no histórico da aba, que o navegador grava em disco. Ela não é gravada no banco, na sessão, na URL nem nos logs. O cadastro, a edição e a consulta ao histórico funcionam sem a chave; sem ela, o painel de IA indica onde salvá-la. A geração depende de JavaScript, porque é ele que lê a chave do navegador.

A chave fica associada ao endereço aberto no navegador: `http://localhost:8080` e `http://127.0.0.1:8080` têm armazenamentos separados. Qualquer outra aplicação aberta depois nesse mesmo endereço consegue ler o `localStorage` dele, então remova a chave se for usar a porta 8080 para outro projeto. A aplicação escuta apenas em `127.0.0.1`, e a chave não trafega pela rede local. Se publicar o StudyAI em outro endereço, use HTTPS.

## Proteções contra outros sites

Mesmo rodando só no seu computador, a aplicação recebe requisições de qualquer página aberta no navegador. O filtro `SegurancaWebFilter` cuida disso:

- recusa formulários e pedidos enviados por páginas de outra origem (CSRF), com base nos cabeçalhos `Sec-Fetch-Site` e `Origin`. Cada recusa é registrada no log, e a página mostra o motivo;
- aceita apenas os endereços listados em `studyai.hosts-permitidos` (padrão: `localhost` e `127.0.0.1`), o que bloqueia ataques de DNS rebinding. Para acessar por outro nome, inclua-o nessa propriedade;
- envia `Content-Security-Policy`, `X-Frame-Options`, `X-Content-Type-Options` e `Referrer-Policy`. A política só permite scripts e estilos da própria aplicação, o que também protege a chave salva no navegador.

## Arquitetura e Spring AI

- `controller` recebe os formulários, valida os campos e seleciona as páginas Thymeleaf.
- `service` executa o CRUD e coordena as gerações. `GeminiService` valida a chave recebida e monta a chamada ao Gemini, `GeminiChatClientFactory` cria o `ChatClient` de cada geração com essa chave e `GeracaoIAService` salva a resposta e consulta o histórico.
- `repository` usa Spring Data JPA para as consultas e operações de persistência.
- `model` define as entidades JPA. `enums` define os tipos de geração.
- `config` contém o `SegurancaWebFilter`, descrito em [Proteções contra outros sites](#proteções-contra-outros-sites).
- `static/js/studyai.js` salva a chave no armazenamento do navegador e faz o pedido de geração com a chave no cabeçalho; o servidor responde com a página a abrir, e a mensagem de resultado chega como atributo flash. Os arquivos JS e CSS são servidos com um hash do conteúdo no nome, para o navegador não reaproveitar uma versão antiga em cache.

Cada ação de IA usa instruções específicas e envia o título, a disciplina e o texto do conteúdo selecionado ao `ChatClient`. Como a chave muda a cada usuário, a autoconfiguração do modelo de chat fica desligada (`spring.ai.model.chat=none`). A cada geração, a fábrica abre um cliente do SDK oficial do Google com a chave do navegador, monta o `GoogleGenAiChatModel` do Spring AI e fecha o cliente no fim. Não há novas tentativas automáticas, nem no Spring AI nem no SDK: cada clique faz uma única chamada e não gasta a cota gratuita em dobro, e os erros aparecem sem demora. O serviço recebe a resposta síncrona e só grava o histórico quando ela contém texto. Uma resposta vazia, que pode indicar bloqueio pelos filtros de segurança do Gemini, vira uma mensagem na tela. Falhas como chave recusada ou bloqueada, cota gratuita atingida, modelo inexistente ou sem cota gratuita para o projeto, texto longo demais, instabilidade do Gemini ou tempo esgotado também viram mensagens específicas. O log registra apenas o código HTTP e o status do erro, nunca a mensagem do Google nem a chave. A interface exibe as respostas como texto escapado e preserva as quebras de linha. [Referência do ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html)

## Camada gratuita do Gemini

- O modelo padrão, `gemini-3.6-flash`, é estável e tem camada gratuita. O `gemini-2.5-flash` foi descontinuado e deixa de funcionar em 1º de dezembro de 2026, por isso não é usado. Para trocar de modelo, defina `GEMINI_MODEL`; `gemini-3.5-flash-lite` é uma alternativa gratuita mais leve. [Modelos](https://ai.google.dev/gemini-api/docs/models) · [Descontinuações](https://ai.google.dev/gemini-api/docs/deprecations)
- Os limites por minuto e por dia valem por projeto do Google e por modelo, e não por chave. Eles aparecem no [Google AI Studio](https://aistudio.google.com/rate-limit), e a cota diária é renovada à meia-noite no horário do Pacífico, entre 4h e 5h no horário de Brasília.
- Quando um limite por minuto acaba, o StudyAI mostra quanto tempo esperar. Quando acaba a cota diária, avisa que ela só volta à meia-noite do Pacífico, mesmo que o Google sugira esperar poucos segundos; até lá, outro modelo gratuito em `GEMINI_MODEL` pode funcionar. Se o projeto não tiver cota gratuita para o modelo escolhido, a mensagem também diz isso.
- Na camada gratuita, o Google pode usar o conteúdo enviado para melhorar os produtos dele. Evite cadastrar materiais com dados pessoais ou sigilosos.

## Chaves fora do GitHub

A chave do Gemini nunca fica no projeto: ela existe só no navegador de quem a salvou, e o servidor a usa apenas durante cada geração. Nenhum arquivo versionado contém chaves. O `.gitignore` ignora `.env`, `.env.*` (exceto o `.env.example`) e `application-local.properties`, para o caso de você guardar configurações locais. Não coloque chaves no `application.properties`, no README nem em outros arquivos do repositório. Se uma chave for publicada por engano, exclua-a no Google AI Studio e crie outra, já que o Google bloqueia chaves vazadas.

## Ilustração da página inicial

A imagem [study-desk.webp](src/main/resources/static/img/study-desk.webp) foi criada com a ferramenta integrada de geração de imagens e convertida para WebP.

<details>
<summary>Prompt da imagem</summary>

```text
Use case: photorealistic-natural. Create a landscape 3:2 editorial study-desk photograph for the right panel of an academic study assistant web homepage. Center a large open ivory notebook with subtle ruled blank pages, a diagonal lavender pencil, two closed aubergine and cream books, and one restrained lime green page tab. Art direct it as a tactile modern design magazine still life, on muted slate-aubergine paper with a soft oval of afternoon sunshine, visible grain and believable natural shadows. Keep the main subject in the central safe area for responsive cropping. Color palette: deep purple #251a43 on the corners, cream paper, touches of lavender and pale chartreuse #d9ef90. Visually rich and realistic yet uncluttered. Full bleed with no framing, no legible lettering, no logos, no people or watermark.
```

</details>
