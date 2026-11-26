package br.com.studyai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protege a aplicação local contra páginas de outros sites abertas no mesmo navegador.
 *
 * <ul>
 * <li>Recusa endereços fora da lista permitida no cabeçalho Host, o que impede ataques de DNS rebinding.</li>
 * <li>Recusa formulários enviados por outra origem (CSRF), com base nos cabeçalhos Sec-Fetch-Site e Origin.</li>
 * <li>Envia cabeçalhos que restringem scripts à própria aplicação e impedem que ela seja aberta dentro de outro
 * site. Isso também protege a chave da API guardada no navegador.</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SegurancaWebFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SegurancaWebFilter.class);

    static final String POLITICA_DE_CONTEUDO = "default-src 'self'; img-src 'self' data:; object-src 'none'; "
            + "base-uri 'none'; form-action 'self'; frame-ancestors 'none'";

    private final Set<String> hostsPermitidos;

    public SegurancaWebFilter(@Value("${studyai.hosts-permitidos}") List<String> hostsPermitidos) {
        this.hostsPermitidos = hostsPermitidos.stream()
                .map(host -> host.strip().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Content-Security-Policy", POLITICA_DE_CONTEUDO);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        // "same-origin" mantém o cabeçalho Origin nos formulários da própria aplicação; "no-referrer" o trocaria por null.
        response.setHeader("Referrer-Policy", "same-origin");

        String host = host(request);
        if (!hostsPermitidos.contains(nomeDoHost(host))) {
            log.warn("Requisição recusada: o endereço {} não está em studyai.hosts-permitidos.", saneado(host));
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!metodoSeguro(request) && vemDeOutraOrigem(request, host)) {
            log.warn("Envio recusado por vir de outra origem: {} {} (Sec-Fetch-Site={}, Origin={}).",
                    request.getMethod(), saneado(request.getRequestURI()), saneado(request.getHeader("Sec-Fetch-Site")),
                    saneado(request.getHeader("Origin")));
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }

    // Os valores vêm de quem fez a requisição: mantém só caracteres comuns em endereços e limita o tamanho no log.
    private static String saneado(String valor) {
        if (valor == null) {
            return "-";
        }
        String limpo = valor.replaceAll("[^A-Za-z0-9.:/\\[\\]_-]", "?");
        return limpo.length() > 100 ? limpo.substring(0, 100) + "…" : limpo;
    }

    private static boolean metodoSeguro(HttpServletRequest request) {
        return switch (request.getMethod()) {
            case "GET", "HEAD", "OPTIONS", "TRACE" -> true;
            default -> false;
        };
    }

    // Navegadores atuais enviam Sec-Fetch-Site; Origin cobre os mais antigos. Sem nenhum dos dois, o pedido não veio
    // de uma página aberta no navegador (ex.: curl) e não há risco de CSRF.
    private static boolean vemDeOutraOrigem(HttpServletRequest request, String host) {
        String site = request.getHeader("Sec-Fetch-Site");
        if (site != null) {
            return !"same-origin".equals(site);
        }
        String origem = request.getHeader("Origin");
        return origem != null && !origem.equalsIgnoreCase(request.getScheme() + "://" + host);
    }

    // Usa o cabeçalho Host recebido, e não getServerName(), que pode vir de X-Forwarded-Host se a aplicação
    // passar a aceitar cabeçalhos de proxy.
    private static String host(HttpServletRequest request) {
        String host = request.getHeader("Host");
        if (host != null && !host.isBlank()) {
            return host.strip().toLowerCase(Locale.ROOT);
        }
        String esquema = request.getScheme();
        int porta = request.getServerPort();
        boolean portaPadrao = ("http".equals(esquema) && porta == 80) || ("https".equals(esquema) && porta == 443);
        return (request.getServerName() + (portaPadrao ? "" : ":" + porta)).toLowerCase(Locale.ROOT);
    }

    private static String nomeDoHost(String host) {
        if (host.startsWith("[")) {
            int fim = host.indexOf(']');
            return fim > 0 ? host.substring(0, fim + 1) : host;
        }
        int separador = host.indexOf(':');
        return separador >= 0 ? host.substring(0, separador) : host;
    }
}
