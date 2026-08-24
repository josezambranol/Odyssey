package com.odyxs.vg.service;

import com.odyxs.vg.entity.Lugar;
import com.odyxs.vg.entity.Resenas;
import com.odyxs.vg.entity.Usuario;
import com.odyxs.vg.repository.LugarRepository;
import com.odyxs.vg.repository.ResenaRepository;
import com.odyxs.vg.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResenaServiceTest {

    @Mock
    private ResenaRepository resenaRepository;

    @Mock
    private LugarRepository lugarRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ResenaService resenaService;

    @Test
    void guardar_Exitoso() {
        Lugar lugar = new Lugar();
        lugar.setId(1L);
        Usuario usuario = new Usuario();
        usuario.setId(2L);

        when(lugarRepository.findById(1L)).thenReturn(Optional.of(lugar));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));

        String res = resenaService.guardar(1L, 2L, "Excelente lugar!", 5);

        assertEquals("Reseña guardada.", res);
        verify(resenaRepository, times(1)).save(any(Resenas.class));
    }

    @Test
    void guardar_CalificacionInvalida_Falla() {
        Lugar lugar = new Lugar();
        lugar.setId(1L);
        Usuario usuario = new Usuario();
        usuario.setId(2L);

        when(lugarRepository.findById(1L)).thenReturn(Optional.of(lugar));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));

        String res = resenaService.guardar(1L, 2L, "Malo", 0);
        assertEquals("La calificación debe estar entre 1 y 5.", res);

        String res2 = resenaService.guardar(1L, 2L, "Exagerado", 6);
        assertEquals("La calificación debe estar entre 1 y 5.", res2);
    }
}