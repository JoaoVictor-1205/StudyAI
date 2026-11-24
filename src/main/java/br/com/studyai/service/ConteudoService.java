package br.com.studyai.service;

import br.com.studyai.model.Conteudo;
import br.com.studyai.repository.ConteudoRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConteudoService {

    private final ConteudoRepository conteudoRepository;

    public ConteudoService(ConteudoRepository conteudoRepository) {
        this.conteudoRepository = conteudoRepository;
    }

    @Transactional(readOnly = true)
    public List<Conteudo> listar() {
        return conteudoRepository.findAllByOrderByDataCriacaoDescIdDesc();
    }

    @Transactional(readOnly = true)
    public Conteudo buscar(Long id) {
        return conteudoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conteúdo de estudo não encontrado."));
    }

    public Conteudo cadastrar(String titulo, String disciplina, String texto) {
        return conteudoRepository.save(new Conteudo(titulo, disciplina, texto));
    }

    @Transactional
    public void editar(Long id, String titulo, String disciplina, String texto) {
        buscar(id).editar(titulo, disciplina, texto);
    }

    @Transactional
    public void excluir(Long id) {
        conteudoRepository.delete(buscar(id));
    }
}
