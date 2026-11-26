package br.com.studyai.config;

import br.com.studyai.controller.ConteudoController;
import br.com.studyai.service.ConteudoService;
import br.com.studyai.service.GeracaoIAService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SegurancaWebFilterTest {

    private final ConteudoService conteudos = mock(ConteudoService.class);
    private final GeracaoIAService geracoes = mock(GeracaoIAService.class);
    private MockMvc mvc;

    @BeforeEach
    void configurarMvc() {
        mvc = MockMvcBuilders.standaloneSetup(new ConteudoController(conteudos, geracoes))
                .addFilters(new SegurancaWebFilter(List.of("localhost", "127.0.0.1")))
                .build();
    }

    @Test
    void enviaCabecalhosDeSeguranca() throws Exception {
        mvc.perform(get("http://127.0.0.1:8080/conteudos"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", SegurancaWebFilter.POLITICA_DE_CONTEUDO))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "same-origin"));
    }

    @Test
    void recusaEnderecoForaDaListaParaImpedirDnsRebinding() throws Exception {
        mvc.perform(get("http://atacante.example:8080/conteudos"))
                .andExpect(status().isForbidden());
        mvc.perform(get("http://localhost:8080/conteudos").header("Host", "atacante.example:8080"))
                .andExpect(status().isForbidden());
        mvc.perform(get("http://localhost:8080/conteudos")
                        .header("Host", "atacante.example:8080")
                        .header("X-Forwarded-Host", "localhost:8080"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(conteudos);
    }

    @Test
    void aceitaOsEnderecosPermitidosEmQualquerPorta() throws Exception {
        mvc.perform(get("http://localhost:8080/conteudos").header("Host", "LOCALHOST:9090"))
                .andExpect(status().isOk());
        mvc.perform(get("http://localhost:8080/conteudos").header("Host", "127.0.0.1"))
                .andExpect(status().isOk());
    }

    @Test
    void recusaFormularioEnviadoPorOutroSite() throws Exception {
        mvc.perform(post("http://localhost:8080/conteudos/7/excluir").header("Sec-Fetch-Site", "cross-site"))
                .andExpect(status().isForbidden());
        mvc.perform(post("http://localhost:8080/conteudos/7/excluir").header("Sec-Fetch-Site", "same-site"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(conteudos);
    }

    @Test
    void recusaOrigemDiferenteEmNavegadoresSemSecFetchSite() throws Exception {
        mvc.perform(post("http://localhost:8080/conteudos/7/excluir").header("Origin", "http://atacante.example"))
                .andExpect(status().isForbidden());
        mvc.perform(post("http://localhost:8080/conteudos/7/excluir").header("Origin", "null"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(conteudos);
    }

    @Test
    void aceitaFormularioDaPropriaAplicacao() throws Exception {
        mvc.perform(post("http://localhost:8080/conteudos/7/excluir")
                        .header("Sec-Fetch-Site", "same-origin")
                        .header("Origin", "http://localhost:8080"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conteudos"));
        mvc.perform(post("http://127.0.0.1:8080/conteudos/8/excluir")
                        .header("Host", "127.0.0.1:8080")
                        .header("Origin", "http://127.0.0.1:8080"))
                .andExpect(status().is3xxRedirection());

        verify(conteudos).excluir(7L);
        verify(conteudos).excluir(8L);
    }

    @Test
    void aceitaClienteForaDoNavegadorSemCabecalhosDeOrigem() throws Exception {
        mvc.perform(post("http://localhost:8080/conteudos/7/excluir"))
                .andExpect(status().is3xxRedirection());

        verify(conteudos).excluir(7L);
    }
}
