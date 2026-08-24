package com.odyxs.vg.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final String UPLOAD_BASE_DIR = System.getProperty("user.home") + "/odyssey-uploads/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> EXTENSIONES_PERMITIDAS = Arrays.asList(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final List<String> TIPOS_MIME_PERMITIDOS = Arrays.asList(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    /**
     * Guarda un archivo de imagen en el subdirectorio especificado (ej. "lugares", "eventos", "actividades").
     * Retorna la ruta web relativa (ej: "/uploads/lugares/uuid_nombre.jpg") o null si no es válida.
     */
    public String guardarImagen(MultipartFile archivo, String subdirectorio) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }

        if (archivo.getSize() > MAX_FILE_SIZE) {
            return null;
        }

        String tipoMime = archivo.getContentType();
        if (tipoMime == null || !TIPOS_MIME_PERMITIDOS.contains(tipoMime.toLowerCase())) {
            return null;
        }

        String extension = extraerExtension(archivo.getOriginalFilename(), tipoMime);
        if (!EXTENSIONES_PERMITIDAS.contains(extension.toLowerCase())) {
            return null;
        }

        try {
            // Asegurar que el subdirectorio sea seguro (sin .. ni caracteres extraños)
            String subDirSeguro = subdirectorio.replaceAll("[^a-zA-Z0-9_-]", "");
            Path rutaDirectorio = Paths.get(UPLOAD_BASE_DIR, subDirSeguro).normalize().toAbsolutePath();
            
            // Verificación contra Path Traversal
            Path rutaBase = Paths.get(UPLOAD_BASE_DIR).normalize().toAbsolutePath();
            if (!rutaDirectorio.startsWith(rutaBase)) {
                return null;
            }

            Files.createDirectories(rutaDirectorio);

            String nombreUnico = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path rutaDestino = rutaDirectorio.resolve(nombreUnico).normalize().toAbsolutePath();

            if (!rutaDestino.startsWith(rutaDirectorio)) {
                return null;
            }

            try (InputStream is = archivo.getInputStream()) {
                Files.copy(is, rutaDestino, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/" + subDirSeguro + "/" + nombreUnico;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Borra un archivo físico del almacenamiento local si corresponde a una ruta "/uploads/...".
     * Ignora URLs externas (http/https) y previene Path Traversal.
     */
    public boolean borrarArchivo(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            return false;
        }

        // Si es una URL externa, no intentar borrar localmente
        if (!rutaRelativa.startsWith("/uploads/")) {
            return false;
        }

        try {
            String parteLimpia = rutaRelativa.replaceFirst("^/uploads/", "");
            Path rutaBase = Paths.get(UPLOAD_BASE_DIR).normalize().toAbsolutePath();
            Path rutaArchivo = rutaBase.resolve(parteLimpia).normalize().toAbsolutePath();

            // Verificar que no intente escapar del directorio base
            if (!rutaArchivo.startsWith(rutaBase)) {
                return false;
            }

            return Files.deleteIfExists(rutaArchivo);
        } catch (IOException e) {
            return false;
        }
    }

    private String extraerExtension(String nombreOriginal, String tipoMime) {
        if (nombreOriginal != null && nombreOriginal.contains(".")) {
            String ext = nombreOriginal.substring(nombreOriginal.lastIndexOf(".")).toLowerCase();
            if (EXTENSIONES_PERMITIDAS.contains(ext)) {
                return ext;
            }
        }
        return switch (tipoMime.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}