package br.com.studyai;

import br.com.studyai.service.GeminiChatClientFactory;
import com.google.genai.Client;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

// Sobe a aplicação inteira, com o H2 no lugar do MySQL, sem chave do Gemini configurada no servidor. No Maven, o
// ambiente ainda tem GEMINI_API_KEY (veja o pom.xml), e mesmo assim nenhum cliente do Google é criado na partida.
@SpringBootTest
@ActiveProfiles("h2")
class AplicacaoSemChaveNoServidorTest {

    @Autowired
    private ApplicationContext aplicacao;

    @Test
    void sobeSemChaveECriaOClienteDoGeminiSoNaHoraDeGerar() {
        assertThat(aplicacao.getBeanNamesForType(Client.class)).isEmpty();
        assertThat(aplicacao.getBeanNamesForType(ChatModel.class)).isEmpty();
        assertThat(aplicacao.getBeanNamesForType(ChatClient.class)).isEmpty();
        assertThat(aplicacao.getBeanNamesForType(ChatClient.Builder.class)).isEmpty();
        assertThat(aplicacao.getBeanNamesForType(GeminiChatClientFactory.class)).hasSize(1);
    }
}
