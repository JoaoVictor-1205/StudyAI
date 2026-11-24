package br.com.studyai.repository;

import br.com.studyai.model.GeracaoIA;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeracaoIARepository extends JpaRepository<GeracaoIA, Long> {

    List<GeracaoIA> findAllByConteudoIdOrderByDataCriacaoDescIdDesc(Long conteudoId);
}
