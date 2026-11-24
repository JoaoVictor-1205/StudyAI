package br.com.studyai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conteudos")
public class Conteudo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, length = 100)
    private String disciplina;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String texto;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @OneToMany(mappedBy = "conteudo")
    private List<GeracaoIA> geracoes = new ArrayList<>();

    protected Conteudo() {
    }

    public Conteudo(String titulo, String disciplina, String texto) {
        editar(titulo, disciplina, texto);
    }

    public void editar(String titulo, String disciplina, String texto) {
        this.titulo = titulo.strip();
        this.disciplina = disciplina.strip();
        this.texto = texto;
    }

    @PrePersist
    private void registrarCriacao() {
        dataCriacao = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDisciplina() {
        return disciplina;
    }

    public String getTexto() {
        return texto;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
}
