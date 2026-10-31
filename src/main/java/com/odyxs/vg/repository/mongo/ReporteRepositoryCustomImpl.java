package com.odyxs.vg.repository.mongo;

import com.odyxs.vg.domain.mongo.Reporte;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Repository;

import java.util.*;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
public class ReporteRepositoryCustomImpl implements ReporteRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Map<String, Long> contarAgrupadoPorTipo() {
        Aggregation agg = newAggregation(
                group("tipo").count().as("total"),
                sort(Sort.Direction.DESC, "total")
        );
        AggregationResults<Document> results = mongoTemplate.aggregate(agg, "reportes", Document.class);
        Map<String, Long> map = new LinkedHashMap<>();
        for (Document doc : results.getMappedResults()) {
            Object id = doc.get("_id");
            if (id != null) {
                Number total = (Number) doc.get("total");
                map.put(id.toString(), total != null ? total.longValue() : 0L);
            }
        }
        return map;
    }

    @Override
    public Map<String, Long> contarAgrupadoPorEstado() {
        Aggregation agg = newAggregation(
                group("estado").count().as("total"),
                sort(Sort.Direction.DESC, "total")
        );
        AggregationResults<Document> results = mongoTemplate.aggregate(agg, "reportes", Document.class);
        Map<String, Long> map = new LinkedHashMap<>();
        for (Document doc : results.getMappedResults()) {
            Object id = doc.get("_id");
            if (id != null) {
                Number total = (Number) doc.get("total");
                map.put(id.toString(), total != null ? total.longValue() : 0L);
            }
        }
        return map;
    }

    @Override
    public List<Map<String, Object>> obtenerTopEntidadesConMasReportes(int limit) {
        Aggregation agg = newAggregation(
                match(org.springframework.data.mongodb.core.query.Criteria.where("entidadId").ne(null)),
                group("entidadId", "nombreEntidad").count().as("totalReportes"),
                sort(Sort.Direction.DESC, "totalReportes"),
                limit(limit)
        );
        AggregationResults<Document> results = mongoTemplate.aggregate(agg, "reportes", Document.class);
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Document doc : results.getMappedResults()) {
            Map<String, Object> item = new HashMap<>();
            Document idDoc = (Document) doc.get("_id");
            if (idDoc != null) {
                item.put("entidadId", idDoc.get("entidadId"));
                item.put("nombreEntidad", idDoc.get("nombreEntidad"));
            }
            item.put("totalReportes", doc.get("totalReportes"));
            lista.add(item);
        }
        return lista;
    }

    @Override
    public void insertarLote(List<Reporte> reportes) {
        if (reportes != null && !reportes.isEmpty()) {
            mongoTemplate.insertAll(reportes);
        }
    }
}
