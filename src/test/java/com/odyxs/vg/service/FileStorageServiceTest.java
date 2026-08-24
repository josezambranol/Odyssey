package com.odyxs.vg.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private final FileStorageService storageService = new FileStorageService();

    @Test
    void guardarImagen_ArchivoNulo_RetornaNull() {
        assertNull(storageService.guardarImagen(null, "lugares"));
    }

    @Test
    void guardarImagen_ArchivoVacio_RetornaNull() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);
        assertNull(storageService.guardarImagen(emptyFile, "lugares"));
    }

    @Test
    void guardarImagen_TipoMimeInvalido_RetornaNull() {
        MockMultipartFile txtFile = new MockMultipartFile("file", "test.txt", "text/plain", "contenido".getBytes());
        assertNull(storageService.guardarImagen(txtFile, "lugares"));
    }

    @Test
    void borrarArchivo_UrlExterna_NoBorra() {
        assertFalse(storageService.borrarArchivo("https://images.unsplash.com/photo-123"));
    }

    @Test
    void borrarArchivo_RutaNula_RetornaFalse() {
        assertFalse(storageService.borrarArchivo(null));
        assertFalse(storageService.borrarArchivo(""));
    }
}