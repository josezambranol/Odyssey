package com.odyxs.vg.repository.mongo;

import com.odyxs.vg.domain.mongo.ChatbotLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface ChatbotLogRepository extends MongoRepository<ChatbotLog, String> {

    Page<ChatbotLog> findByCategoria(String categoria, Pageable pageable);

    Page<ChatbotLog> findByFechaCreacionBetween(Instant desde, Instant hasta, Pageable pageable);

    long countByExitoTrue();

    long countByExitoFalse();
}
