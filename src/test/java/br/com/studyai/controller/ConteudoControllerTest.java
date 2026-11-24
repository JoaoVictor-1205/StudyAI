package br.com.studyai.controller;

import br.com.studyai.enums.TipoGeracao;
import br.com.studyai.model.Conteudo;
import br.com.studyai.model.GeracaoIA;
import br.com.studyai.service.ConteudoService;
import br.com.studyai.service.GeracaoIAException;
import br.com.studyai.service.GeracaoIAService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ConteudoControllerTest {

    private final ConteudoService conteudos = mock(ConteudoService.class);
    private final GeracaoIAService geracoes = mock(GeracaoIAService.class);
    private MockMvc mvc;

    @BeforeEach
    void configurarMvc() {
        mvc = MockMvcBuilders.standaloneSetup(new ConteudoController(conteudos, geracoes)).build();
    }

    @Test
    void listaConteudosCadastrados() throws Exception {
        Conteudo conteudo = mock(Conteudo.class);
        when(conteudos.listar()).thenReturn(List.of(conteudo));

        mvc.perform(get("/conteudos"))
                .andExpect(status().isOk())
                .andExpect(view().name("conteudos/lista"))
                .andExpect(model().attribute("conteudos", List.of(conteudo)));
    }

    @Test
    void rejeitaCamposEmBrancoSemCadastrar() throws Exception {
        mvc.perform(post("/conteudos")
                        .param("titulo", " ")
                        .param("disciplina", "Física")
                        .param("texto", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("conteudos/formulario"))
                .andExpect(model().attributeHasFieldErrors("conteudo", "titulo", "texto"));

        verifyNoInteractions(conteudos);
    }

    @Test
    void cadastraConteudoValido() throws Exception {
        Conteudo cadastrado = mock(Conteudo.class);
        when(cadastrado.getId()).thenReturn(7L);
        when(conteudos.cadastrar("Forças", "Física", "A força pode alterar o movimento."))
                .thenReturn(cadastrado);

        mvc.perform(post("/conteudos")
                        .param("titulo", "Forças")
                        .param("disciplina", "Física")
                        .param("texto", "A força pode alterar o movimento."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conteudos/7"))
                .andExpect(flash().attributeExists("mensagemSucesso"));

        verify(conteudos).cadastrar("Forças", "Física", "A força pode alterar o movimento.");
    }

    @Test
    void editaRegistroIdentificadoNaRota() throws Exception {
        mvc.perform(post("/conteudos/7")
                        .param("id", "99")
                        .param("titulo", "Força resultante")
                        .param("disciplina", "Física")
                        .param("texto", "É a soma vetorial das forças aplicadas."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conteudos/7"))
                .andExpect(flash().attributeExists("mensagemSucesso"));

        verify(conteudos).editar(7L, "Força resultante", "Física", "É a soma vetorial das forças aplicadas.");
    }

    @Test
    void apresentaUmaGeracaoDoHistoricoSemChamarAApi() throws Exception {
        Conteudo conteudo = mock(Conteudo.class);
        GeracaoIA recente = mock(GeracaoIA.class);
        GeracaoIA anterior = mock(GeracaoIA.class);
        when(recente.getId()).thenReturn(23L);
        when(anterior.getId()).thenReturn(22L);
        when(conteudos.buscar(7L)).thenReturn(conteudo);
        when(geracoes.listar(7L)).thenReturn(List.of(recente, anterior));

        mvc.perform(get("/conteudos/7").param("geracao", "22"))
                .andExpect(status().isOk())
                .andExpect(view().name("conteudos/detalhes"))
                .andExpect(model().attribute("conteudo", conteudo))
                .andExpect(model().attribute("geracoes", List.of(recente, anterior)))
                .andExpect(model().attribute("geracaoSelecionada", anterior));

        verify(geracoes).listar(7L);
        verifyNoMoreInteractions(geracoes);
    }

    @Test
    void excluiConteudoERetornaParaListagem() throws Exception {
        mvc.perform(post("/conteudos/7/excluir"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conteudos"))
                .andExpect(flash().attributeExists("mensagemSucesso"));

        verify(conteudos).excluir(7L);
    }

    @Test
    void redirecionaParaARespostaGerada() throws Exception {
        GeracaoIA geracao = mock(GeracaoIA.class);
        when(geracao.getId()).thenReturn(21L);
        when(geracoes.gerar(7L, TipoGeracao.RESUMO)).thenReturn(geracao);

        mvc.perform(post("/conteudos/7/geracoes").param("tipo", "RESUMO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conteudos/7?geracao=21"))
                .andExpect(flash().attributeExists("mensagemSucesso"));
    }

    @Test
    void trataFalhaDaIaNaTelaDoConteudo() throws Exception {
        when(geracoes.gerar(7L, TipoGeracao.QUESTOES))
                .thenThrow(new GeracaoIAException("Configure a chave da API."));

        mvc.perform(post("/conteudos/7/geracoes").param("tipo", "QUESTOES"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conteudos/7"))
                .andExpect(flash().attribute("mensagemErro", "Configure a chave da API."));
    }
}
