package com.odyxs.vg.repository;
import com.odyxs.vg.entity.ChatbotRespuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatbotRespuestaRepository extends JpaRepository<ChatbotRespuesta, Long> {
    Optional<ChatbotRespuesta> findByClave(String clave);
}
