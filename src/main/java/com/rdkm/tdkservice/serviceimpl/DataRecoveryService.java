package com.rdkm.tdkservice.serviceimpl;

import com.rdkm.tdkservice.exception.TDKServiceException;
import com.rdkm.tdkservice.service.IDataRecoveryService;

import java.sql.Connection;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

@Service
public class DataRecoveryService implements IDataRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataRecoveryService.class);

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private DataSource dataSource;

    @Override
    public void executeDataRecovery() {
        LOGGER.info("Starting data recovery process for script and related tables...");

        Connection connection = null;
        try {
            Resource recoveryResource = appContext.getResource("classpath:db/tdk-data-recovery-dump.sql");

            // Check if the recovery file exists
            if (!recoveryResource.exists()) {
                String errorMessage = "Data recovery file is not available. Please ensure the file exists at: src/main/resources/db/tdk-data-recovery-dump.sql";
                LOGGER.error(errorMessage);
                throw new TDKServiceException(errorMessage);
            }

            // Check if the recovery file is readable
            if (!recoveryResource.isReadable()) {
                String errorMessage = "Data recovery file exists but is not readable. Please check permissions for: src/main/resources/db/tdk-data-recovery-dump.sql";
                LOGGER.error(errorMessage);
                throw new TDKServiceException(errorMessage);
            }

            // Get connection and start transaction
            connection = dataSource.getConnection();
            connection.setAutoCommit(false); // Start transaction

            LOGGER.info("Starting database transaction for data recovery...");

            // Explicitly disable foreign key checks before executing the script
            LOGGER.info("Disabling foreign key checks for data recovery");
            connection.createStatement().execute("SET FOREIGN_KEY_CHECKS = 0");

            // Execute the recovery script within transaction
            ScriptUtils.executeSqlScript(connection, recoveryResource);

            // Re-enable foreign key checks after successful execution
            LOGGER.info("Re-enabling foreign key checks after data recovery");
            connection.createStatement().execute("SET FOREIGN_KEY_CHECKS = 1");

            // Commit transaction if all successful
            connection.commit();
            LOGGER.info("Data recovery completed successfully - script tables reloaded");

        } catch (Exception e) {
            LOGGER.error("Data recovery failed, rolling back transaction", e);

            // Rollback transaction on any failure
            if (connection != null) {
                try {
                    // Re-enable foreign key checks even on failure to maintain database integrity
                    connection.createStatement().execute("SET FOREIGN_KEY_CHECKS = 1");
                    connection.rollback();
                    LOGGER.info("Transaction rolled back successfully");
                } catch (Exception rollbackException) {
                    LOGGER.error("Failed to rollback transaction", rollbackException);
                }
            }

            throw new TDKServiceException("Data recovery failed: " + e.getMessage());
        } finally {
            // Restore auto-commit and close connection
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (Exception closeException) {
                    LOGGER.error("Failed to close database connection", closeException);
                }
            }
        }
    }
}