package br.com.studyai.model;

import br.com.studyai.enums.TipoGeracao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "geracoes_ia")
public class GeracaoIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conteudo_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Conteudo conteudo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoGeracao tipo;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String resposta;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    protected GeracaoIA() {
    }

    public GeracaoIA(Conteudo conteudo, TipoGeracao tipo, String resposta) {
        this.conteudo = conteudo;
        this.tipo = tipo;
        this.resposta = resposta;
    }

    @PrePersist
    private void registrarCriacao() {
        dataCriacao = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Conteudo getConteudo() {
        return conteudo;
    }

    public TipoGeracao getTipo() {
        return tipo;
    }

    public String getResposta() {
        return resposta;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
}
