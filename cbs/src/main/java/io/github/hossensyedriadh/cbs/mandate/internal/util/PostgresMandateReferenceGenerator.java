package io.github.hossensyedriadh.cbs.mandate.internal.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class PostgresMandateReferenceGenerator implements MandateReferenceGenerator {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PostgresMandateReferenceGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String generate() {
        Long sequence = this.jdbcTemplate.queryForObject("SELECT nextval('mandate.mandate_reference_sequence')", Long.class);
        if (sequence == null) {
            throw new IllegalStateException("Failed to generate mandate reference");
        }
        return String.valueOf(sequence);
    }
}
