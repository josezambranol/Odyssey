package com.odyxs.vg.service;

import org.springframework.beans.factory.annotation.Value;
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

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> EXTENSIONES_PERMITIDAS = Arrays.asList(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final List<String> TIPOS_MIME_PERMITIDOS = Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final String uploadBaseDir;

    public FileStorageService() {
        this.uploadBaseDir = System.getProperty("user.home") + "/odyssey-uploads/";
    }

    @org.springframework.beans.factory.annotation.Autowired
    public FileStorageService(@Value("${odyssey.upload.dir:${user.home}/odyssey-uploads}") String uploadDir) {
        String clean = (uploadDir != null && !uploadDir.isBlank())
                ? uploadDir.trim()
                : System.getProperty("user.home") + "/odyssey-uploads";
        this.uploadBaseDir = clean.endsWith("/") || clean.endsWith("\\") ? clean : clean + "/";
    }

    /**
     * Guarda una imagen validando tamaño, Content-Type, extensión y firma binaria real (magic bytes).
     */
    public String guardarImagen(MultipartFile archivo, String subdirectorio) {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > MAX_FILE_SIZE) {
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
            byte[] header = new byte[12];
            int leidos;
            try (InputStream probe = archivo.getInputStream()) {
                leidos = probe.read(header);
            }
            if (!esFirmaImagenValida(header, leidos)) {
                return null;
            }

            String subDirSeguro = subdirectorio.replaceAll("[^a-zA-Z0-9_-]", "");
            Path rutaBase = Paths.get(uploadBaseDir).normalize().toAbsolutePath();
            Path rutaDirectorio = rutaBase.resolve(subDirSeguro).normalize().toAbsolutePath();
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

    public boolean borrarArchivo(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank() || !rutaRelativa.startsWith("/uploads/")) {
            return false;
        }

        try {
            String parteLimpia = rutaRelativa.replaceFirst("^/uploads/", "");
            Path rutaBase = Paths.get(uploadBaseDir).normalize().toAbsolutePath();
            Path rutaArchivo = rutaBase.resolve(parteLimpia).normalize().toAbsolutePath();
            if (!rutaArchivo.startsWith(rutaBase)) {
                return false;
            }
            return Files.deleteIfExists(rutaArchivo);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean esFirmaImagenValida(byte[] h, int len) {
        if (len < 4) return false;
        // JPEG: FF D8 FF
        if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (len >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
            return true;
        }
        // GIF: GIF8
        if (h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') {
            return true;
        }
        // WEBP: RIFF....WEBP
        return len >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
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
