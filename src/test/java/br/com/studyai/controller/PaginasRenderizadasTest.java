package br.com.studyai.controller;

import br.com.studyai.model.Conteudo;
import br.com.studyai.service.ConteudoService;
import br.com.studyai.service.GeracaoIAService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// Renderiza os templates reais com o Thymeleaf para validar a marcação usada pelo JavaScript da chave.
class PaginasRenderizadasTest {

    private final ConteudoService conteudos = mock(ConteudoService.class);
    private final GeracaoIAService geracoes = mock(GeracaoIAService.class);
    private MockMvc mvc;

    @BeforeEach
    void configurarMvc() {
        ClassLoaderTemplateResolver templates = new ClassLoaderTemplateResolver();
        templates.setPrefix("templates/");
        templates.setSuffix(".html");
        templates.setTemplateMode(TemplateMode.HTML);
        templates.setCharacterEncoding("UTF-8");
        SpringTemplateEngine motor = new SpringTemplateEngine();
        motor.setTemplateResolver(templates);
        ThymeleafViewResolver views = new ThymeleafViewResolver();
        views.setTemplateEngine(motor);
        views.setCharacterEncoding("UTF-8");

        mvc = MockMvcBuilders.standaloneSetup(new ChaveApiController(), new ConteudoController(conteudos, geracoes))
                .setViewResolvers(views)
                .build();
    }

    @Test
    void telaDaChaveNaoTemCampoQueEnvieAChaveAoServidor() throws Exception {
        String html = mvc.perform(get("/chave-api"))
                .andExpect(status().isOk())
                .andExpect(view().name("chave-api"))
                .andReturn().getResponse().getContentAsString();

        String principal = trecho(html, "<main", "</main>");
        assertThat(principal).contains("id=\"chave-api\"", "data-key-form", "data-key-status");
        assertThat(principal).doesNotContain("name=");
        assertThat(principal).contains("<fieldset disabled data-key-fields>");
        assertThat(html).containsPattern("<a href=\"/chave-api\"[^>]*aria-current=\"page\"");
    }

    @Test
    void detalhesOrientamASalvarAChaveSemGuardarAChaveNaPagina() throws Exception {
        Conteudo conteudo = mock(Conteudo.class);
        when(conteudo.getId()).thenReturn(7L);
        when(conteudo.getTitulo()).thenReturn("Forças");
        when(conteudo.getDisciplina()).thenReturn("Física");
        when(conteudo.getTexto()).thenReturn("A força pode alterar o movimento.");
        when(conteudo.getDataCriacao()).thenReturn(LocalDateTime.of(2026, 11, 25, 10, 30));
        when(conteudos.buscar(7L)).thenReturn(conteudo);
        when(geracoes.listar(7L)).thenReturn(List.of());

        String html = mvc.perform(get("/conteudos/7"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("data-key-missing", "data-key-summary", "href=\"/chave-api\"");
        assertThat(html).doesNotContain("chaveApi");
        assertThat(html).containsPattern("<a href=\"/conteudos\"[^>]*aria-current=\"true\"");
    }

    private static String trecho(String texto, String inicio, String fim) {
        Matcher matcher = Pattern.compile(Pattern.quote(inicio) + ".*?" + Pattern.quote(fim), Pattern.DOTALL)
                .matcher(texto);
        assertThat(matcher.find()).as("trecho entre %s e %s", inicio, fim).isTrue();
        return matcher.group();
    }
}
