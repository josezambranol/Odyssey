package com.odyxs.vg.controller;

import com.odyxs.vg.entity.Categoria;
import com.odyxs.vg.entity.Lugar;
import com.odyxs.vg.repository.EventoRepository;
import com.odyxs.vg.service.CategoriaService;
import com.odyxs.vg.service.LugarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class IndexController {

    @Autowired private LugarService     lugarService;
    @Autowired private CategoriaService categoriaService;
    @Autowired private EventoRepository eventoRepo;

    @GetMapping("/")
    public String index(Model model) {
        List<Lugar> lugares = lugarService.obtenerAprobados();
        List<Categoria> categorias = categoriaService.obtenerTodas();
        Set<Long> categoriaIdsActivas = lugares.stream()
                .filter(l -> l.getCategoria() != null && l.getCategoria().getId() != null)
                .map(l -> l.getCategoria().getId())
                .collect(Collectors.toSet());
        List<Categoria> categoriasConLugares = categorias.stream()
                .filter(c -> categoriaIdsActivas.contains(c.getId()))
                .toList();

        model.addAttribute("lugares",              lugares);
        model.addAttribute("categorias",           categorias);
        model.addAttribute("categoriasConLugares", categoriasConLugares);
        model.addAttribute("eventos",
            eventoRepo.findByActivoTrueAndFechaGreaterThanEqualOrderByFechaAsc(LocalDate.now()));
        return "index";
    }

    @GetMapping("/terminos")
    public String terminos() {
        return "terminos";
    }

    @GetMapping("/privacidad")
    public String privacidad() {
        return "privacidad";
    }
}
