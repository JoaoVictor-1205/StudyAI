document.querySelectorAll("[data-confirm-delete]").forEach((form) => {
    form.addEventListener("submit", (event) => {
        const confirmed = window.confirm("Excluir este conteúdo e todas as respostas geradas para ele?");
        if (!confirmed) {
            event.preventDefault();
        }
    });
});

const aiPanel = document.querySelector("[data-ai-panel]");

if (aiPanel) {
    const forms = aiPanel.querySelectorAll("[data-ai-action]");
    const buttons = aiPanel.querySelectorAll("button");
    const status = aiPanel.querySelector("[data-ai-status]");
    let loading = false;

    forms.forEach((form) => {
        form.addEventListener("submit", (event) => {
            if (loading) {
                event.preventDefault();
                return;
            }

            loading = true;
            form.querySelector("button").classList.add("is-loading");
            form.querySelector("[data-ai-spinner]").hidden = false;
            buttons.forEach((button) => {
                button.disabled = true;
            });
            status.textContent = "Gerando resposta. Aguarde um momento.";
        });
    });

    window.addEventListener("pageshow", (event) => {
        if (event.persisted) {
            loading = false;
            buttons.forEach((button) => {
                button.disabled = false;
                button.classList.remove("is-loading");
            });
            aiPanel.querySelectorAll("[data-ai-spinner]").forEach((spinner) => {
                spinner.hidden = true;
            });
            status.textContent = "";
        }
    });
}

const response = document.querySelector("[data-jump-to-response]");

if (response && new URLSearchParams(window.location.search).has("geracao")) {
    response.focus();
}
