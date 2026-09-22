package com.ethiorental.backend.agreement.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

/**
 * Ensures the 'agreements' table schema matches the JPA entity.
 * If a legacy 'agreements' table exists with an 'id' column of type 'bigint',
 * it drops the table before EntityManagerFactory initializes so Hibernate can recreate it with UUID.
 */
@Slf4j
@Component("agreementSchemaFixer")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AgreementSchemaFixer {

    private final DataSource dataSource;

    @PostConstruct
    public void fixAgreementTableSchema() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            boolean isLegacyBigint = false;
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT data_type FROM information_schema.columns " +
                    "WHERE table_name = 'agreements' AND column_name = 'id'")) {
                if (rs.next()) {
                    String dataType = rs.getString("data_type");
                    if (dataType != null && (dataType.equalsIgnoreCase("bigint")
                            || dataType.equalsIgnoreCase("integer")
                            || dataType.equalsIgnoreCase("numeric"))) {
                        isLegacyBigint = true;
                    }
                }
            }

            if (isLegacyBigint) {
                log.warn("Detected legacy agreements table with bigint id column. Dropping legacy table to allow Hibernate to recreate it with UUID id...");
                stmt.execute("DROP TABLE IF EXISTS agreements CASCADE");
                log.info("Successfully dropped legacy agreements table. Hibernate will recreate it cleanly on startup.");
            }

            // Ensure all columns in 'citizens' table exist (e.g. tin_number, payout bank accounts, Kebele details)
            String[] citizenColumns = {
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS national_id VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS works_on VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS city VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS sub_city VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS woreda VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS house_number VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS tin_number VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS preferred_payment_method VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS bank_name VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS account_number VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS account_holder_name VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS bank_name2 VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS account_number2 VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS account_holder_name2 VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS bank_name3 VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS account_number3 VARCHAR(255)",
                "ALTER TABLE citizens ADD COLUMN IF NOT EXISTS account_holder_name3 VARCHAR(255)"
            };
            for (String sql : citizenColumns) {
                try {
                    stmt.execute(sql);
                } catch (Exception e) {
                    log.warn("Schema patch on citizens table skipped/failed: {} - {}", sql, e.getMessage());
                }
            }

            // Ensure all columns in 'property_units' and 'agreements' tables exist
            String[] otherColumns = {
                "ALTER TABLE property_units ADD COLUMN IF NOT EXISTS tenant_name VARCHAR(255)",
                "ALTER TABLE property_units ADD COLUMN IF NOT EXISTS floor_level VARCHAR(255)",
                "ALTER TABLE property_units ADD COLUMN IF NOT EXISTS category VARCHAR(255)",
                "ALTER TABLE property_units ADD COLUMN IF NOT EXISTS rent_amount NUMERIC(19, 2)",
                "ALTER TABLE property_units ADD COLUMN IF NOT EXISTS submeter BOOLEAN",
                "ALTER TABLE property_units ADD COLUMN IF NOT EXISTS water_supply BOOLEAN",
                "ALTER TABLE property_units ADD COLUMN IF NOT EXISTS frontage VARCHAR(255)",
                "ALTER TABLE property_units ADD COLUMN IF NOT EXISTS description TEXT",
                "ALTER TABLE agreements ADD COLUMN IF NOT EXISTS unit_id UUID",
                "ALTER TABLE agreements ADD COLUMN IF NOT EXISTS monthly_payment_due_day INT",
                "ALTER TABLE agreements ADD COLUMN IF NOT EXISTS utilities_paid_by VARCHAR(255)",
                "ALTER TABLE agreements ADD COLUMN IF NOT EXISTS property_condition VARCHAR(255)",
                "ALTER TABLE agreements ADD COLUMN IF NOT EXISTS property_ownership_type VARCHAR(255)"
            };
            for (String sql : otherColumns) {
                try {
                    stmt.execute(sql);
                } catch (Exception e) {
                    log.warn("Schema patch skipped/failed: {} - {}", sql, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to check/fix database table schemas: {}", e.getMessage(), e);
        }
    }

    @Configuration
    public static class AgreementSchemaConfig {
        @Bean
        public static BeanFactoryPostProcessor entityManagerFactoryDependsOnSchemaFixer() {
            return beanFactory -> {
                String[] names = beanFactory.getBeanNamesForType(EntityManagerFactory.class);
                for (String name : names) {
                    BeanDefinition bd = beanFactory.getBeanDefinition(name);
                    String[] dependsOn = bd.getDependsOn();
                    if (dependsOn == null || dependsOn.length == 0) {
                        bd.setDependsOn("agreementSchemaFixer");
                    } else {
                        String[] newDependsOn = Arrays.copyOf(dependsOn, dependsOn.length + 1);
                        newDependsOn[dependsOn.length] = "agreementSchemaFixer";
                        bd.setDependsOn(newDependsOn);
                    }
                }
            };
        }
    }
}
