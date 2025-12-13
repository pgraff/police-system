package com.knowit.policesystem.projection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Assignment projection application.
 * 
 * @deprecated This module is deprecated. Use {@link com.knowit.policesystem.projection.OperationalProjectionApplication}
 *             instead, which handles assignments along with incidents, calls, dispatches, and activities.
 *             See migration guide: doc/migration/client-migration-guide.md
 */
@Deprecated
@SpringBootApplication
public class AssignmentProjectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssignmentProjectionApplication.class, args);
    }
}

