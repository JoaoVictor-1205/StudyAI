package br.com.studyai.service;

import br.com.studyai.enums.TipoGeracao;
import br.com.studyai.model.Conteudo;
import br.com.studyai.model.GeracaoIA;
import br.com.studyai.repository.GeracaoIARepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.CannotCreateTransactionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GeracaoIAServiceTest {

    private static final String CHAVE = "AIza-chave-de-teste";

    private final ConteudoService conteudos = mock(ConteudoService.class);
    private final GeracaoIARepository historico = mock(GeracaoIARepository.class);
    private final GeminiService gemini = mock(GeminiService.class);
    private final GeracaoIAService service = new GeracaoIAService(conteudos, historico, gemini);
    private final Conteudo conteudo = new Conteudo("Forças", "Física", "A força pode alterar o movimento.");

    @Test
    void salvaRespostaVinculadaAoConteudoSelecionado() {
        when(conteudos.buscar(7L)).thenReturn(conteudo);
        when(gemini.gerar(conteudo, TipoGeracao.RESUMO, CHAVE)).thenReturn("Forças alteram o movimento.");
        when(historico.save(any(GeracaoIA.class))).thenAnswer(chamada -> chamada.getArgument(0));

        GeracaoIA geracao = service.gerar(7L, TipoGeracao.RESUMO, CHAVE);

        assertThat(geracao.getConteudo()).isSameAs(conteudo);
        assertThat(geracao.getTipo()).isEqualTo(TipoGeracao.RESUMO);
        assertThat(geracao.getResposta()).isEqualTo("Forças alteram o movimento.");
    }

    @Test
    void falhaDeGeracaoNaoCriaRegistroNoHistorico() {
        when(conteudos.buscar(7L)).thenReturn(conteudo);
        when(gemini.gerar(conteudo, TipoGeracao.EXPLICACAO, CHAVE))
                .thenThrow(new GeracaoIAException("O Gemini não retornou texto."));

        assertThatThrownBy(() -> service.gerar(7L, TipoGeracao.EXPLICACAO, CHAVE))
                .isInstanceOf(GeracaoIAException.class);

        verifyNoInteractions(historico);
    }

    @Test
    void bancoIndisponivelAoSalvarViraMensagemNaTela() {
        when(conteudos.buscar(7L)).thenReturn(conteudo);
        when(gemini.gerar(conteudo, TipoGeracao.RESUMO, CHAVE)).thenReturn("Forças alteram o movimento.");
        when(historico.save(any(GeracaoIA.class))).thenThrow(new CannotCreateTransactionException("banco indisponível"));

        assertThatThrownBy(() -> service.gerar(7L, TipoGeracao.RESUMO, CHAVE))
                .isInstanceOf(GeracaoIAException.class)
                .hasMessageContaining("salvar a geração");
    }
}
