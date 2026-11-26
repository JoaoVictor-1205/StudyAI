package br.com.studyai.controller;

import br.com.studyai.enums.TipoGeracao;
import br.com.studyai.model.Conteudo;
import br.com.studyai.model.GeracaoIA;
import br.com.studyai.service.ConteudoService;
import br.com.studyai.service.GeracaoIAException;
import br.com.studyai.service.GeracaoIAService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ConteudoControllerTest {

    private static final String CHAVE = "AIza-chave-de-teste";

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
    void contaQuebrasDeLinhaComoONavegadorNoLimiteDoTexto() throws Exception {
        String enviado = "a".repeat(199_998) + "\r\nb";
        String normalizado = "a".repeat(199_998) + "\nb";
        Conteudo cadastrado = mock(Conteudo.class);
        when(cadastrado.getId()).thenReturn(7L);
        when(conteudos.cadastrar("Forças", "Física", normalizado)).thenReturn(cadastrado);

        mvc.perform(post("/conteudos")
                        .param("titulo", "Forças")
                        .param("disciplina", "Física")
                        .param("texto", enviado))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conteudos/7"));

        verify(conteudos).cadastrar("Forças", "Física", normalizado);
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
    void geracaoPeloNavegadorIndicaAPaginaDaRespostaSemExporAChave() throws Exception {
        GeracaoIA geracao = mock(GeracaoIA.class);
        when(geracao.getId()).thenReturn(21L);
        when(geracoes.gerar(7L, TipoGeracao.RESUMO, CHAVE)).thenReturn(geracao);

        MvcResult resultado = mvc.perform(post("/conteudos/7/geracoes")
                        .param("tipo", "RESUMO")
                        .header(ConteudoController.CABECALHO_CHAVE, CHAVE)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destino").value("/conteudos/7?geracao=21"))
                .andExpect(flash().attribute("mensagemSucesso", "Resposta gerada e salva no histórico."))
                .andReturn();

        verify(geracoes).gerar(7L, TipoGeracao.RESUMO, CHAVE);
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain(CHAVE);
        assertThat(resultado.getFlashMap().values()).allSatisfy(valor -> assertThat(valor).asString().doesNotContain(CHAVE));
        assertThat(resultado.getResponse().getHeaderNames()).allSatisfy(
                nome -> assertThat(resultado.getResponse().getHeaders(nome)).noneMatch(valor -> valor.contains(CHAVE)));
    }

    @Test
    void falhaDaIaVoltaParaOConteudoComAMensagem() throws Exception {
        when(geracoes.gerar(7L, TipoGeracao.QUESTOES, CHAVE))
                .thenThrow(new GeracaoIAException("O Gemini recusou a chave da API."));

        mvc.perform(post("/conteudos/7/geracoes")
                        .param("tipo", "QUESTOES")
                        .header(ConteudoController.CABECALHO_CHAVE, CHAVE)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destino").value("/conteudos/7"))
                .andExpect(flash().attribute("mensagemErro", "O Gemini recusou a chave da API."));
    }

    @Test
    void formularioSemJavaScriptNaoGeraEIgnoraChaveNoCorpo() throws Exception {
        mvc.perform(post("/conteudos/7/geracoes")
                        .param("tipo", "RESUMO")
                        .param("chaveApi", CHAVE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conteudos/7"))
                .andExpect(flash().attribute("mensagemErro",
                        "Para gerar respostas, ative o JavaScript: é ele que envia a chave da API salva neste navegador."));

        verifyNoInteractions(geracoes);
    }

    // O JavaScript envia a chave num cabeçalho com nome fixo; se os dois nomes divergirem, nenhuma geração funciona.
    @Test
    void javaScriptEnviaAChaveNoCabecalhoQueOControladorLe() throws Exception {
        String script = new ClassPathResource("static/js/studyai.js").getContentAsString(StandardCharsets.UTF_8);

        assertThat(script).contains("const API_KEY_HEADER = \"" + ConteudoController.CABECALHO_CHAVE + "\";");
    }
}
