package br.com.studyai.controller;

import br.com.studyai.model.Conteudo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ConteudoFormulario {

    private Long id;

    @NotBlank(message = "Informe o título do conteúdo.")
    @Size(max = 160, message = "O título deve ter até 160 caracteres.")
    private String titulo;

    @NotBlank(message = "Informe a disciplina.")
    @Size(max = 100, message = "A disciplina deve ter até 100 caracteres.")
    private String disciplina;

    @NotBlank(message = "Informe o texto do conteúdo.")
    @Size(max = 200_000, message = "O texto deve ter até 200.000 caracteres.")
    private String texto;

    public ConteudoFormulario() {
    }

    ConteudoFormulario(Conteudo conteudo) {
        id = conteudo.getId();
        titulo = conteudo.getTitulo();
        disciplina = conteudo.getDisciplina();
        texto = conteudo.getTexto();
    }

    void definirId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDisciplina() {
        return disciplina;
    }

    public void setDisciplina(String disciplina) {
        this.disciplina = disciplina;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }
}
