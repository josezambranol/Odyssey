package com.odyxs.vg.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.odyxs.vg.repository",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.odyxs\\.vg\\.repository\\.mongo\\..*"
        )
)
@EntityScan(basePackages = "com.odyxs.vg.entity")
public class JpaConfig {
}
