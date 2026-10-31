package com.odyxs.vg.repository.mongo;

import com.odyxs.vg.domain.mongo.LugarGeoDocument;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LugarGeoRepository extends MongoRepository<LugarGeoDocument, String> {

    GeoResults<LugarGeoDocument> findByUbicacionNear(Point point, Distance distance);

    Optional<LugarGeoDocument> findByLugarId(Long lugarId);
}
