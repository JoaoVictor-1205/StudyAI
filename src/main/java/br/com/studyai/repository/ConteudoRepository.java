package br.com.studyai.repository;

import br.com.studyai.model.Conteudo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConteudoRepository extends JpaRepository<Conteudo, Long> {

    List<Conteudo> findAllByOrderByDataCriacaoDescIdDesc();
}
