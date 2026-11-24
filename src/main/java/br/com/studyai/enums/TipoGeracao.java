package br.com.studyai.enums;

public enum TipoGeracao {
    RESUMO("Resumo"),
    QUESTOES("Questões"),
    EXPLICACAO("Explicação");

    private final String rotulo;

    TipoGeracao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
