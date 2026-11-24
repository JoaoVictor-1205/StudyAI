package br.com.studyai.service;

import br.com.studyai.enums.TipoGeracao;
import br.com.studyai.model.Conteudo;
import br.com.studyai.model.GeracaoIA;
import br.com.studyai.repository.GeracaoIARepository;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GeracaoIAService {

    private final ConteudoService conteudoService;
    private final GeracaoIARepository geracaoIARepository;
    private final OpenAiService openAiService;

    public GeracaoIAService(
            ConteudoService conteudoService, GeracaoIARepository geracaoIARepository, OpenAiService openAiService) {
        this.conteudoService = conteudoService;
        this.geracaoIARepository = geracaoIARepository;
        this.openAiService = openAiService;
    }

    @Transactional(readOnly = true)
    public List<GeracaoIA> listar(Long conteudoId) {
        return geracaoIARepository.findAllByConteudoIdOrderByDataCriacaoDescIdDesc(conteudoId);
    }

    public GeracaoIA gerar(Long conteudoId, TipoGeracao tipo) {
        Conteudo conteudo = conteudoService.buscar(conteudoId);
        String resposta = openAiService.gerar(conteudo, tipo);

        try {
            return geracaoIARepository.save(new GeracaoIA(conteudo, tipo, resposta));
        } catch (DataAccessException exception) {
            throw new GeracaoIAException("Não foi possível salvar a geração no histórico. Tente novamente.");
        }
    }
}
