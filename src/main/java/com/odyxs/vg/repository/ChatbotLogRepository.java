package com.odyxs.vg.repository;

import com.odyxs.vg.entity.ChatbotLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface ChatbotLogRepository extends JpaRepository<ChatbotLog, String> {

    Page<ChatbotLog> findByCategoria(String categoria, Pageable pageable);

    Page<ChatbotLog> findByFechaCreacionBetween(Instant desde, Instant hasta, Pageable pageable);

    long countByExitoTrue();

    long countByExitoFalse();
}
