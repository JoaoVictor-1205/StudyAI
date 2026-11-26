document.querySelectorAll("[data-confirm-delete]").forEach((form) => {
    form.addEventListener("submit", (event) => {
        const confirmed = window.confirm("Excluir este conteúdo e todas as respostas geradas para ele?");
        if (!confirmed) {
            event.preventDefault();
        }
    });
});

// Evita cadastrar o mesmo conteúdo duas vezes com um clique duplo. A trava se desfaz sozinha se a página não mudar.
document.querySelectorAll("form.editor").forEach((form) => {
    let sending = false;
    let timer;
    form.addEventListener("submit", (event) => {
        if (sending) {
            event.preventDefault();
            return;
        }
        sending = true;
        timer = window.setTimeout(() => {
            sending = false;
        }, 15000);
    });
    window.addEventListener("pageshow", (event) => {
        if (event.persisted) {
            window.clearTimeout(timer);
            sending = false;
        }
    });
});

// A chave da API fica somente no armazenamento do navegador: localStorage quando o usuário pede para lembrá-la,
// sessionStorage (apenas esta aba) quando não. Ela vai ao servidor só no cabeçalho de cada pedido de geração.
const API_KEY_ITEM = "studyai.geminiApiKey";
// Momento da última remoção. Faz "Remover chave" valer também para cópias da sessionStorage em outras abas.
const API_KEY_REMOVED_ITEM = "studyai.geminiApiKeyRemovedAt";
const API_KEY_HEADER = "X-Chave-Gemini";
const MAX_API_KEY_LENGTH = 500;
const BLOCKED_STORAGE_MESSAGE =
    "O navegador bloqueou o armazenamento deste site. Libere-o nas configurações do navegador para salvar a chave.";

function storageArea(name) {
    try {
        return window[name];
    } catch (error) {
        return null;
    }
}

function readItem(area, item) {
    try {
        return storageArea(area)?.getItem(item) ?? null;
    } catch (error) {
        return null;
    }
}

function removeItem(area, item) {
    try {
        storageArea(area)?.removeItem(item);
    } catch (error) {
        // Sem acesso a esta área, não há nada salvo nela.
    }
}

// A chave da integração anterior, com a OpenAI, não serve para o Gemini e não deve continuar guardada.
for (const area of ["localStorage", "sessionStorage"]) {
    removeItem(area, "studyai.openaiApiKey");
    removeItem(area, "studyai.openaiApiKeyRemovedAt");
}

function parseStoredKey(raw) {
    if (!raw) {
        return null;
    }
    try {
        const data = JSON.parse(raw);
        if (typeof data?.value === "string") {
            return { value: data.value, savedAt: Number(data.savedAt) || 0 };
        }
    } catch (error) {
        // Formato antigo: só o texto da chave.
    }
    return { value: raw, savedAt: 0 };
}

function removedAt() {
    return Number(readItem("localStorage", API_KEY_REMOVED_ITEM)) || 0;
}

function readSavedKey() {
    const lastRemoval = removedAt();
    for (const area of ["sessionStorage", "localStorage"]) {
        const stored = parseStoredKey(readItem(area, API_KEY_ITEM));
        if (stored && stored.savedAt > lastRemoval) {
            return { value: stored.value, remember: area === "localStorage" };
        }
        if (stored) {
            // A chave foi removida depois de salva nesta área (por exemplo, em outra aba).
            removeItem(area, API_KEY_ITEM);
        }
    }
    return null;
}

function saveKey(value, remember) {
    const data = JSON.stringify({ value, savedAt: Math.max(Date.now(), removedAt() + 1) });
    try {
        storageArea(remember ? "localStorage" : "sessionStorage").setItem(API_KEY_ITEM, data);
    } catch (error) {
        return false;
    }
    removeItem(remember ? "sessionStorage" : "localStorage", API_KEY_ITEM);
    return true;
}

function removeKey() {
    removeItem("localStorage", API_KEY_ITEM);
    removeItem("sessionStorage", API_KEY_ITEM);
    try {
        storageArea("localStorage").setItem(API_KEY_REMOVED_ITEM, String(Date.now()));
    } catch (error) {
        // Sem localStorage, não há como avisar as outras abas; esta aba já não tem a chave.
    }
    return !readSavedKey();
}

function maskKey(value) {
    return value.length > 12 ? `${value.slice(0, 4)}…${value.slice(-4)}` : "••••";
}

function validateKey(value) {
    if (!value) {
        return "Informe a chave da API.";
    }
    if (/\s/.test(value)) {
        return "A chave não pode ter espaços nem quebras de linha. Copie-a novamente do Google AI Studio.";
    }
    if (!/^[\x21-\x7E]+$/.test(value)) {
        return "A chave tem caracteres inválidos. Copie-a novamente do Google AI Studio.";
    }
    if (value.length > MAX_API_KEY_LENGTH) {
        return `A chave tem mais de ${MAX_API_KEY_LENGTH} caracteres. Confira se você copiou apenas a chave.`;
    }
    return "";
}

function onKeyChangedInAnotherTab(callback) {
    window.addEventListener("storage", (event) => {
        if (event.key === API_KEY_ITEM || event.key === API_KEY_REMOVED_ITEM || event.key === null) {
            callback();
        }
    });
}

const keyPage = document.querySelector("[data-key-page]");

if (keyPage) {
    const form = keyPage.querySelector("[data-key-form]");
    const fields = keyPage.querySelector("[data-key-fields]");
    const input = keyPage.querySelector("[data-key-input]");
    const error = keyPage.querySelector("[data-key-error]");
    const remember = keyPage.querySelector("[data-key-remember]");
    const removeButton = keyPage.querySelector("[data-key-remove]");
    const toggle = keyPage.querySelector("[data-key-toggle]");
    const toggleText = keyPage.querySelector("[data-key-toggle-text]");
    const current = keyPage.querySelector("[data-key-current]");
    const currentIcon = keyPage.querySelector("[data-key-current-icon]");
    const currentTitle = keyPage.querySelector("[data-key-current-title]");
    const currentDetail = keyPage.querySelector("[data-key-current-detail]");
    const status = keyPage.querySelector("[data-key-status]");
    const inputDescription = input.getAttribute("aria-describedby");
    const sharedComputerTip = "Antes de sair de um computador compartilhado, clique em Remover chave.";
    let errorTimer;

    const showError = (message) => {
        status.textContent = "";
        input.classList.add("is-invalid");
        input.setAttribute("aria-invalid", "true");
        input.setAttribute("aria-describedby", `${inputDescription} ${error.id}`);
        // O contêiner é role="alert": esvaziar e preencher de novo faz o leitor de tela anunciar até uma mensagem repetida.
        error.textContent = "";
        window.clearTimeout(errorTimer);
        errorTimer = window.setTimeout(() => {
            error.textContent = message;
        }, 50);
        input.focus();
    };

    const clearError = () => {
        window.clearTimeout(errorTimer);
        error.textContent = "";
        input.classList.remove("is-invalid");
        input.removeAttribute("aria-invalid");
        input.setAttribute("aria-describedby", inputDescription);
    };

    const setKeyVisible = (visible) => {
        input.type = visible ? "text" : "password";
        toggleText.textContent = visible ? "Ocultar" : "Mostrar";
        toggle.setAttribute("aria-label", visible ? "Ocultar chave" : "Mostrar chave");
        toggle.querySelector("i").className = visible ? "bi bi-eye-slash" : "bi bi-eye";
    };

    const renderSavedKey = () => {
        const saved = readSavedKey();
        current.hidden = false;
        removeButton.hidden = !saved;
        currentIcon.classList.toggle("icon-lime", Boolean(saved));
        currentIcon.classList.toggle("icon-peach", !saved);
        if (saved) {
            currentTitle.textContent = "Chave salva neste navegador";
            currentDetail.textContent = saved.remember
                ? `${maskKey(saved.value)} · fica salva até você removê-la.`
                : `${maskKey(saved.value)} · fica só nesta aba, mas o navegador pode restaurá-la se a aba for reaberta.`;
            remember.checked = saved.remember;
        } else {
            currentTitle.textContent = "Nenhuma chave salva";
            currentDetail.textContent = "Salve uma chave para usar a IA.";
        }
    };

    // Com uma chave já salva, a opção "Lembrar neste navegador" vale assim que muda.
    const applyRememberOption = () => {
        const saved = readSavedKey();
        if (!saved) {
            return false;
        }
        if (!saveKey(saved.value, remember.checked)) {
            remember.checked = saved.remember;
            showError(BLOCKED_STORAGE_MESSAGE);
            return true;
        }
        clearError();
        renderSavedKey();
        status.textContent = remember.checked
            ? "Pronto: a chave fica salva neste navegador até você removê-la."
            : `Pronto: a chave fica só nesta aba. ${sharedComputerTip}`;
        return true;
    };

    renderSavedKey();

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        const value = input.value.trim();

        if (!value && applyRememberOption()) {
            return;
        }

        const message = validateKey(value);
        if (message) {
            showError(message);
            return;
        }
        if (!saveKey(value, remember.checked)) {
            showError(BLOCKED_STORAGE_MESSAGE);
            return;
        }

        clearError();
        input.value = "";
        setKeyVisible(false);
        renderSavedKey();
        status.textContent = remember.checked
            ? "Chave salva neste navegador."
            : `Chave salva só nesta aba. ${sharedComputerTip}`;
        if (!value.startsWith("AIza")) {
            status.textContent += " Confira se ela foi copiada inteira: chaves do Gemini costumam começar com “AIza”.";
        }
    });

    remember.addEventListener("change", applyRememberOption);

    input.addEventListener("input", clearError);

    toggle.addEventListener("click", () => {
        setKeyVisible(input.type === "password");
    });

    removeButton.addEventListener("click", () => {
        const confirmed = window.confirm(
            "Remover a chave salva neste navegador? Para usar a IA de novo, será preciso colá-la outra vez.");
        if (!confirmed) {
            return;
        }
        const removed = removeKey();
        renderSavedKey();
        status.textContent = removed
            ? "Chave removida deste navegador."
            : "Não foi possível remover a chave. Apague os dados deste site nas configurações do navegador.";
        input.focus();
    });

    onKeyChangedInAnotherTab(renderSavedKey);

    window.addEventListener("pageshow", (event) => {
        if (event.persisted) {
            renderSavedKey();
        }
    });

    // Os campos só são liberados depois que o envio pelo JavaScript está pronto.
    fields.disabled = false;
}

const aiPanel = document.querySelector("[data-ai-panel]");

if (aiPanel) {
    const forms = aiPanel.querySelectorAll("[data-ai-action]");
    const buttons = aiPanel.querySelectorAll("button");
    const status = aiPanel.querySelector("[data-ai-status]");
    const keySummary = aiPanel.querySelector("[data-key-summary]");
    const keySummaryText = aiPanel.querySelector("[data-key-summary-text]");
    const missingKey = aiPanel.querySelector("[data-key-missing]");
    let loading = false;

    const resetPanel = () => {
        loading = false;
        buttons.forEach((button) => {
            button.disabled = false;
            button.classList.remove("is-loading");
        });
        aiPanel.querySelectorAll("[data-ai-spinner]").forEach((spinner) => {
            spinner.hidden = true;
        });
        status.textContent = "";
    };

    const renderKeyStatus = () => {
        const saved = readSavedKey();
        keySummary.hidden = !saved;
        missingKey.hidden = Boolean(saved);
        if (saved) {
            keySummaryText.textContent = `Usando a chave salva neste navegador (${maskKey(saved.value)}).`;
        }
        return saved;
    };

    renderKeyStatus();

    forms.forEach((form) => {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (loading) {
                return;
            }
            const saved = renderKeyStatus();
            if (!saved) {
                missingKey.focus();
                return;
            }

            loading = true;
            const button = form.querySelector("button");
            button.classList.add("is-loading");
            form.querySelector("[data-ai-spinner]").hidden = false;
            buttons.forEach((each) => {
                each.disabled = true;
            });
            status.textContent = "Gerando resposta. Aguarde um momento.";

            // A chave vai num cabeçalho de um pedido em segundo plano, e não num formulário de navegação: assim ela
            // não fica no histórico da aba. O painel só destrava quando o servidor termina, para não gerar em dobro.
            let failure;
            try {
                const reply = await fetch(form.action, {
                    method: "POST",
                    headers: { Accept: "application/json", [API_KEY_HEADER]: saved.value },
                    body: new URLSearchParams(new FormData(form)),
                    credentials: "same-origin",
                    cache: "no-store",
                });
                if (reply.ok) {
                    const { destino } = await reply.json();
                    window.location.assign(destino);
                    return;
                }
                failure = `Não foi possível gerar a resposta (erro ${reply.status}). Recarregue a página e tente novamente.`;
            } catch (error) {
                failure = "Não foi possível falar com o StudyAI. Confira se a aplicação continua em execução e "
                    + "recarregue a página para ver se a resposta foi salva antes de tentar de novo.";
            }
            resetPanel();
            status.textContent = failure;
            button.focus();
        });
    });

    window.addEventListener("pageshow", (event) => {
        if (event.persisted) {
            resetPanel();
            renderKeyStatus();
        }
    });

    onKeyChangedInAnotherTab(renderKeyStatus);
}

// Ao carregar, leva o foco ao que precisa de atenção: um erro, o primeiro campo inválido ou a resposta gerada.
const attention = document.querySelector("[data-focus-on-load]") ?? document.querySelector("form.editor .is-invalid");
const response = document.querySelector("[data-jump-to-response]");

if (attention) {
    attention.focus();
} else if (response && new URLSearchParams(window.location.search).has("geracao")) {
    response.focus();
}
