package br.com.studyai.service;

import br.com.studyai.enums.TipoGeracao;
import br.com.studyai.model.Conteudo;
import br.com.studyai.model.GeracaoIA;
import br.com.studyai.repository.GeracaoIARepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GeracaoIAServiceTest {

    private final ConteudoService conteudos = mock(ConteudoService.class);
    private final GeracaoIARepository historico = mock(GeracaoIARepository.class);
    private final OpenAiService openAi = mock(OpenAiService.class);
    private final GeracaoIAService service = new GeracaoIAService(conteudos, historico, openAi);
    private final Conteudo conteudo = new Conteudo("Forças", "Física", "A força pode alterar o movimento.");

    @Test
    void salvaRespostaVinculadaAoConteudoSelecionado() {
        when(conteudos.buscar(7L)).thenReturn(conteudo);
        when(openAi.gerar(conteudo, TipoGeracao.RESUMO)).thenReturn("Forças alteram o movimento.");
        when(historico.save(any(GeracaoIA.class))).thenAnswer(chamada -> chamada.getArgument(0));

        GeracaoIA geracao = service.gerar(7L, TipoGeracao.RESUMO);

        assertThat(geracao.getConteudo()).isSameAs(conteudo);
        assertThat(geracao.getTipo()).isEqualTo(TipoGeracao.RESUMO);
        assertThat(geracao.getResposta()).isEqualTo("Forças alteram o movimento.");
    }

    @Test
    void falhaDeGeracaoNaoCriaRegistroNoHistorico() {
        when(conteudos.buscar(7L)).thenReturn(conteudo);
        when(openAi.gerar(conteudo, TipoGeracao.EXPLICACAO))
                .thenThrow(new GeracaoIAException("A OpenAI retornou uma resposta vazia."));

        assertThatThrownBy(() -> service.gerar(7L, TipoGeracao.EXPLICACAO))
                .isInstanceOf(GeracaoIAException.class);

        verifyNoInteractions(historico);
    }
}
