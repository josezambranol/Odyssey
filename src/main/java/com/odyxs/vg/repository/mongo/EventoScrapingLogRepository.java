package com.odyxs.vg.repository.mongo;

import com.odyxs.vg.domain.mongo.EventoScrapingLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoScrapingLogRepository extends MongoRepository<EventoScrapingLog, String> {

    Page<EventoScrapingLog> findAllByOrderByFechaScrapingDesc(Pageable pageable);

    long countByExitoTrue();
}
