package br.com.studyai.controller;

import br.com.studyai.enums.TipoGeracao;
import br.com.studyai.model.Conteudo;
import br.com.studyai.model.GeracaoIA;
import br.com.studyai.service.ConteudoService;
import br.com.studyai.service.GeracaoIAException;
import br.com.studyai.service.GeracaoIAService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

@Controller
public class ConteudoController {

    static final String CABECALHO_CHAVE = "X-Chave-Gemini";

    private final ConteudoService conteudoService;
    private final GeracaoIAService geracaoIAService;

    public ConteudoController(ConteudoService conteudoService, GeracaoIAService geracaoIAService) {
        this.conteudoService = conteudoService;
        this.geracaoIAService = geracaoIAService;
    }

    @InitBinder("conteudo")
    void configurarFormulario(WebDataBinder binder) {
        binder.setAllowedFields("titulo", "disciplina", "texto");
    }

    @GetMapping("/")
    public String inicio() {
        return "index";
    }

    @GetMapping("/conteudos")
    public String listar(Model model) {
        model.addAttribute("conteudos", conteudoService.listar());
        return "conteudos/lista";
    }

    @GetMapping("/conteudos/novo")
    public String novo(Model model) {
        model.addAttribute("conteudo", new ConteudoFormulario());
        return "conteudos/formulario";
    }

    @PostMapping("/conteudos")
    public String cadastrar(@Valid @ModelAttribute("conteudo") ConteudoFormulario formulario,
            BindingResult validacao, RedirectAttributes redirect) {
        if (validacao.hasErrors()) {
            return "conteudos/formulario";
        }
        Conteudo conteudo = conteudoService.cadastrar(formulario.getTitulo(), formulario.getDisciplina(),
                formulario.getTexto());
        redirect.addFlashAttribute("mensagemSucesso", "Conteúdo cadastrado com sucesso.");
        return "redirect:/conteudos/" + conteudo.getId();
    }

    @GetMapping("/conteudos/{id}")
    public String detalhes(@PathVariable Long id, @RequestParam(name = "geracao", required = false) Long geracaoId,
            Model model) {
        Conteudo conteudo = conteudoService.buscar(id);
        List<GeracaoIA> geracoes = geracaoIAService.listar(id);
        model.addAttribute("conteudo", conteudo);
        model.addAttribute("geracoes", geracoes);
        model.addAttribute("geracaoSelecionada", selecionarGeracao(geracoes, geracaoId));
        return "conteudos/detalhes";
    }

    @GetMapping("/conteudos/{id}/editar")
    public String formularioEdicao(@PathVariable Long id, Model model) {
        model.addAttribute("conteudo", new ConteudoFormulario(conteudoService.buscar(id)));
        return "conteudos/formulario";
    }

    @PostMapping("/conteudos/{id}")
    public String editar(@PathVariable Long id, @Valid @ModelAttribute("conteudo") ConteudoFormulario formulario,
            BindingResult validacao, RedirectAttributes redirect) {
        if (validacao.hasErrors()) {
            conteudoService.buscar(id);
            formulario.definirId(id);
            return "conteudos/formulario";
        }
        conteudoService.editar(id, formulario.getTitulo(), formulario.getDisciplina(), formulario.getTexto());
        redirect.addFlashAttribute("mensagemSucesso", "Conteúdo atualizado com sucesso.");
        return "redirect:/conteudos/" + id;
    }

    @PostMapping("/conteudos/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        conteudoService.excluir(id);
        redirect.addFlashAttribute("mensagemSucesso", "Conteúdo excluído com sucesso.");
        return "redirect:/conteudos";
    }

    /**
     * Geração pedida pelo JavaScript da página, que lê a chave do armazenamento do navegador e a envia neste
     * cabeçalho. A chave não vai no corpo de um formulário: assim ela nunca fica no histórico da aba, que o navegador
     * grava em disco para permitir recarregar a página. A resposta indica a página a abrir em seguida, e a mensagem
     * de resultado segue como atributo flash, como num redirect.
     */
    @PostMapping(value = "/conteudos/{id}/geracoes", headers = CABECALHO_CHAVE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, String> gerar(@PathVariable Long id, @RequestParam TipoGeracao tipo,
            @RequestHeader(CABECALHO_CHAVE) String chaveApi, HttpServletRequest request,
            HttpServletResponse response) {
        FlashMap mensagens = RequestContextUtils.getOutputFlashMap(request);
        String destino = request.getContextPath() + "/conteudos/" + id;
        try {
            GeracaoIA geracao = geracaoIAService.gerar(id, tipo, chaveApi);
            destino += "?geracao=" + geracao.getId();
            mensagens.put("mensagemSucesso", "Resposta gerada e salva no histórico.");
        } catch (GeracaoIAException exception) {
            mensagens.put("mensagemErro", exception.getMessage());
        }
        RequestContextUtils.saveOutputFlashMap(destino, request, response);
        return Map.of("destino", destino);
    }

    // Envio do formulário sem o JavaScript: sem ele não há como ler a chave salva no navegador.
    @PostMapping("/conteudos/{id}/geracoes")
    public String gerarSemJavaScript(@PathVariable Long id, RedirectAttributes redirect) {
        redirect.addFlashAttribute("mensagemErro",
                "Para gerar respostas, ative o JavaScript: é ele que envia a chave da API salva neste navegador.");
        return "redirect:/conteudos/" + id;
    }

    private GeracaoIA selecionarGeracao(List<GeracaoIA> geracoes, Long geracaoId) {
        if (geracaoId == null) {
            return geracoes.isEmpty() ? null : geracoes.getFirst();
        }
        return geracoes.stream()
                .filter(geracao -> geracaoId.equals(geracao.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Geração não encontrada para este conteúdo."));
    }
}
