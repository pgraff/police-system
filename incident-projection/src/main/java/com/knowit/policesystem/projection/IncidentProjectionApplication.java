package com.knowit.policesystem.projection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Incident projection application.
 * 
 * @deprecated This module is deprecated. Use {@link com.knowit.policesystem.projection.OperationalProjectionApplication}
 *             instead, which handles incidents along with calls, dispatches, activities, and assignments.
 *             See migration guide: doc/migration/client-migration-guide.md
 */
@Deprecated
@SpringBootApplication
public class IncidentProjectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidentProjectionApplication.class, args);
    }
}
