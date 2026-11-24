package br.com.studyai.service;

import br.com.studyai.enums.TipoGeracao;
import br.com.studyai.model.Conteudo;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OpenAiServiceTest {

    private final Conteudo conteudo = new Conteudo("Fotossíntese", "Biologia",
            "A planta utiliza luz, água e gás carbônico para produzir açúcares.");
    private final ChatClient chatClient = mock(ChatClient.class);
    private final ChatClient.ChatClientRequestSpec prompt = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
    private final ChatClient.CallResponseSpec resposta = mock(ChatClient.CallResponseSpec.class);

    @Test
    void chaveAusenteImpedeRequisicao() {
        OpenAiService service = criarServico("");

        assertThatThrownBy(() -> service.gerar(conteudo, TipoGeracao.RESUMO))
                .isInstanceOf(GeracaoIAException.class)
                .hasMessageContaining("OPENAI_API_KEY");

        verifyNoInteractions(chatClient);
    }

    @Test
    void rejeitaRespostaEmBranco() {
        OpenAiService service = criarServico("chave-ficticia-de-teste");
        prepararResposta(" \n ");

        assertThatThrownBy(() -> service.gerar(conteudo, TipoGeracao.QUESTOES))
                .isInstanceOf(GeracaoIAException.class)
                .hasMessageContaining("resposta vazia");
    }

    @Test
    void mantemParagrafosDaResposta() {
        OpenAiService service = criarServico("chave-ficticia-de-teste");
        String texto = "A luz fornece energia.\n\nA planta usa essa energia para produzir açúcares.";
        prepararResposta(texto);

        assertThat(service.gerar(conteudo, TipoGeracao.EXPLICACAO)).isEqualTo(texto);
    }

    private OpenAiService criarServico(String chave) {
        ChatClient.Builder builder = mock(ChatClient.Builder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(chatClient);
        return new OpenAiService(builder, chave);
    }

    private void prepararResposta(String texto) {
        when(chatClient.prompt()).thenReturn(prompt);
        when(prompt.call()).thenReturn(resposta);
        when(resposta.content()).thenReturn(texto);
    }
}
