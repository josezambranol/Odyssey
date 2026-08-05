package com.odyxs.vg.controller;

import com.odyxs.vg.repository.EventoRepository;
import com.odyxs.vg.service.CategoriaService;
import com.odyxs.vg.service.LugarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.LocalDate;

@Controller
public class IndexController {

    @Autowired private LugarService     lugarService;
    @Autowired private CategoriaService categoriaService;
    @Autowired private EventoRepository eventoRepo;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("lugares",    lugarService.obtenerAprobados());
        model.addAttribute("categorias", categoriaService.obtenerTodas());
        model.addAttribute("eventos",
            eventoRepo.findByActivoTrueAndFechaGreaterThanEqualOrderByFechaAsc(LocalDate.now()));
        return "index";
    }
}
