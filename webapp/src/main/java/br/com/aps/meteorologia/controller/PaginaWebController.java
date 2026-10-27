package br.com.aps.meteorologia.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Rotas da interface de página única.
 *
 * <p>Também mantém o endereço {@code /webapp}, que algumas versões do NetBeans
 * tentam abrir automaticamente para projetos Maven web, redirecionando-o à raiz.
 */
@Controller
public class PaginaWebController {

    @GetMapping("/webapp")
    public String abrirRaiz() {
        return "redirect:/";
    }

    @GetMapping({"/historico", "/simulacoes", "/configuracoes"})
    public String abrirAplicacao() {
        return "forward:/index.html";
    }
}
