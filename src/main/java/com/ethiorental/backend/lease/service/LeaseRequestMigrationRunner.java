package com.ethiorental.backend.lease.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaseRequestMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        // 1. Drop any legacy check constraints on lease_requests.status
        try {
            jdbcTemplate.execute(
                "DO $$ \n" +
                "DECLARE \n" +
                "    r RECORD; \n" +
                "BEGIN \n" +
                "    FOR r IN ( \n" +
                "        SELECT conname \n" +
                "        FROM pg_constraint \n" +
                "        WHERE conrelid = 'lease_requests'::regclass \n" +
                "          AND contype = 'c' \n" +
                "          AND pg_get_constraintdef(oid) LIKE '%status%' \n" +
                "    ) LOOP \n" +
                "        EXECUTE 'ALTER TABLE lease_requests DROP CONSTRAINT ' || quote_ident(r.conname); \n" +
                "    END LOOP; \n" +
                "END $$;"
            );
            log.info("Successfully dropped legacy status check constraints on lease_requests");
        } catch (Exception e) {
            log.warn("Could not drop status check constraint via dynamic query: {}", e.getMessage());
            try {
                jdbcTemplate.execute("ALTER TABLE lease_requests DROP CONSTRAINT IF EXISTS lease_requests_status_check");
            } catch (Exception ex) {
                log.warn("Direct drop constraint also failed: {}", ex.getMessage());
            }
        }

        // 2. Add updated check constraint allowing all valid LeaseRequestStatus values
        try {
            jdbcTemplate.execute(
                "ALTER TABLE lease_requests ADD CONSTRAINT lease_requests_status_check " +
                "CHECK (status IN (" +
                "'PENDING', 'LANDLORD_APPROVED', 'UNDER_VERIFICATION', " +
                "'PENDING_SUPERVISOR_APPROVAL', 'SUPERVISOR_APPROVED', " +
                "'REJECTED', 'CANCELLED', 'EXPIRED', 'APPROVED'" +
                "))"
            );
            log.info("Successfully added updated lease_requests_status_check constraint");
        } catch (Exception e) {
            log.warn("Could not add updated lease_requests_status_check constraint: {}", e.getMessage());
        }

        // 3. Migrate legacy APPROVED status to LANDLORD_APPROVED
        try {
            int updatedRows = jdbcTemplate.update(
                "UPDATE lease_requests SET status = 'LANDLORD_APPROVED' WHERE status = 'APPROVED'"
            );
            if (updatedRows > 0) {
                log.info("Successfully migrated {} lease request(s) from 'APPROVED' to 'LANDLORD_APPROVED'", updatedRows);
            }
        } catch (Exception e) {
            log.warn("Could not update legacy APPROVED status: {}", e.getMessage());
        }

        // 4. Ensure null boolean flags are initialized to false
        try {
            jdbcTemplate.update("UPDATE lease_requests SET landlord_signed = false WHERE landlord_signed IS NULL");
            jdbcTemplate.update("UPDATE lease_requests SET tenant_signed = false WHERE tenant_signed IS NULL");
            jdbcTemplate.update("UPDATE lease_requests SET supervisor_signed = false WHERE supervisor_signed IS NULL");
            log.info("Successfully initialized null signed flags to false");
        } catch (Exception e) {
            log.warn("Could not normalize null signed flags: {}", e.getMessage());
        }
    }
}
