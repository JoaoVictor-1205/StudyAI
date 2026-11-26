package br.com.studyai;

import br.com.studyai.config.SegurancaWebFilter;
import br.com.studyai.service.GeminiChatClientFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Carrega o filtro e a fábrica como o Spring faz na aplicação, com o application.properties real, sem banco de dados.
class ConfiguracaoDaAplicacaoTest {

    // Como o SpringApplication faz, usa o conversor do Boot (ex.: "45s" para Duration) e lê o application.properties.
    private final WebApplicationContextRunner contexto = new WebApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withBean("conversionService", ConversionService.class, ApplicationConversionService::getSharedInstance)
            .withUserConfiguration(PacoteDeConfiguracao.class, GeminiChatClientFactory.class);

    @Test
    void registraOFiltroEAFabricaComAsPropriedadesDoProjeto() {
        contexto.run(aplicacao -> {
            assertThat(aplicacao).hasNotFailed();
            assertThat(aplicacao).hasSingleBean(SegurancaWebFilter.class);
            assertThat(aplicacao).hasSingleBean(GeminiChatClientFactory.class);
            try (GeminiChatClientFactory.SessaoGemini sessao =
                    aplicacao.getBean(GeminiChatClientFactory.class).abrir("AIza-chave-de-teste")) {
                assertThat(sessao.chatClient()).isNotNull();
            }

            MockMvc mvc = MockMvcBuilders.standaloneSetup(new PaginaSimples())
                    .addFilters(aplicacao.getBean(SegurancaWebFilter.class))
                    .build();
            mvc.perform(get("http://localhost:8080/ok")).andExpect(status().isOk());
            mvc.perform(get("http://127.0.0.1:8080/ok")).andExpect(status().isOk());
            mvc.perform(get("http://studyai.local:8080/ok")).andExpect(status().isForbidden());
        });
    }

    @Configuration
    @ComponentScan(basePackageClasses = SegurancaWebFilter.class)
    static class PacoteDeConfiguracao {
    }

    @RestController
    static class PaginaSimples {

        @GetMapping("/ok")
        String ok() {
            return "ok";
        }
    }
}
