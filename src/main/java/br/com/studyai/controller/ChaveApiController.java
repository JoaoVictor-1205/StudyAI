package br.com.studyai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// A tela só é renderizada aqui: a chave é salva e lida pelo JavaScript, no armazenamento do navegador.
@Controller
public class ChaveApiController {

    @GetMapping("/chave-api")
    public String chaveApi() {
        return "chave-api";
    }
}
