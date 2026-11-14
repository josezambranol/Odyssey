package com.odyxs.vg.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@Profile("!test")
@EnableMongoRepositories(basePackages = "com.odyxs.vg.repository.mongo")
@EnableMongoAuditing
public class MongoConfig {
}
