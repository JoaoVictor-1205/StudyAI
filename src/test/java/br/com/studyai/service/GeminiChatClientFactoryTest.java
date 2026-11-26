package br.com.studyai.service;

import br.com.studyai.enums.TipoGeracao;
import br.com.studyai.model.Conteudo;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

// Usa um servidor HTTP local no lugar do Gemini para conferir o que a aplicação realmente envia. No Maven, os testes
// rodam com variáveis de ambiente do Google que tentam trocar a chave, o modo e o endereço (veja o pom.xml).
class GeminiChatClientFactoryTest {

    private static final String RESPOSTA = """
            {"candidates":[{"content":{"parts":[{"text":"Resposta simulada do Gemini."}],"role":"model"},
             "finishReason":"STOP","index":0}],
             "usageMetadata":{"promptTokenCount":3,"candidatesTokenCount":4,"totalTokenCount":7}}
            """;

    // Resposta real do Gemini quando a cota diária gratuita acaba: o Google sugere esperar poucos segundos mesmo assim.
    private static final String COTA_DIARIA_ESGOTADA = """
            {"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing \
            details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. \
            \\n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, \
            limit: 20, model: gemini-3.5-flash-lite\\nPlease retry in 11.615098148s.","status":"RESOURCE_EXHAUSTED",
             "details":[{"@type":"type.googleapis.com/google.rpc.QuotaFailure","violations":[{"quotaMetric":
             "generativelanguage.googleapis.com/generate_content_free_tier_requests",
             "quotaId":"GenerateRequestsPerDayPerProjectPerModel-FreeTier",
             "quotaDimensions":{"location":"global","model":"gemini-3.5-flash-lite"},"quotaValue":"20"}]},
             {"@type":"type.googleapis.com/google.rpc.RetryInfo","retryDelay":"11s"}]}}
            """;

    private final List<String> chaves = new CopyOnWriteArrayList<>();
    private final List<String> caminhos = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String corpo = RESPOSTA;
    private volatile long atrasoMs;
    private HttpServer servidor;
    private String urlBase;
    private GeminiChatClientFactory fabrica;

    @BeforeEach
    void iniciarGeminiSimulado() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.createContext("/", troca -> {
            chaves.add(troca.getRequestHeaders().getFirst("x-goog-api-key"));
            caminhos.add(troca.getRequestMethod() + " " + troca.getRequestURI().getPath());
            troca.getRequestBody().readAllBytes();
            try {
                Thread.sleep(atrasoMs);
            } catch (InterruptedException interrupcao) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            try {
                troca.getResponseHeaders().add("Content-Type", "application/json");
                troca.sendResponseHeaders(status, bytes.length);
                try (var saida = troca.getResponseBody()) {
                    saida.write(bytes);
                }
            } catch (IOException conexaoEncerrada) {
                // O cliente desistiu antes da resposta (teste de tempo limite).
            }
        });
        servidor.start();
        urlBase = "http://" + servidor.getAddress().getHostString() + ":" + servidor.getAddress().getPort() + "/";
        fabrica = new GeminiChatClientFactory(urlBase, "gemini-3.5-flash-lite", Duration.ofSeconds(5));
    }

    @AfterEach
    void encerrar() {
        servidor.stop(0);
    }

    @Test
    void enviaAChaveDeCadaGeracaoNoCabecalhoDoGoogleEUsaOModeloConfigurado() {
        String primeira = gerar(fabrica, "AIza-chave-do-navegador-1");
        String segunda = gerar(fabrica, "AIza-chave-do-navegador-2");

        assertThat(primeira).isEqualTo("Resposta simulada do Gemini.");
        assertThat(segunda).isEqualTo("Resposta simulada do Gemini.");
        assertThat(chaves).containsExactly("AIza-chave-do-navegador-1", "AIza-chave-do-navegador-2");
        assertThat(caminhos).containsExactly("POST /v1beta/models/gemini-3.5-flash-lite:generateContent",
                "POST /v1beta/models/gemini-3.5-flash-lite:generateContent");
    }

    // GEMINI_MODEL definida, mas vazia, não pode quebrar a geração.
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void usaOModeloPadraoQuandoNenhumModeloEhInformado(String modelo) {
        GeminiChatClientFactory semModelo = new GeminiChatClientFactory(urlBase, modelo, Duration.ofSeconds(5));

        assertThat(gerar(semModelo, "AIza-chave")).isEqualTo("Resposta simulada do Gemini.");
        assertThat(caminhos).containsExactly("POST /v1beta/models/" + GeminiChatClientFactory.MODELO_PADRAO
                + ":generateContent");
    }

    @Test
    void ignoraEspacosNasPontasDoModeloEDoEndereco() {
        GeminiChatClientFactory comEspacos = new GeminiChatClientFactory(" " + urlBase + " ",
                " gemini-3.5-flash-lite\n", Duration.ofSeconds(5));

        assertThat(gerar(comEspacos, "AIza-chave")).isEqualTo("Resposta simulada do Gemini.");
        assertThat(caminhos).containsExactly("POST /v1beta/models/gemini-3.5-flash-lite:generateContent");
    }

    // A chave e o modo vêm sempre da fábrica, mesmo com GOOGLE_GENAI_USE_VERTEXAI ou GEMINI_API_KEY no ambiente.
    @Test
    void usaSempreAGeminiApiComAChaveDoNavegador() {
        try (Client cliente = fabrica.criarCliente("AIza-chave-do-navegador")) {
            assertThat(cliente.vertexAI()).isFalse();
            assertThat(cliente.enterprise()).isFalse();
            assertThat(cliente.apiKey()).isEqualTo("AIza-chave-do-navegador");
        }
    }

    @Test
    void naoRepeteAChamadaQuandoACotaGratuitaAcaba() {
        status = 429;
        corpo = """
                {"error":{"code":429,"message":"You exceeded your current quota. Please retry in 30s.","status":"RESOURCE_EXHAUSTED"}}
                """;

        Throwable falha = catchThrowable(() -> gerar(fabrica, "AIza-chave"));

        assertThat(falha).hasRootCauseInstanceOf(ApiException.class);
        assertThat(((ApiException) raiz(falha)).code()).isEqualTo(429);
        assertThat(chaves).hasSize(1);
    }

    @Test
    void naoRepeteAChamadaQuandoOGeminiEstaSobrecarregado() {
        status = 503;
        corpo = """
                {"error":{"code":503,"message":"The model is overloaded. Please try again later.","status":"UNAVAILABLE"}}
                """;

        Throwable falha = catchThrowable(() -> gerar(fabrica, "AIza-chave"));

        assertThat(((ApiException) raiz(falha)).code()).isEqualTo(503);
        assertThat(chaves).hasSize(1);
    }

    // Sem a configuração de novas tentativas da fábrica, o SDK espera cerca de 1 s antes de devolver cada erro.
    @Test
    void devolveOsErrosDoGeminiSemEsperar() {
        status = 503;
        corpo = """
                {"error":{"code":503,"message":"The model is overloaded. Please try again later.","status":"UNAVAILABLE"}}
                """;
        catchThrowable(() -> gerar(fabrica, "AIza-aquecimento"));
        long inicio = System.nanoTime();

        for (int i = 0; i < 5; i++) {
            assertThat(catchThrowable(() -> gerar(fabrica, "AIza-chave"))).hasRootCauseInstanceOf(ApiException.class);
        }

        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(1));
        assertThat(chaves).hasSize(6);
    }

    @Test
    void desisteQuandoOGeminiPassaDoTempoLimite() {
        atrasoMs = 3000;
        GeminiChatClientFactory comPrazoCurto = new GeminiChatClientFactory(urlBase, "gemini-3.5-flash-lite",
                Duration.ofMillis(500));
        long inicio = System.nanoTime();

        Throwable falha = catchThrowable(() -> gerar(comPrazoCurto, "AIza-chave"));

        assertThat(falha).hasCauseInstanceOf(GenAiIOException.class);
        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofMillis(2500));
        assertThat(chaves).hasSize(1);
    }

    // O SDK repassa os detalhes do erro (o quotaId) na mensagem, e o serviço os usa para explicar qual cota acabou.
    @Test
    void explicaACotaDiariaComOsDetalhesQueOGoogleEnvia() {
        status = 429;
        corpo = COTA_DIARIA_ESGOTADA;
        GeminiService servico = new GeminiService(fabrica);
        Conteudo conteudo = new Conteudo("Fotossíntese", "Biologia", "A planta utiliza luz para produzir açúcares.");

        assertThatThrownBy(() -> servico.gerar(conteudo, TipoGeracao.RESUMO, "AIza-chave"))
                .isInstanceOf(GeracaoIAException.class)
                .hasMessageStartingWith("A cota diária gratuita do Gemini para este modelo acabou.")
                .hasMessageNotContaining("segundos");
        assertThat(chaves).hasSize(1);
    }

    private static String gerar(GeminiChatClientFactory fabrica, String chave) {
        try (GeminiChatClientFactory.SessaoGemini sessao = fabrica.abrir(chave)) {
            return sessao.chatClient().prompt().user("Olá").call().content();
        }
    }

    private static Throwable raiz(Throwable erro) {
        Throwable atual = erro;
        while (atual.getCause() != null && atual.getCause() != atual) {
            atual = atual.getCause();
        }
        return atual;
    }
}
