package br.com.studyai.service;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import java.time.Duration;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;

/**
 * Cria o {@link ChatClient} do Gemini para cada geração, com a chave da API enviada pelo navegador.
 *
 * <p>O servidor não guarda a chave: cada geração abre um cliente do Google com ela e o fecha logo depois. A chave, o
 * endereço da API e o modo (Gemini API, e não Vertex AI) são sempre definidos aqui. Assim, nenhuma variável de
 * ambiente do Google, como GOOGLE_API_KEY, GEMINI_API_KEY, GOOGLE_GENAI_USE_VERTEXAI ou GOOGLE_GEMINI_BASE_URL, muda
 * a chave usada nem o destino da chamada. Não há novas tentativas automáticas, para que cada clique faça uma única
 * chamada e não gaste a cota gratuita em dobro.
 */
@Component
public class GeminiChatClientFactory {

    static final String MODELO_PADRAO = "gemini-3.6-flash";
    static final String URL_GEMINI = "https://generativelanguage.googleapis.com/";

    // Uma única tentativa e nenhum código HTTP para repetir. Só com attempts(1), o SDK ainda espera cerca de 1 s
    // antes de devolver erros como 429 e 503.
    private static final HttpRetryOptions SEM_NOVAS_TENTATIVAS_HTTP =
            HttpRetryOptions.builder().attempts(1).httpStatusCodes(List.of()).build();

    private final String urlBase;
    private final int tempoLimiteMs;
    private final GoogleGenAiChatOptions opcoes;
    private final RetryTemplate semNovasTentativas = new RetryTemplate(RetryPolicy.builder().maxRetries(0).build());

    public GeminiChatClientFactory(@Value("${studyai.gemini.url-base:}") String urlBase,
            @Value("${studyai.gemini.modelo:}") String modelo,
            @Value("${studyai.gemini.tempo-limite}") Duration tempoLimite) {
        this.urlBase = urlBase == null || urlBase.isBlank() ? URL_GEMINI : urlBase.strip();
        this.tempoLimiteMs = Math.toIntExact(tempoLimite.toMillis());
        // GEMINI_MODEL definida, mas vazia, chega aqui como texto vazio.
        String nomeDoModelo = modelo == null || modelo.isBlank() ? MODELO_PADRAO : modelo.strip();
        this.opcoes = GoogleGenAiChatOptions.builder().model(nomeDoModelo).build();
    }

    public SessaoGemini abrir(String chaveApi) {
        Client cliente = criarCliente(chaveApi);
        GoogleGenAiChatModel modelo = GoogleGenAiChatModel.builder()
                .genAiClient(cliente)
                .options(opcoes)
                .retryTemplate(semNovasTentativas)
                .build();
        return new SessaoGemini(ChatClient.create(modelo), cliente::close);
    }

    Client criarCliente(String chaveApi) {
        HttpOptions http = HttpOptions.builder()
                .baseUrl(urlBase)
                .timeout(tempoLimiteMs)
                .retryOptions(SEM_NOVAS_TENTATIVAS_HTTP)
                .build();
        return Client.builder().vertexAI(false).apiKey(chaveApi).httpOptions(http).build();
    }

    /** O cliente do Gemini de uma única geração. Fechá-lo libera a conexão e descarta a chave. */
    public record SessaoGemini(ChatClient chatClient, Runnable aoFechar) implements AutoCloseable {

        @Override
        public void close() {
            aoFechar.run();
        }
    }
}
