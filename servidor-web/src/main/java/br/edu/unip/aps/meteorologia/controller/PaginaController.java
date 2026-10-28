package br.edu.unip.aps.meteorologia.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Entrega a mesma aplicação web nas rotas desenhadas nos wireframes. */
@Controller
public class PaginaController {

    @GetMapping({"/historico", "/simulacoes", "/configuracoes"})
    public String pagina() {
        return "forward:/index.html";
    }
}
